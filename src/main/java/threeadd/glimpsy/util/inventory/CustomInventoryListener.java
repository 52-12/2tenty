package threeadd.glimpsy.util.inventory;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class CustomInventoryListener implements Listener {

    // Dragging places items across slots without ever firing InventoryClickEvent, so it
    // bypasses the click-cancellation below entirely - a player could drag real items into
    // the fake GUI slots (e.g. the /recipes display) and lose them once the GUI is discarded.
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof CustomInventory) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // COLLECT_TO_CURSOR (double-click) and MOVE_TO_OTHER_INVENTORY (shift-click) both
        // move items across inventories while getClickedInventory() reports the inventory
        // the click *originated* from - e.g. a shift-click in the player's own inventory
        // reports the bottom inventory as clicked, even though the item lands in the top
        // GUI. Relying on the per-slot cancellation below would miss these entirely, letting
        // real items get shifted into the fake GUI's genuinely-empty slots (e.g. blank
        // recipe-grid positions) and lost once the GUI is discarded.
        if (event.getView().getTopInventory().getHolder() instanceof CustomInventory
                && (event.getAction() == InventoryAction.COLLECT_TO_CURSOR
                || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY)) {
            event.setCancelled(true);
            return;
        }

        Inventory inventory = event.getClickedInventory();
        if (inventory == null) return;

        if (!(inventory.getHolder() instanceof CustomInventory customInventory))
            return;

        customInventory.handleSlotClickEvent(event);
    }
}
