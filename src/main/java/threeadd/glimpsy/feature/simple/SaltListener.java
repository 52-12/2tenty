package threeadd.glimpsy.feature.simple;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceExtractEvent;
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

    // Water bucket -> Salt. A vanilla furnace recipe can't leave a container behind (the only
    // vanilla case, wet sponge, is hardcoded), and swapping the input slot from a smelt event is a
    // timing lottery. So instead the empty bucket is put back into the furnace's top (input) slot
    // when the salt is taken out - ready to refill and use again. If the slot is already occupied
    // (e.g. a hopper refilled it), the buckets go to the player so none are lost.
    @EventHandler
    public void onExtract(FurnaceExtractEvent event) {
        if (event.getItemType() != Material.WHITE_CONCRETE_POWDER) return;

        int buckets = event.getItemAmount() / 2; // recipe yields 2 salt per water bucket
        if (buckets <= 0) return;

        Block block = event.getBlock();
        if (block.getType() == Material.FURNACE) {
            Furnace furnace = (Furnace) block.getState();
            ItemStack input = furnace.getInventory().getSmelting();

            if (input == null || input.getType().isAir()) {
                int inSlot = Math.min(buckets, Material.BUCKET.getMaxStackSize());
                furnace.getInventory().setSmelting(new ItemStack(Material.BUCKET, inSlot));
                furnace.update(true);

                buckets -= inSlot;
                if (buckets <= 0) return;
            }
        }

        Player player = event.getPlayer();
        for (ItemStack leftover : player.getInventory().addItem(new ItemStack(Material.BUCKET, buckets)).values())
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
    }
}
