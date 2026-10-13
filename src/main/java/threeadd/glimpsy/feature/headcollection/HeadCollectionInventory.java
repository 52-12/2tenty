package threeadd.glimpsy.feature.headcollection;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.util.config.Messages;

import java.util.UUID;

/**
 * A player's head collection as a plain 27-slot chest. Deliberately NOT a CustomInventory - every
 * slot is a real, editable item slot (CustomInventory cancels all clicks); the "heads only" rule
 * and the saving are enforced by {@link HeadCollectionManager}'s listeners.
 * <p>
 * An instance opened for someone other than the owner (the {@code [heads]} chat share) is a
 * read-only snapshot: clicks are cancelled and nothing is ever written back.
 */
public class HeadCollectionInventory implements InventoryHolder {

    private final Inventory inventory;
    private final UUID owner;
    private final boolean editable;

    /** The owner's own, editable collection. */
    public HeadCollectionInventory(HeadCollection collection, String ownerName) {
        this(collection.getOwner(), ownerName, collection.getContents(), true);
    }

    /** A frozen, look-but-don't-touch copy of someone else's collection. */
    public static HeadCollectionInventory snapshot(UUID owner, String ownerName, ItemStack[] contents) {
        return new HeadCollectionInventory(owner, ownerName, contents, false);
    }

    private HeadCollectionInventory(UUID owner, String ownerName, @Nullable ItemStack[] contents, boolean editable) {
        this.owner = owner;
        this.editable = editable;
        this.inventory = Bukkit.createInventory(this, HeadCollection.SIZE,
                Messages.blackTitle("head-collection.title", "{player}'s Head Collection", "player", ownerName));

        for (int i = 0; i < HeadCollection.SIZE && i < contents.length; i++)
            inventory.setItem(i, contents[i]);
    }

    /** The current slots, with empty slots as null. */
    public @Nullable ItemStack[] readContents() {
        ItemStack[] contents = new ItemStack[HeadCollection.SIZE];
        for (int i = 0; i < HeadCollection.SIZE; i++) {
            ItemStack item = inventory.getItem(i);
            contents[i] = item == null || item.getType().isAir() ? null : item.clone();
        }
        return contents;
    }

    public UUID getOwner() {
        return owner;
    }

    public boolean isEditable() {
        return editable;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
