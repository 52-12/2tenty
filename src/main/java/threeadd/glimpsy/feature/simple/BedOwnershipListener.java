package threeadd.glimpsy.feature.simple;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import threeadd.glimpsy.feature.teams.TeamManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A bed only drops its normal bed item when broken by whoever placed it, one of their
 * teammates, or an allied team's member - anyone else breaking it gets the bed falling apart
 * into 1 stick and 2 string instead, discouraging raiding someone else's bed for a free bed item.
 */
public class BedOwnershipListener implements Listener {

    private static final Map<Block, UUID> bedOwners = new ConcurrentHashMap<>();

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (!(block.getBlockData() instanceof Bed bed)) return;

        UUID owner = event.getPlayer().getUniqueId();
        bedOwners.put(block, owner);
        bedOwners.put(otherHalf(block, bed), owner);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!(block.getBlockData() instanceof Bed bed)) return;

        UUID owner = bedOwners.remove(block);
        bedOwners.remove(otherHalf(block, bed));

        if (owner == null) return; // untracked (placed before this feature, or by worldgen) - leave vanilla drops alone

        Player breaker = event.getPlayer();
        if (breaker.getUniqueId().equals(owner) || TeamManager.isSameTeamOrAllied(breaker.getUniqueId(), owner)) return;

        event.setDropItems(false);
        block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.STICK, 1));
        block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.STRING, 2));
    }

    // The bed's facing direction points from its foot end towards its head end.
    private Block otherHalf(Block block, Bed bed) {
        return switch (bed.getPart()) {
            case HEAD -> block.getRelative(bed.getFacing().getOppositeFace());
            case FOOT -> block.getRelative(bed.getFacing());
        };
    }
}
