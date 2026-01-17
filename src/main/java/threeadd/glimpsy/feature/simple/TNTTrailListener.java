package threeadd.glimpsy.feature.simple;

import com.destroystokyo.paper.ParticleBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import threeadd.glimpsy.util.ScheduleUtil;

import java.util.*;

public class TNTTrailListener implements Listener {

    private static final int OVERALL_MAX_TNT_TRAIL = 1000;
    private static final int TNT_TRAIL_EXTENSION = 20;
    private static final ParticleBuilder BROKEN_PARTICLE = Particle.CLOUD.builder()
            .extra(0d)
            .offset(0.5, 0.5, 0.5);
    private static final List<BlockFace> neighbours = Arrays.stream(BlockFace.values()).toList(); // U can change this to make it work directionally

    public static void explode(Location location, int blocksAffected) {
        Block contactBlock = findFirstSolidBlock(location);
        if (contactBlock == null) return; // too far from actual blocks

        startTNTTrail(contactBlock, blocksAffected);
    }

    private static Block findFirstSolidBlock(Location originLocation) {
        Block origin = originLocation.getBlock();
        if (isBreakable(origin)) return origin;

        for (BlockFace face : neighbours) {
            Block relative = origin.getRelative(face);
            if (isBreakable(relative)) return relative;
        }
        return null;
    }

    private static void startTNTTrail(Block start, int blocksAffected) {
        Set<Block> visited = new HashSet<>();
        Queue<Block> currentWave = new ArrayDeque<>();

        currentWave.add(start);

        processWave(currentWave, visited, blocksAffected, 0);
    }

    private static void processWave(Queue<Block> wave, Set<Block> visited, int remaining, int totalBroken) {
        Queue<Block> nextWave = new ArrayDeque<>();

        int brokenThisWave = 0;

        while (!wave.isEmpty() && remaining > 0) {
            Block block = wave.poll();

            if (totalBroken >= OVERALL_MAX_TNT_TRAIL) {
                return;
            }

            if (!visited.add(block)) continue;

            boolean isTNT = block.getType() == Material.TNT;

            if (isTNT) {
                block.setType(Material.AIR);

                remaining += TNT_TRAIL_EXTENSION;

                remaining--; // Decrement for the TNT block itself
                brokenThisWave++;

            } else if (!isBreakable(block)) {
                continue;
            } else {
                block.breakNaturally(ItemStack.of(Material.STONE_PICKAXE));
                BROKEN_PARTICLE.location(block.getLocation()).spawn();

                remaining--;
                brokenThisWave++;
            }

            for (BlockFace face : neighbours) {
                Block n = block.getRelative(face);
                if (!visited.contains(n) && (isBreakable(n) || n.getType() == Material.TNT)) {
                    nextWave.add(n);
                }
            }
        }

        if (brokenThisWave == 0) return; // nothing else to break

        int finalRemaining = remaining;
        ScheduleUtil.scheduleSync(
                () -> processWave(nextWave, visited, finalRemaining, totalBroken),
                2,
                ScheduleUtil.Unit.TICK); // run next wave 2 tick later
    }

    private static boolean isBreakable(Block block) {
        if (!block.isSolid()) return false;

        return switch (block.getType()) {
            case BEDROCK, BARRIER -> false;
            default -> true;
        };
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if (!event.getEntity().getType().equals(EntityType.TNT))
            return;
        event.setCancelled(true);

        explode(event.getLocation(), TNT_TRAIL_EXTENSION);
    }
}
