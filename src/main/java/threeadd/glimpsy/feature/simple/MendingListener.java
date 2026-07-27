package threeadd.glimpsy.feature.simple;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.inventory.ItemStack;
import threeadd.glimpsy.util.config.ConfigManager;

/**
 * Two Mending-related controls, both config-gated:
 * <ul>
 *     <li>{@code mending.enabled} - when false, Mending items don't repair from XP at all (the XP
 *         goes to the player as normal instead), disabling the enchantment globally.</li>
 *     <li>{@code mending.block-on-diamond-tools} - when true, Mending can't be added to diamond
 *         tools. Mending is a treasure enchantment, so the only way onto a tool is the anvil
 *         (a Mending book, or combining a Mending item), which is what this blocks.</li>
 * </ul>
 */
public class MendingListener implements Listener {

    private static boolean mendingEnabled() {
        return ConfigManager.getBoolean("mending.enabled", true);
    }

    private static boolean blockOnDiamondTools() {
        return ConfigManager.getBoolean("mending.block-on-diamond-tools", true);
    }

    @EventHandler
    public void onMend(PlayerItemMendEvent event) {
        if (!mendingEnabled()) event.setCancelled(true);
    }

    @SuppressWarnings("UnstableApiUsage")
    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        if (!blockOnDiamondTools()) return;

        ItemStack result = event.getResult();
        if (result == null || !isDiamondTool(result.getType())) return;
        if (!result.containsEnchantment(Enchantment.MENDING)) return;

        // Only strip Mending when it's being newly *added* - if the tool already had it (e.g.
        // repairing two Mending pickaxes together) leave it alone.
        ItemStack base = event.getInventory().getFirstItem();
        if (base != null && base.containsEnchantment(Enchantment.MENDING)) return;

        ItemStack cleaned = result.clone();
        cleaned.removeEnchantment(Enchantment.MENDING);
        event.setResult(cleaned);
    }

    private static boolean isDiamondTool(Material material) {
        return switch (material) {
            case DIAMOND_PICKAXE, DIAMOND_AXE, DIAMOND_SHOVEL, DIAMOND_HOE, DIAMOND_SWORD -> true;
            default -> false;
        };
    }
}
