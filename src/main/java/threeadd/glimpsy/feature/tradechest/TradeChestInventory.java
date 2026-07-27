package threeadd.glimpsy.feature.tradechest;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.util.config.Messages;

import java.util.UUID;

/**
 * The 27-slot GUI of a placed Trade Chest. Deliberately NOT a CustomInventory - slots 0-24 are
 * real, freely editable item slots (CustomInventory cancels all clicks), with only the last two
 * slots (gray filler + action button) managed. Click/drag/close handling lives in
 * {@link TradeChestManager}'s listeners.
 */
public class TradeChestInventory implements InventoryHolder {

    public static final int ITEM_SLOTS = TradeChestData.SIZE; // 0-24
    public static final int LEFT_BUTTON_SLOT = 25;
    public static final int RIGHT_BUTTON_SLOT = 26;

    private final Inventory inventory;
    private final UUID owner;

    public TradeChestInventory(TradeChestData data) {
        this.owner = data.getOwner();
        this.inventory = Bukkit.createInventory(this, 27,
                Messages.blackTitle("trade-chest.chest-title", "Trade Chest"));

        ItemStack[] contents = data.getContents();
        for (int i = 0; i < ITEM_SLOTS; i++)
            inventory.setItem(i, contents[i]);

        refreshButtons();
    }

    public void refreshButtons() {
        inventory.setItem(LEFT_BUTTON_SLOT, TradeChestManager.buildLeftButton(owner));
        inventory.setItem(RIGHT_BUTTON_SLOT, TradeChestManager.buildRightButton(owner));
    }

    /** The current 25 item slots, with empty slots as null. */
    public @Nullable ItemStack[] readContents() {
        ItemStack[] contents = new ItemStack[ITEM_SLOTS];
        for (int i = 0; i < ITEM_SLOTS; i++) {
            ItemStack item = inventory.getItem(i);
            contents[i] = item == null || item.getType().isAir() ? null : item.clone();
        }
        return contents;
    }

    public UUID getOwner() {
        return owner;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
