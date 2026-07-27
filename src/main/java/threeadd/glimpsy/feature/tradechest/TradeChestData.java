package threeadd.glimpsy.feature.tradechest;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class TradeChestData {

    public static final int SIZE = 25;

    private final UUID owner;
    private final Location location;
    private final @Nullable ItemStack[] contents = new ItemStack[SIZE];

    public TradeChestData(UUID owner, Location location) {
        this.owner = owner;
        this.location = location;
    }

    public UUID getOwner() {
        return owner;
    }

    public Location getLocation() {
        return location;
    }

    public @Nullable ItemStack[] getContents() {
        return contents;
    }

    public void setContents(@Nullable ItemStack[] newContents) {
        for (int i = 0; i < SIZE; i++)
            contents[i] = i < newContents.length ? newContents[i] : null;
    }

    /**
     * @return true if the stack was placed into an empty slot, false if the chest is full
     */
    public boolean addItem(ItemStack item) {
        for (int i = 0; i < SIZE; i++) {
            if (contents[i] == null || contents[i].getType().isAir()) {
                contents[i] = item;
                return true;
            }
        }
        return false;
    }

    public int countItems() {
        int count = 0;
        for (ItemStack item : contents)
            if (item != null && !item.getType().isAir()) count++;
        return count;
    }
}
