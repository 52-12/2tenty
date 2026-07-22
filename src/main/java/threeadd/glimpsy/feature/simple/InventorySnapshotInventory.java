package threeadd.glimpsy.feature.simple;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;
import threeadd.glimpsy.util.inventory.CustomInventory;

public class InventorySnapshotInventory extends CustomInventory {

    public InventorySnapshotInventory(String ownerName, ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {
        super(Rows.FIVE, Component.text(ownerName + "'s Inventory").color(NamedTextColor.YELLOW));

        // contents indices: 0-8 hotbar, 9-17 top main row, 18-26 middle main row, 27-35 bottom main row
        placeRow(0, contents, 0);   // hotbar -> GUI row 1 (unchanged)
        placeRow(9, contents, 27);  // bottom main row -> GUI row 2 (was last)
        placeRow(18, contents, 18); // middle main row -> GUI row 3 (unchanged)
        placeRow(27, contents, 9);  // top main row -> GUI row 4

        for (int i = 0; i < armor.length; i++) {
            ItemStack piece = armor[i];
            if (piece == null || piece.isEmpty()) continue;

            setSlot(36 + i, piece); // helmet, chestplate, leggings, boots
        }

        if (offhand != null && !offhand.isEmpty())
            setSlot(40, offhand);
    }

    private void placeRow(int guiStart, ItemStack[] source, int sourceStart) {
        for (int i = 0; i < 9; i++) {
            ItemStack item = source[sourceStart + i];
            if (item == null || item.isEmpty()) continue;

            setSlot(guiStart + i, item);
        }
    }
}
