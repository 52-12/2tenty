package threeadd.glimpsy.feature.simple;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
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
    // hoppers / offline smelting silently lost the bucket. Instead the empty bucket is put into the
    // furnace's top (input) slot once the water bucket has been consumed.
    @EventHandler
    public void onStartSmelt(FurnaceStartSmeltEvent event) {
        if (event.getSource().getType() != Material.WATER_BUCKET) return;

        // Scheduled for when this smelt finishes, not for "now": FurnaceSmeltEvent fires around the
        // point the input is consumed, so a 1-tick check could still see the water bucket and give
        // up. getTotalCookTime() tells us exactly how long to wait.
        Block block = event.getBlock();
        ScheduleUtil.scheduleSync(() -> placeEmptyBucket(block, 0), event.getTotalCookTime() + 1, ScheduleUtil.Unit.TICK);
    }

    private static void placeEmptyBucket(Block block, int attempt) {
        if (block.getType() != Material.FURNACE) return;

        Furnace furnace = (Furnace) block.getState();
        ItemStack input = furnace.getInventory().getSmelting();

        if (input == null || input.getType().isAir()) {
            furnace.getInventory().setSmelting(new ItemStack(Material.BUCKET));
            furnace.update(true);
            return;
        }

        // Water bucket still sitting there (timing drift) - wait a moment and look again.
        if (input.getType() == Material.WATER_BUCKET) {
            if (attempt < 10)
                ScheduleUtil.scheduleSync(() -> placeEmptyBucket(block, attempt + 1), 2, ScheduleUtil.Unit.TICK);

            return; // still smelting (or it stalled) - don't hand out a bucket for a smelt that hasn't happened
        }

        // Something else refilled the slot - drop the empty bucket on top so it isn't lost.
        block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 1.0, 0.5), new ItemStack(Material.BUCKET));
    }
}
