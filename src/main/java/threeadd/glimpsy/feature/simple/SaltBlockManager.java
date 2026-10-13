package threeadd.glimpsy.feature.simple;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import threeadd.glimpsy.feature.recipes.SaltRecipe;
import threeadd.glimpsy.util.manager.ListeningManager;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks player-placed Salt blocks so they behave like Salt rather than like the white concrete
 * powder they're made of: breaking one drops Salt again (not plain powder) and water can't harden
 * it into concrete. Positions are persisted, so tracking survives restarts.
 */
public class SaltBlockManager extends ListeningManager {

    private static final Set<SaltBlockRepository.Pos> placedSalt = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        placedSalt.addAll(SaltBlockRepository.getInstance().loadAllSync());
    }

    private static SaltBlockRepository.Pos posOf(Block block) {
        return new SaltBlockRepository.Pos(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (!SaltRecipe.isSalt(event.getPlayer().getInventory().getItem(event.getHand()))) return;

        SaltBlockRepository.Pos pos = posOf(event.getBlockPlaced());
        placedSalt.add(pos);
        SaltBlockRepository.getInstance().addAsync(pos);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (!forget(event.getBlock())) return;

        event.setDropItems(false);
        event.getBlock().getWorld().dropItemNaturally(
                event.getBlock().getLocation().add(0.5, 0.5, 0.5), SaltRecipe.createSaltItem(1));
    }

    // Salt is white concrete powder under the hood; cancelling the form event keeps a placed Salt
    // block from hardening into concrete the moment water touches it.
    @EventHandler
    public void onForm(BlockFormEvent event) {
        if (event.getNewState().getType() != Material.WHITE_CONCRETE) return;
        if (placedSalt.contains(posOf(event.getBlock()))) event.setCancelled(true);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        dropSaltFrom(event.blockList());
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        dropSaltFrom(event.blockList());
    }

    private void dropSaltFrom(List<Block> blocks) {
        for (Block block : blocks) {
            if (!forget(block)) continue;
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), SaltRecipe.createSaltItem(1));
        }
    }

    /** Removes the position from the tracked set, returning whether it was a placed Salt block. */
    private static boolean forget(Block block) {
        SaltBlockRepository.Pos pos = posOf(block);
        if (!placedSalt.remove(pos)) return false;

        SaltBlockRepository.getInstance().removeAsync(pos);
        return true;
    }
}
