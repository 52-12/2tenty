package threeadd.glimpsy.feature.antifilter;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.inventory.ItemBuilder;

import java.util.Set;
import java.util.UUID;

/**
 * The anti-drop / anti-pickup menu. It's not storage - it only shows a display copy ("marker")
 * of each protected material. Placing an item registers its type (a copy appears here, the
 * player keeps their real item); clicking a marker unprotects that type. All real item movement
 * is cancelled by {@link AntiFilterManager}, so nothing can be stored or duplicated out.
 */
public class AntiFilterInventory implements InventoryHolder {

    public static final int SIZE = 27;

    private final Inventory inventory;
    private final UUID owner;
    private final AntiFilterType type;

    public AntiFilterInventory(UUID owner, AntiFilterType type) {
        this.owner = owner;
        this.type = type;
        this.inventory = Bukkit.createInventory(this, SIZE, Messages.blackTitle(type.titleKey(), type.titleDefault()));
    }

    /** Rebuilds the display: one marker per protected material. */
    public void render(Set<Material> materials) {
        inventory.clear();

        int slot = 0;
        for (Material material : materials) {
            if (slot >= SIZE) break;
            if (!material.isItem()) continue; // can't show a display copy of a non-item material
            inventory.setItem(slot++, marker(material));
        }
    }

    private static ItemStack marker(Material material) {
        return new ItemBuilder(material)
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
