package threeadd.glimpsy.feature.simple;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import threeadd.glimpsy.util.ScheduleUtil;
import threeadd.glimpsy.util.config.ConfigManager;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Vanilla suspicious sand is immune to gravity so archaeology sites don't collapse. This spawns
 * a real FallingBlock entity so it falls with genuine vanilla physics - a hand-rolled tick
 * simulation got the "can this surface support a full block" check wrong for slabs. On a
 * successful landing it settles back into brushable suspicious sand; if vanilla can't place it
 * (slab, torch, anything less than a full block) the entity just dies without ever firing a
 * landing event, which is how we tell success and failure apart.
 */
public class SuspiciousSandFallListener implements Listener {

    private static double sandDropChance() {
        return ConfigManager.getDouble("suspicious-sand.drop-chance-on-break", 0.5);
    }

    // Falling blocks we spawned that haven't been resolved yet - removed either by onLand
    // (landed successfully) or by trackUntilResolved noticing the entity died without landing.
    private static final Set<UUID> pendingFalls = ConcurrentHashMap.newKeySet();

    @EventHandler
    public void onPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SUSPICIOUS_SAND) return;

        Block below = block.getRelative(BlockFace.DOWN);
        if (below.getType() != Material.AIR) return; // supported, nothing to do

        BlockData data = block.getBlockData();
        Location spawnLocation = block.getLocation().add(0.5, 0, 0.5);
        block.setType(Material.AIR);

        FallingBlock fallingBlock = block.getWorld().spawnFallingBlock(spawnLocation, data);
        fallingBlock.setDropItem(false); // we roll our own drop below
        fallingBlock.setHurtEntities(false);

        pendingFalls.add(fallingBlock.getUniqueId());
        trackUntilResolved(fallingBlock, spawnLocation);
    }

    private void trackUntilResolved(FallingBlock fallingBlock, Location lastKnownLocation) {
        if (!pendingFalls.contains(fallingBlock.getUniqueId())) return; // already resolved by onLand

        if (!fallingBlock.isValid()) {
            pendingFalls.remove(fallingBlock.getUniqueId());
            rollBreakDrop(lastKnownLocation);
            return;
        }

        ScheduleUtil.scheduleSync(() -> trackUntilResolved(fallingBlock, fallingBlock.getLocation()), 1, ScheduleUtil.Unit.TICK);
    }

    @EventHandler
    public void onLand(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock fallingBlock)) return;
        pendingFalls.remove(fallingBlock.getUniqueId());
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.SUSPICIOUS_SAND) return;

        event.setDropItems(false);
        rollBreakDrop(event.getBlock().getLocation());
    }

    private void rollBreakDrop(Location location) {
        if (ThreadLocalRandom.current().nextDouble() < sandDropChance())
            location.getWorld().dropItemNaturally(location, new ItemStack(Material.SAND));
    }
}
