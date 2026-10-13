package threeadd.glimpsy.feature.simple;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threeadd.glimpsy.feature.recipes.SaltRecipe;
import threeadd.glimpsy.feature.recipes.SaltedJerkyRecipe;

public class SaltListener implements Listener {

    private static final Logger log = LoggerFactory.getLogger(SaltListener.class);

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

    // Water bucket -> Salt. A vanilla furnace recipe has no way to leave a container behind (the
    // only vanilla case, wet sponge holding a bucket, is hardcoded), and FurnaceSmeltEvent fires
    // just before vanilla shrinks the input - so anything done a tick later is a race. Instead we
    // take the smelt over completely *inside* the event: cancel the vanilla outcome, drop the
    // empty bucket back into the input slot, and place the Salt ourselves. No timing window.
    @EventHandler(priority = EventPriority.HIGH)
    public void onSmelt(FurnaceSmeltEvent event) {
        if (event.getSource().getType() != Material.WATER_BUCKET) return;
        if (!SaltRecipe.isSalt(event.getResult())) return;

        ItemStack salt = SaltRecipe.createSaltItem(2);

        Block block = event.getBlock();
        Furnace furnace = (Furnace) block.getState();
        ItemStack output = furnace.getInventory().getResult();

        // If the output somehow holds a different item, don't interfere - vanilla's own "is there
        // room" check means this shouldn't happen, but cancelling without taking over would loop.
        if (output != null && !output.getType().isAir() && !output.isSimilar(salt)) return;

        event.setCancelled(true);

        if (output == null || output.getType().isAir()) {
            furnace.getInventory().setResult(salt);
        } else {
            ItemStack grown = salt.clone();
            grown.setAmount(output.getAmount() + salt.getAmount());
            furnace.getInventory().setResult(grown);
        }

        furnace.getInventory().setSmelting(new ItemStack(Material.BUCKET));
        furnace.update(true);

        log.info("Salt smelted - returned an empty bucket to the furnace input slot");
    }
}
