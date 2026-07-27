package threeadd.glimpsy.feature.simple;

import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

public class VillagerTradeListener implements Listener {

    // Cancelling leaves the villager unemployed at that job site rather than assigning it -
    // they're free to take a different job site instead, but can never settle into these three.
    @EventHandler
    public void onCareerChange(VillagerCareerChangeEvent event) {
        if (event.getProfession() == Villager.Profession.ARMORER
                || event.getProfession() == Villager.Profession.LIBRARIAN
                || event.getProfession() == Villager.Profession.WEAPONSMITH)
            event.setCancelled(true);
    }

    @EventHandler
    public void onAcquireTrade(VillagerAcquireTradeEvent event) {
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
