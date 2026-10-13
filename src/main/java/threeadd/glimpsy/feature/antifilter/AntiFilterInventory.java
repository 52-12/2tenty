package threeadd.glimpsy.feature.antifilter;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.Repairable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.inventory.ItemBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The anti-drop / anti-pickup menu. It's not storage - it only shows a display copy ("marker") of
 * each protected item sample. Placing an item copies it in as a protected sample (the player keeps
 * their real item); clicking a marker unprotects it. All real item movement is cancelled by
 * {@link AntiFilterManager}, so nothing can be stored or duplicated out.
 */
public class AntiFilterInventory implements InventoryHolder {

    public static final int SIZE = 27;

    private final Inventory inventory;
    private final UUID owner;
    private final AntiFilterType type;
    // The sample shown in each slot, so a click on slot N maps back to the right sample.
    private List<ItemStack> shown = new ArrayList<>();

    public AntiFilterInventory(UUID owner, AntiFilterType type) {
        this.owner = owner;
        this.type = type;
        this.inventory = Bukkit.createInventory(this, SIZE, Messages.blackTitle(type.titleKey(), type.titleDefault()));
    }

    /** Rebuilds the display: one marker per protected sample. */
    public void render(List<ItemStack> samples) {
        inventory.clear();
        shown = new ArrayList<>();

        for (int i = 0; i < samples.size() && i < SIZE; i++) {
            ItemStack sample = samples.get(i);
            inventory.setItem(i, marker(sample));
            shown.add(sample);
        }
    }

    /** The protected sample displayed in a given slot, or null if that slot has no marker. */
    public @Nullable ItemStack sampleAt(int slot) {
        return slot >= 0 && slot < shown.size() ? shown.get(slot) : null;
    }

    private static ItemStack marker(ItemStack sample) {
        // Show a clean copy: a protected item is matched ignoring durability, so displaying its
        // current wear/damage bar would just be confusing - a damaged pickaxe shows as a fresh one.
        ItemStack display = sample.clone();
        display.editMeta(meta -> {
            if (meta instanceof Damageable damageable) damageable.setDamage(0);
            if (meta instanceof Repairable repairable) repairable.setRepairCost(0);
        });

        return new ItemBuilder(display)
                .withLore(Messages.msg("antifilter.marker-lore", "<muted>Click to stop protecting this item"))
                .build();
    }

    public UUID getOwner() {
        return owner;
    }

    public AntiFilterType getType() {
        return type;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
