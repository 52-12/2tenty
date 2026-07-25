package threeadd.glimpsy.feature.simple;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Mycelium is now craft-only (see MyceliumRecipe) - without Silk Touch it already drops dirt
 * like vanilla, but Silk Touch would normally preserve the block itself. This makes Silk Touch
 * drop dirt too, so mycelium can't be farmed from the world at all.
 */
public class MyceliumSilkTouchListener implements Listener {

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.MYCELIUM) return;

        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (!tool.containsEnchantment(Enchantment.SILK_TOUCH)) return;

        event.setDropItems(false);
        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.DIRT));
    }
}
