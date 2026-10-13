package threeadd.glimpsy.feature.simple;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import threeadd.glimpsy.util.ScheduleUtil;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.manager.ListeningManager;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Grows bamboo faster than vanilla's random ticks, without touching anything else.
 * <p>
 * Instead of blindly sampling random positions (which mostly miss and cost the server), bamboo
 * block positions are tracked via place/grow/break events and a one-time scan of each chunk as it
 * loads, then a bounded number of tracked blocks are force-grown every tick. Growth is therefore
 * proportional to the bamboo you actually have, not to world size.
 */
public class BambooGrowthManager extends ListeningManager {

    private static final int MAX_BAMBOO_HEIGHT = 16;

    private final Map<World, Set<Long>> bamboo = new ConcurrentHashMap<>();
    private final Map<World, Iterator<Long>> cursors = new ConcurrentHashMap<>();
    private final Deque<Chunk> pendingScan = new ArrayDeque<>();

    @Override
    public void onEnable() {
        for (World world : Bukkit.getWorlds())
            for (Chunk chunk : world.getLoadedChunks())
                pendingScan.add(chunk);

        ScheduleUtil.scheduleSync(this::tick, 1, 1, ScheduleUtil.Unit.TICK);
    }

    // --- Tracking ----------------------------------------------------------------------------

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        pendingScan.add(event.getChunk()); // scanned a chunk or two per tick, see tick()
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        track(event.getBlockPlaced());
    }

    @EventHandler
    public void onGrow(BlockGrowEvent event) {
        track(event.getBlock());
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        untrack(event.getBlock());
    }

    @EventHandler
    public void onBurn(BlockBurnEvent event) {
        untrack(event.getBlock());
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) untrack(block);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : event.blockList()) untrack(block);
    }

    // --- Tick --------------------------------------------------------------------------------

    private void tick() {
        int scanChunks = Math.max(1, ConfigManager.getInt("bamboo.scan-chunks-per-tick", 2));
        for (int i = 0; i < scanChunks && !pendingScan.isEmpty(); i++)
            scanChunk(pendingScan.poll());

        int growths = ConfigManager.getInt("bamboo.growths-per-tick", 50);
        if (growths <= 0) return;

        int budget = growths;
        for (Map.Entry<World, Set<Long>> entry : bamboo.entrySet()) {
            if (budget <= 0) break;
            budget -= growFrom(entry.getKey(), entry.getValue(), budget);
        }
    }

    private int growFrom(World world, Set<Long> positions, int budget) {
        if (positions.isEmpty()) return 0;

        Iterator<Long> cursor = cursors.computeIfAbsent(world, w -> positions.iterator());
        int grown = 0;

        while (grown < budget) {
            if (!cursor.hasNext()) {
                cursor = positions.iterator();
                cursors.put(world, cursor);
                if (!cursor.hasNext()) break;
            }

            long key = cursor.next();
            int x = unpackX(key), y = unpackY(key), z = unpackZ(key);

            if (!world.isChunkLoaded(x >> 4, z >> 4)) continue; // keep it for when the chunk loads

            Block block = world.getBlockAt(x, y, z);
            if (!isBamboo(block)) {
                cursor.remove(); // stale (broken/pushed) - self-heals
                continue;
            }

            Block above = block.getRelative(BlockFace.UP);
            if (isBamboo(above)) {
                // Middle of a column, not a growable tip - drop it so every visit counts.
                cursor.remove();
                continue;
            }

            // Grow the block up ourselves instead of applyBoneMeal - that spawns bone-meal
            // particles and a sound every single time, which we don't want at this rate.
            if (!above.getType().isAir() || columnHeight(block) >= MAX_BAMBOO_HEIGHT) continue;

            above.setType(Material.BAMBOO, false);
            grown++;

            cursor.remove(); // this block is now a middle block
            positions.add(pack(above.getX(), above.getY(), above.getZ()));
        }

        return grown;
    }

    // Bamboo's natural height cap (16) - grow only up to it, like vanilla.
    private int columnHeight(Block block) {
        int height = 1;
        Block below = block.getRelative(BlockFace.DOWN);
        while (height < MAX_BAMBOO_HEIGHT && isBamboo(below)) {
            height++;
            below = below.getRelative(BlockFace.DOWN);
        }
        return height;
    }

    private void scanChunk(Chunk chunk) {
        World world = chunk.getWorld();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();
        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;

        Set<Long> positions = bamboo.computeIfAbsent(world, w -> ConcurrentHashMap.newKeySet());

        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++)
                for (int y = minY; y < maxY; y++) {
                    Material type = chunk.getBlock(x, y, z).getType();
                    if (type != Material.BAMBOO && type != Material.BAMBOO_SAPLING) continue;

                    // Only track growable tips (nothing bamboo directly above).
                    if (y + 1 < maxY) {
                        Material above = chunk.getBlock(x, y + 1, z).getType();
                        if (above == Material.BAMBOO) continue;
                    }

                    positions.add(pack(baseX + x, y, baseZ + z));
                }
    }

    // --- Helpers -----------------------------------------------------------------------------

    private static boolean isBamboo(Block block) {
        Material type = block.getType();
        return type == Material.BAMBOO || type == Material.BAMBOO_SAPLING;
    }

    private void track(Block block) {
        if (!isBamboo(block)) return;
        bamboo.computeIfAbsent(block.getWorld(), w -> ConcurrentHashMap.newKeySet())
                .add(pack(block.getX(), block.getY(), block.getZ()));
    }

    private void untrack(Block block) {
        Set<Long> positions = bamboo.get(block.getWorld());
        if (positions != null) positions.remove(pack(block.getX(), block.getY(), block.getZ()));
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (z & 0x3FFFFFF);
    }

    private static int unpackX(long key) {
        return (int) (key >> 38);
    }

    private static int unpackY(long key) {
        int y = (int) ((key >> 26) & 0xFFF);
        return y >= 2048 ? y - 4096 : y;
    }

    private static int unpackZ(long key) {
        int z = (int) (key & 0x3FFFFFF);
        return z >= 0x2000000 ? z - 0x4000000 : z;
    }
}
