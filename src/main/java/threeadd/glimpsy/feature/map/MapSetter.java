
package threeadd.glimpsy.feature.map;

import com.fastasyncworldedit.core.FaweAPI;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.ScheduleUtil;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

// This is my baby, the map setter, even though it's only slightly faster than FAWE itself (even though it uses FAWE, it's still faster).
// Multi Threaded (using requested or max cpu threads), safe to call from any thread, never touching main thread.

// finish was inspired by: https://github.com/Awokens/VoidSurvival-2025-Edition/blob/master/src/main/java/com/github/voidSurvival2025/Manager/Others/WorldResetManager.java

public class MapSetter {

    private final static Set<BaseBlock> unbreakableBlocks = Set.of(
            Objects.requireNonNull(BlockTypes.BEDROCK).getDefaultState().toBaseBlock()
    );
    private static final Logger log = LoggerFactory.getLogger(MapSetter.class);
    private static final AtomicBoolean RESETTING = new AtomicBoolean(false);

    /**
     * @return {@code true} if a reset was started, {@code false} if one is already in progress
     * or no world border is set.
     */
    public static boolean reset(int requestedThreads) {

        long start = System.currentTimeMillis();

        final int maxThreads = Math.max(1, Runtime.getRuntime().availableProcessors());
        final int threadAmount = Math.max(1, Math.min(maxThreads, requestedThreads));

        // First world is the one defined under server-level in server.properties
        org.bukkit.World bukkitWorld = Bukkit.getWorlds().getFirst();
        World world = BukkitAdapter.adapt(bukkitWorld);

        // Getting the world based of off the center of the world (plus half the world border)
        double borderSize = bukkitWorld.getWorldBorder().getSize();
        if (borderSize >= 29_999_984) {
            log.error("Cannot reset map: no world border is set. Run /worldborder set <size> first.");
            return false;
        }

        if (!RESETTING.compareAndSet(false, true)) {
            log.warn("A map reset is already in progress, ignoring this request.");
            return false;
        }

        double worldSize = borderSize / 2;
        BlockVector3 min = BlockVector3.at(-worldSize, -64, -worldSize);
        BlockVector3 max = BlockVector3.at(worldSize, 319, worldSize);

        CuboidRegion region = new CuboidRegion(min, max);

        List<CompletableFuture<Integer>> futures = new CopyOnWriteArrayList<>(); // async safety
        for (int i = 0; i < threadAmount; i++) {
            futures.add(new CompletableFuture<>());
        }

        Bukkit.getAsyncScheduler().runNow(Glimpsy.getInstance(), task -> {
            List<Region> subRegions = getSubRegions(region, threadAmount);

            long end = System.currentTimeMillis();
            double seconds = (double) (end - start) / 1000;
            log.info("Calculated {} sub regions for this reset using {} threads in {}s", subRegions.size(), threadAmount, seconds);

            int index = 0;
            for (Region subRegion : getSubRegions(region, threadAmount)) {

                int finalIndex = index; // make compiler happy
                FaweAPI.getTaskManager().async(() -> {
                    try (EditSession session = WorldEdit.getInstance().newEditSessionBuilder()
                            .world(world)
                            .limitUnlimited()
                            .fastMode(true)
                            .changeSetNull()
                            .build()) {
                        int replacedBlocks = session.replaceBlocks(subRegion,
                                new BlockMask().add(state -> !unbreakableBlocks.contains(state.toBaseBlock())),
                                BlockTypes.AIR);

                        futures.get(finalIndex).complete(replacedBlocks);

                        session.flushQueue();
                    }
                });
                index++;
            }
        });

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> {
                    int totalBlocksReplaced = 0;

                    for (CompletableFuture<Integer> future : futures)
                        totalBlocksReplaced += future.join();

                    return totalBlocksReplaced;
                })
                .thenAccept(totalBlocksReplaced -> {
                    finishReset(bukkitWorld);

                    long end = System.currentTimeMillis();
                    double seconds = (double) (end - start) / 1000;

                    log.info("Reset the map in ({} blocks affected) {}s", totalBlocksReplaced, seconds);
                })
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) log.error("Map reset failed", throwable);
                    RESETTING.set(false);
                });

        return true;
    }

    private static void finishReset(org.bukkit.World bukkitWorld) {
        Component alertSymbol = Component.text('⚠')
                .color(TextColor.color(0xD93B3B));

        Bukkit.getServer().broadcast(
                Component.newline()
                        .append(alertSymbol)
                        .appendSpace()
                        .append(ComponentParser.parseRichString("The map has been reset"))
                        .appendSpace()
                        .append(alertSymbol)
                        .appendNewline()
                        .append(ComponentParser.parseRichString("All players were teleported to spawn"))
                        .appendNewline()
        );

        ScheduleUtil.scheduleSync(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.teleportAsync(bukkitWorld.getSpawnLocation()).thenAccept(success -> {
                    if (!success) throw new IllegalStateException("Teleport failed");

                    player.setFoodLevel(10);

                    player.stopSound(SoundStop.all()); // using adventure instead of Bukkit

                    player.showElderGuardian(true);
                });
            }
        });
    }

    private static List<Region> getSubRegions(CuboidRegion parent, int amount) {
        if (amount <= 1) {
            List<Region> subs = new ArrayList<>();
            if (amount == 1) {
                subs.add(parent);
            }
            return subs;
        }

        BlockVector3 min = parent.getMinimumPoint();
        BlockVector3 max = parent.getMaximumPoint();

        int sizeX = parent.getWidth();
        int sizeY = parent.getHeight();
        int sizeZ = parent.getLength();

        int divX = 1, divY = 1, divZ = 1;
        int totalDivCount = 1;

        while (totalDivCount < amount) {
            int currentMaxDimension = Math.max(sizeX / divX, Math.max(sizeY / divY, sizeZ / divZ));

            if (currentMaxDimension == sizeX / divX) {
                divX++;
            } else if (currentMaxDimension == sizeY / divY) {
                divY++;
            } else {
                divZ++;
            }
            totalDivCount = divX * divY * divZ;
        }

        int subSizeX = (int) Math.ceil((double) sizeX / divX);
        int subSizeY = (int) Math.ceil((double) sizeY / divY);
        int subSizeZ = (int) Math.ceil((double) sizeZ / divZ);

        List<CuboidRegion> allPotentialSubs = new ArrayList<>();
        for (int i = 0; i < divX; i++) {
            for (int j = 0; j < divY; j++) {
                for (int k = 0; k < divZ; k++) {
                    int subMinX = min.x() + i * subSizeX;
                    int subMinY = min.y() + j * subSizeY;
                    int subMinZ = min.z() + k * subSizeZ;

                    int subMaxX = Math.min(min.x() + (i + 1) * subSizeX - 1, max.x());
                    int subMaxY = Math.min(min.y() + (j + 1) * subSizeY - 1, max.y());
                    int subMaxZ = Math.min(min.z() + (k + 1) * subSizeZ - 1, max.z());

                    BlockVector3 subMin = BlockVector3.at(subMinX, subMinY, subMinZ);
                    BlockVector3 subMax = BlockVector3.at(subMaxX, subMaxY, subMaxZ);

                    allPotentialSubs.add(new CuboidRegion(subMin, subMax));
                }
            }
        }

        List<Region> finalSubs = new ArrayList<>();

        for (int i = 0; i < amount - 1; i++) {
            finalSubs.add(allPotentialSubs.get(i));
        }

        CuboidRegion lastRegion;
        if (allPotentialSubs.size() > amount - 1) {
            lastRegion = allPotentialSubs.get(amount - 1);
        } else {
            finalSubs.add(allPotentialSubs.get(amount - 1));
            return finalSubs;
        }

        if (allPotentialSubs.size() > amount) {

            BlockVector3 mergedMin = lastRegion.getMinimumPoint();
            BlockVector3 mergedMax = lastRegion.getMaximumPoint();

            for (int i = amount; i < allPotentialSubs.size(); i++) {
                CuboidRegion current = allPotentialSubs.get(i);

                mergedMin = min(mergedMin, current.getMinimumPoint());
                mergedMax = max(mergedMax, current.getMaximumPoint());
            }

            lastRegion = new CuboidRegion(mergedMin, mergedMax);
        }

        finalSubs.add(lastRegion);

        return finalSubs;
    }

    private static BlockVector3 min(BlockVector3 v1, BlockVector3 v2) {
        int minX = Math.min(v1.x(), v2.x());
        int minY = Math.min(v1.y(), v2.y());
        int minZ = Math.min(v1.z(), v2.z());

        return BlockVector3.at(minX, minY, minZ);
    }

    private static BlockVector3 max(BlockVector3 v1, BlockVector3 v2) {
        int maxX = Math.max(v1.x(), v2.x());
        int maxY = Math.max(v1.y(), v2.y());
        int maxZ = Math.max(v1.z(), v2.z());

        return BlockVector3.at(maxX, maxY, maxZ);
    }
}
