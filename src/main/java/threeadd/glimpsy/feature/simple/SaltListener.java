package threeadd.glimpsy.feature.simple;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import threeadd.glimpsy.feature.recipes.SaltRecipe;
import threeadd.glimpsy.feature.recipes.SaltedJerkyRecipe;
import threeadd.glimpsy.util.ScheduleUtil;

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

    // Water bucket -> Salt. A vanilla furnace recipe can't declare a container that's left behind,
    // and the old furnace-extract refund only fired when a player pulled the salt out by hand, so
    // hoppers / offline smelting silently lost the bucket. Instead the empty bucket goes straight
    // back into the furnace's top (input) slot the moment the water bucket is consumed.
    @EventHandler
    public void onSmelt(FurnaceSmeltEvent event) {
        if (event.getSource().getType() != Material.WATER_BUCKET) return;

        Block block = event.getBlock();

        // One tick later: by then vanilla has removed the water bucket from the input slot and added
        // the salt to the output, so there's somewhere to put the bucket back.
        ScheduleUtil.scheduleSync(() -> {
            if (block.getType() != Material.FURNACE) return;

            Furnace furnace = (Furnace) block.getState();
            ItemStack input = furnace.getInventory().getSmelting();
            if (input == null || input.getType().isAir()) {
                furnace.getInventory().setSmelting(new ItemStack(Material.BUCKET));
                furnace.update(true);
                return;
            }

            // The slot was refilled in the meantime (e.g. a hopper feeding water buckets) - don't
            // overwrite whatever's there, just drop the empty bucket so it isn't lost.
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 1.0, 0.5), new ItemStack(Material.BUCKET));
        }, 1, ScheduleUtil.Unit.TICK);
    }
}
