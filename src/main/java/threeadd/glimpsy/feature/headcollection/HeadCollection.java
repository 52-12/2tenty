package threeadd.glimpsy.feature.headcollection;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** One player's stored head collection - a plain chest's worth of slots, heads only. */
public class HeadCollection {

    public static final int SIZE = 27;

    private final UUID owner;
    private final @Nullable ItemStack[] contents = new ItemStack[SIZE];

    public HeadCollection(UUID owner) {
        this.owner = owner;
    }

    public UUID getOwner() {
        return owner;
    }

    public @Nullable ItemStack[] getContents() {
        return contents;
    }

    public void setContents(@Nullable ItemStack[] newContents) {
        for (int i = 0; i < SIZE; i++)
            contents[i] = i < newContents.length ? newContents[i] : null;
    }

    /** A detached copy, safe to hand to a viewer's read-only GUI or a chat snapshot. */
    public @Nullable ItemStack[] copyContents() {
        ItemStack[] copy = new ItemStack[SIZE];
        for (int i = 0; i < SIZE; i++)
            copy[i] = contents[i] == null ? null : contents[i].clone();
        return copy;
    }

    public int countHeads() {
        int count = 0;
        for (ItemStack item : contents)
            if (item != null && !item.getType().isAir()) count += item.getAmount();
        return count;
    }
}
