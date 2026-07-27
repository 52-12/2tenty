package threeadd.glimpsy.feature.simple;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import threeadd.glimpsy.util.config.ConfigManager;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class VillagerTradeListener implements Listener {

    // Professions a villager can never settle into - defaults to the three that would otherwise
    // sell enchanted/diamond gear. Overridable via config (villager.blocked-professions).
    private static final Villager.Profession[] DEFAULT_BLOCKED = {
            Villager.Profession.ARMORER, Villager.Profession.LIBRARIAN, Villager.Profession.WEAPONSMITH
    };

    private static Set<Villager.Profession> blockedProfessions() {
        List<String> names = ConfigManager.getStringList("villager.blocked-professions");
        if (names.isEmpty()) return Set.of(DEFAULT_BLOCKED);

        Set<Villager.Profession> blocked = new HashSet<>();
        for (String name : names) {
            try {
                Villager.Profession profession = Registry.VILLAGER_PROFESSION.get(
                        NamespacedKey.minecraft(name.trim().toLowerCase(Locale.ROOT)));
                if (profession != null) blocked.add(profession);
            } catch (IllegalArgumentException ignored) {
                // A malformed profession name in config - skip it
            }
        }
        return blocked;
    }

    private static boolean stripDiamondToolEnchants() {
        return ConfigManager.getBoolean("villager.strip-diamond-tool-enchants", true);
    }

    // Cancelling leaves the villager unemployed at that job site rather than assigning it -
    // they're free to take a different job site instead, but can never settle into a blocked one.
    @EventHandler
    public void onCareerChange(VillagerCareerChangeEvent event) {
        if (blockedProfessions().contains(event.getProfession()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onAcquireTrade(VillagerAcquireTradeEvent event) {
        if (!stripDiamondToolEnchants()) return;
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (villager.getProfession() != Villager.Profession.TOOLSMITH) return;

        MerchantRecipe recipe = event.getRecipe();
        ItemStack result = recipe.getResult();
        if (!isDiamondTool(result.getType()) || result.getEnchantments().isEmpty()) return;

        ItemStack cleaned = result.clone();
        cleaned.removeEnchantments();

        MerchantRecipe newRecipe = new MerchantRecipe(
                cleaned,
                recipe.getUses(),
                recipe.getMaxUses(),
                recipe.hasExperienceReward(),
                recipe.getVillagerExperience(),
                recipe.getPriceMultiplier(),
                recipe.getDemand(),
                recipe.getSpecialPrice(),
                recipe.shouldIgnoreDiscounts());
        newRecipe.setIngredients(recipe.getIngredients());

        event.setRecipe(newRecipe);
    }

    private boolean isDiamondTool(org.bukkit.Material material) {
        return switch (material) {
            case DIAMOND_PICKAXE, DIAMOND_AXE, DIAMOND_SHOVEL, DIAMOND_HOE, DIAMOND_SWORD -> true;
            default -> false;
        };
    }
}
