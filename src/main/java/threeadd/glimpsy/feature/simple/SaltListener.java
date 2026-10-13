package threeadd.glimpsy.feature.simple;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import threeadd.glimpsy.feature.recipes.SaltRecipe;
import threeadd.glimpsy.feature.recipes.SaltedJerkyRecipe;

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

    // Water bucket -> Salt. A furnace recipe can't declare a container to leave behind (the only
    // vanilla case, wet sponge, is hardcoded), so we swap the consumed water bucket for an empty
    // bucket ourselves - here, while the smelt is being processed, because FurnaceSmeltEvent runs
    // just before vanilla adds the Salt and shrinks the (already-captured) water-bucket stack.
    // Net effect: Salt in the output, empty bucket back in the input slot.
    //
    // getInventory() on a PLACED furnace returns the live block entity (CraftFurnace), so the slot
    // change applies immediately - and update() must NOT be called, or it writes the stale state
    // snapshot back and undoes both this and vanilla's own result.
    @EventHandler(priority = EventPriority.HIGH)
    public void onSmelt(FurnaceSmeltEvent event) {
        if (event.getSource().getType() != Material.WATER_BUCKET) return;
        if (!SaltRecipe.isSalt(event.getResult())) return;
        if (!(event.getBlock().getState() instanceof Furnace furnace)) return;

        furnace.getInventory().setSmelting(new ItemStack(Material.BUCKET));
    }
}
