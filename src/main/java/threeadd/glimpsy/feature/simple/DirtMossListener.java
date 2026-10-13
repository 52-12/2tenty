package threeadd.glimpsy.feature.simple;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import threeadd.glimpsy.feature.achievement.Achievement;
import threeadd.glimpsy.feature.achievement.AchievementManager;

import java.util.concurrent.ThreadLocalRandom;

public class DirtMossListener implements Listener {

    private static final Material[] TREE_SAPLING_TYPES = {
            Material.OAK_SAPLING,
            Material.BIRCH_SAPLING,
            Material.DARK_OAK_SAPLING,
            Material.JUNGLE_SAPLING,
            Material.ACACIA_SAPLING,
            Material.SPRUCE_SAPLING,
            Material.PALE_OAK_SAPLING,
            Material.MANGROVE_PROPAGULE,
            Material.TORCHFLOWER
    };

    @EventHandler
    public void onBoneMeal(PlayerInteractEvent event) {
        if (event.getAction().isLeftClick()) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() != Material.BONE_MEAL) return;

        switch (block.getType()) {
            case DIRT -> block.setType(Material.MOSS_BLOCK);
            case ROOTED_DIRT -> {
                block.setType(Material.GRASS_BLOCK);
                AchievementManager.award(player, Achievement.A_NEW_WORLD);
            }
            case DEAD_BUSH -> block.setType(TREE_SAPLING_TYPES[ThreadLocalRandom.current().nextInt(TREE_SAPLING_TYPES.length)]);
            default -> {
                return;
            }
        }

        heldItem.subtract(1);
        block.applyBoneMeal(BlockFace.UP);
    }
}
