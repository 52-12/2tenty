package threeadd.glimpsy.feature.simple;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import threeadd.glimpsy.feature.recipes.SaltRecipe;
import threeadd.glimpsy.feature.recipes.SaltedJerkyRecipe;

import java.util.Map;

public class SaltListener implements Listener {

    // Salt is white concrete powder under the hood, so it would otherwise silently work in
    // every vanilla concrete powder recipe (recipe matching is by Material, not display
    // name/PDC). Block any result that isn't our own Salted Jerky recipe whenever Salt is in
    // the crafting grid.
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        boolean hasSalt = false;
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && SaltRecipe.isSalt(item)) {
                hasSalt = true;
                break;
            }
        }

        if (!hasSalt) return;

        Recipe recipe = event.getRecipe();
        if (recipe instanceof Keyed keyed && keyed.getKey().equals(SaltedJerkyRecipe.KEY)) return;

        event.getInventory().setResult(null);
    }

    // Vanilla has no furnace recipe that outputs white concrete powder, so any extracted from a
    // furnace can only be our Salt recipe - return the player's bucket empty (1 per 2 salt).
    @EventHandler
    public void onExtract(FurnaceExtractEvent event) {
        if (event.getItemType() != Material.WHITE_CONCRETE_POWDER) return;

        int buckets = event.getItemAmount() / 2;
        if (buckets <= 0) return;

        Player player = event.getPlayer();
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack(Material.BUCKET, buckets));
        for (ItemStack item : leftover.values())
            player.getWorld().dropItem(player.getLocation(), item);
    }
}
