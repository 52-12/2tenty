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
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.feature.spawn.WorldSpawnManager;
import threeadd.glimpsy.util.ScheduleUtil;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

// This is my baby, the map setter, even though it's only slightly faster than FAWE itself (even though it uses FAWE, it's still faster).
// Runs off the main thread. The wipe is done in bounded waves whose concurrency adapts to how much
// heap is free, so it uses as much RAM as it safely can without OOMing a low-memory host (issue #23).

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
        BlockVector3 max = BlockVector3.at(worldSize, 1000, worldSize);

        CuboidRegion region = new CuboidRegion(min, max);
        ResetJob job = new ResetJob(bukkitWorld, world, requestedThreads, start);

        Bukkit.getAsyncScheduler().runNow(Glimpsy.getInstance(), task -> {
            job.units.addAll(splitIntoUnits(region, sessionBlockLimit()));
            job.unitCount = job.units.size();
            log.info("Map reset split into {} units (max {} threads, {}MB reserve, {}MB/thread)",
                    job.unitCount, maxThreads(requestedThreads), memoryReserveMb(), memoryPerThreadMb());

            runWave(job);
        });

        return true;
    }

    // --- Adaptive wave driver -------------------------------------------------------------------

    private static void runWave(ResetJob job) {
        if (job.aborted) return;

        if (job.units.isEmpty()) {
            complete(job);
            return;
        }

        long freeMb = freeHeapMb();

        // Critically low on memory - pause and let GC / other load ease. Bail out entirely if it
        // never recovers, rather than letting the JVM die.
        if (freeMb < minFreeMb()) {
            if (job.lowMemorySince == 0) {
                job.lowMemorySince = System.currentTimeMillis();
                log.warn("Map reset: only {}MB heap free (below the {}MB floor) - throttling", freeMb, minFreeMb());
            }

            if (System.currentTimeMillis() - job.lowMemorySince > abortTimeoutSeconds() * 1000L) {
                abort(job, "the server stayed below " + minFreeMb() + "MB free heap for "
                        + abortTimeoutSeconds() + "s");
                return;
            }

            ScheduleUtil.scheduleAsync(() -> runWave(job), Math.max(1, batchDelayTicks()), ScheduleUtil.Unit.TICK);
            return;
        }

        job.lowMemorySince = 0;

        long usableMb = freeMb - memoryReserveMb();
        int concurrency = Math.max(1, (int) Math.min(maxThreads(job.requestedThreads),
                usableMb / Math.max(1, memoryPerThreadMb())));

        List<CompletableFuture<Integer>> wave = new ArrayList<>();

        for (int i = 0; i < concurrency && !job.units.isEmpty(); i++) {
            Region unit = job.units.poll();
            CompletableFuture<Integer> future = new CompletableFuture<>();
            wave.add(future);

            FaweAPI.getTaskManager().async(() -> {
                try (EditSession session = WorldEdit.getInstance().newEditSessionBuilder()
                        .world(job.world)
                        .limitUnlimited() // safe now that each unit is capped by session-block-limit
                        .fastMode(true)
                        .changeSetNull()
                        .build()) {

                    int replacedBlocks = session.replaceBlocks(unit,
                            new BlockMask().add(state -> !unbreakableBlocks.contains(state.toBaseBlock())),
                            BlockTypes.AIR);

                    session.flushQueue();
                    future.complete(replacedBlocks);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        }

        CompletableFuture.allOf(wave.toArray(new CompletableFuture[0]))
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        abort(job, throwable);
                        return;
                    }

                    for (CompletableFuture<Integer> future : wave)
                        job.totalBlocks += future.join();

                    ScheduleUtil.scheduleAsync(() -> runWave(job), Math.max(1, batchDelayTicks()), ScheduleUtil.Unit.TICK);
                });
    }

    private static void complete(ResetJob job) {
        long end = System.currentTimeMillis();
        double seconds = (double) (end - job.start) / 1000;

        log.info("Reset the map in ({} blocks affected, {} units) {}s", job.totalBlocks, job.unitCount, seconds);
        finishReset(job.bukkitWorld);
        RESETTING.set(false);
    }

    private static void abort(ResetJob job, Throwable throwable) {
        log.error("Map reset failed and was stopped", throwable);
        abort(job, throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage());
    }

    private static void abort(ResetJob job, String reason) {
        if (job.aborted) return;
        job.aborted = true;

        log.error("Map reset aborted: {}", reason);
        Component message = ComponentParser.parseRichString("<error>Map reset aborted: " + reason);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("2tenty.command.mapreset")) player.sendMessage(message);
        }

        RESETTING.set(false);
    }

    // --- Region splitting -----------------------------------------------------------------------

    // Recursively halves the region along its longest axis until every piece is at most
    // maxUnitBlocks, so each FAWE session has a hard upper bound on the blocks it can queue - the
    // real defence against the old "one giant unlimited session per thread" OOM.
    private static List<Region> splitIntoUnits(CuboidRegion parent, int maxUnitBlocks) {
        List<Region> units = new ArrayList<>();
        Deque<CuboidRegion> pending = new ArrayDeque<>();
        pending.add(parent);

        while (!pending.isEmpty()) {
            CuboidRegion current = pending.poll();

            long volume = (long) current.getWidth() * current.getHeight() * current.getLength();
            if (volume <= maxUnitBlocks) {
                units.add(current);
                continue;
            }

            int width = current.getWidth();
            int height = current.getHeight();
            int length = current.getLength();
            BlockVector3 currentMin = current.getMinimumPoint();
            BlockVector3 currentMax = current.getMaximumPoint();

            if (width >= height && width >= length) {
                int mid = currentMin.x() + width / 2;
                pending.add(new CuboidRegion(currentMin, BlockVector3.at(mid - 1, currentMax.y(), currentMax.z())));
                pending.add(new CuboidRegion(BlockVector3.at(mid, currentMin.y(), currentMin.z()), currentMax));
            } else if (height >= length) {
                int mid = currentMin.y() + height / 2;
                pending.add(new CuboidRegion(currentMin, BlockVector3.at(currentMax.x(), mid - 1, currentMax.z())));
                pending.add(new CuboidRegion(BlockVector3.at(currentMin.x(), mid, currentMin.z()), currentMax));
            } else {
                int mid = currentMin.z() + length / 2;
                pending.add(new CuboidRegion(currentMin, BlockVector3.at(currentMax.x(), currentMax.y(), mid - 1)));
                pending.add(new CuboidRegion(BlockVector3.at(currentMin.x(), currentMin.y(), mid), currentMax));
            }
        }

        return units;
    }

    // --- Memory / config ------------------------------------------------------------------------

    /** Heap currently available to allocate, in MB. */
    private static long freeHeapMb() {
        Runtime runtime = Runtime.getRuntime();
        long available = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
        return Math.max(0, available / (1024 * 1024));
    }

    // 0 (or unset) means "use the machine's CPU count".
    private static int maxThreads(int requestedThreads) {
        int configured = ConfigManager.getInt("map-reset.max-threads", 0);
        int cap = configured > 0 ? configured : Runtime.getRuntime().availableProcessors();

        return Math.max(1, Math.min(cap, Math.max(1, requestedThreads)));
    }

    private static int sessionBlockLimit() {
        return Math.max(1, ConfigManager.getInt("map-reset.session-block-limit", 2_000_000));
    }

    private static int memoryReserveMb() {
        return Math.max(0, ConfigManager.getInt("map-reset.memory-reserve-mb", 256));
    }

    private static int memoryPerThreadMb() {
        return Math.max(1, ConfigManager.getInt("map-reset.memory-per-thread-mb", 128));
    }

    private static int minFreeMb() {
        return Math.max(0, ConfigManager.getInt("map-reset.min-free-mb", 128));
    }

    private static int abortTimeoutSeconds() {
        return Math.max(1, ConfigManager.getInt("map-reset.abort-timeout-seconds", 300));
    }

    private static int batchDelayTicks() {
        return Math.max(1, ConfigManager.getInt("map-reset.batch-delay-ticks", 5));
    }

    private static void finishReset(org.bukkit.World bukkitWorld) {
        MapResetManager.recordReset(); // so offline players get redirected to spawn on rejoin, not their old (now-void) position

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

        Location spawnLocation = WorldSpawnManager.getCustomSpawn().orElse(bukkitWorld.getSpawnLocation());

        ScheduleUtil.scheduleSync(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.teleportAsync(spawnLocation).thenAccept(success -> {
                    if (!success) throw new IllegalStateException("Teleport failed");

                    player.setFoodLevel(10);

                    player.stopSound(SoundStop.all()); // using adventure instead of Bukkit

                    player.showElderGuardian(true);
                });
            }
        });
    }

    /** Mutable state for one in-flight reset, shared across the async wave callbacks. */
    private static final class ResetJob {

        final org.bukkit.World bukkitWorld;
        final World world;
        final int requestedThreads;
        final long start;

        final Deque<Region> units = new ArrayDeque<>();

        long totalBlocks = 0;
        long unitCount = 0;
        long lowMemorySince = 0;
        boolean aborted = false;

        ResetJob(org.bukkit.World bukkitWorld, World world, int requestedThreads, long start) {
            this.bukkitWorld = bukkitWorld;
            this.world = world;
            this.requestedThreads = requestedThreads;
            this.start = start;
        }
    }
}
