package threeadd.glimpsy.feature.simple;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.config.Messages;

import java.util.Locale;

/**
 * Config-driven anvil costs. The final level cost of every anvil operation (repair, enchant,
 * combine, rename) is: vanilla cost x cost-multiplier x the result item's entry in
 * item-multipliers - or a flat override instead (the result item's entry in item-flat-costs,
 * falling back to the global flat-cost) - then capped by max-cost.
 */
public class ExpensiveAnvilListener implements Listener {

    private static boolean removeTooExpensiveCap() {
        return ConfigManager.getBoolean("anvil.remove-too-expensive-cap", true);
    }

    private static double costMultiplier() {
        return ConfigManager.getDouble("anvil.cost-multiplier", 1.0);
    }

    private static int flatCost() {
        return ConfigManager.getInt("anvil.flat-cost", -1);
    }

    private static int maxCost() {
        return ConfigManager.getInt("anvil.max-cost", -1);
    }

    private static boolean disableCostDoubling() {
        return ConfigManager.getBoolean("anvil.disable-cost-doubling", false);
    }

    private static double itemMultiplier(Material material) {
        return ConfigManager.getDouble(
                "anvil.item-multipliers." + material.name().toLowerCase(Locale.ROOT), 1.0);
    }

    private static int itemFlatCost(Material material) {
        return ConfigManager.getInt(
                "anvil.item-flat-costs." + material.name().toLowerCase(Locale.ROOT), -1);
    }

    @SuppressWarnings("UnstableApiUsage")
    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result == null || result.getType().isAir()) return;

        if (removeTooExpensiveCap())
            event.getView().setMaximumRepairCost(Integer.MAX_VALUE);

        // Vanilla doubles a hidden per-item penalty counter on every anvil use, which is what
        // makes repeated anvil work spiral toward unaffordable - zeroing it on the result keeps
        // every operation at its base cost.
        if (disableCostDoubling()) {
            ItemMeta meta = result.getItemMeta();
            if (meta instanceof Repairable repairable && repairable.getRepairCost() > 0) {
                repairable.setRepairCost(0);
                result.setItemMeta(meta);
                event.setResult(result);
            }
        }

        int cost = event.getView().getRepairCost();
        if (cost <= 0) return;

        int itemFlat = itemFlatCost(result.getType());
        if (itemFlat >= 0) {
            cost = itemFlat;
        } else if (flatCost() >= 0) {
            cost = flatCost();
        } else {
            cost = (int) Math.round(cost * costMultiplier() * itemMultiplier(result.getType()));
        }

        if (maxCost() >= 0) cost = Math.min(cost, maxCost());

        event.getView().setRepairCost(cost);

        // Past vanilla's usual 40-level "Too Expensive" point the GUI hides the real price, so
        // tell the player what they'd actually pay.
        if (removeTooExpensiveCap() && cost >= 40) {
            event.getView().getPlayer().sendMessage(Messages.msg("anvil.cost-notice",
                    "This anvil upgrade costs you: <#F8DD97>{cost} Levels", "cost", String.valueOf(cost)));
        }
    }
}
