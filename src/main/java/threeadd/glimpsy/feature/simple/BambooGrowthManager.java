package threeadd.glimpsy.feature.simple;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import threeadd.glimpsy.util.ScheduleUtil;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.manager.ListeningManager;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates extra vanilla-style random ticks aimed only at bamboo, independent of the world's
 * randomTickSpeed gamerule (which would speed up every crop, sapling, leaf decay, etc).
 * <p>
 * Chunks are tracked via load/unload events and visited with a moving cursor, and each tick only
 * spends a bounded number of samples - instead of re-scanning every loaded chunk every tick. That
 * keeps the bamboo boost while capping the per-tick cost (it used to be
 * {@code extra-random-ticks × loaded chunks} samples every single tick).
 */
public class BambooGrowthManager extends ListeningManager {

    private final Set<Chunk> loadedChunks = new LinkedHashSet<>();
    private Iterator<Chunk> cursor;

    @Override
    public void onEnable() {
        for (World world : Bukkit.getWorlds())
            loadedChunks.addAll(java.util.List.of(world.getLoadedChunks()));

        ScheduleUtil.scheduleSync(this::tick, 1, 1, ScheduleUtil.Unit.TICK);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        loadedChunks.add(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        loadedChunks.remove(event.getChunk());
    }

    private void tick() {
        int perChunk = ConfigManager.getInt("bamboo.extra-random-ticks", 0);
        if (perChunk <= 0 || loadedChunks.isEmpty()) return;

        int budget = Math.max(1, ConfigManager.getInt("bamboo.max-samples-per-tick", 2000));
        int chunksThisTick = Math.max(1, budget / perChunk);

        for (int processed = 0; processed < chunksThisTick; processed++) {
            if (cursor == null || !cursor.hasNext())
                cursor = loadedChunks.iterator();

            if (!cursor.hasNext()) return;

            Chunk chunk = cursor.next();
            if (!chunk.isLoaded()) {
                cursor.remove();
                continue;
            }

            growChunk(chunk, perChunk);
        }
    }

    private void growChunk(Chunk chunk, int extraTicks) {
        World world = chunk.getWorld();
        int minY = world.getMinHeight();
        int height = world.getMaxHeight() - minY;
        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;

        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < extraTicks; i++) {
            int localX = random.nextInt(16);
            int localZ = random.nextInt(16);
            int y = minY + random.nextInt(height);

            Block block = chunk.getBlock(localX, y, localZ); // chunk-local x/z, absolute y
            Material type = block.getType();
            if (type == Material.BAMBOO || type == Material.BAMBOO_SAPLING)
                block.applyBoneMeal(BlockFace.UP);
        }
    }
}
