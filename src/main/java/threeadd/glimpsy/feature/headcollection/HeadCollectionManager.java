package threeadd.glimpsy.feature.headcollection;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.manager.ListeningManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player head collections - a chest that only accepts heads, opened with /headcollections and
 * shown off in chat with [heads]. Everything a player takes back out is unrestricted; the rule is
 * only ever about what's allowed in.
 */
public class HeadCollectionManager extends ListeningManager {

    private static final Map<UUID, HeadCollection> collectionsByOwner = new ConcurrentHashMap<>();

    public static boolean enabled() {
        return ConfigManager.getBoolean("head-collection.enabled", true);
    }

    @Override
    public void onEnable() {
        collectionsByOwner.putAll(HeadCollectionRepository.getInstance().loadAllSync());
    }

    /** Every head-shaped item, not just player heads - a collection wants the mob skulls too. */
    public static boolean isHead(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) return false;

        return switch (item.getType()) {
            case PLAYER_HEAD, ZOMBIE_HEAD, CREEPER_HEAD, DRAGON_HEAD, PIGLIN_HEAD,
                 SKELETON_SKULL, WITHER_SKELETON_SKULL -> true;
            default -> false;
        };
    }

    public static HeadCollection getOrCreate(UUID owner) {
        return collectionsByOwner.computeIfAbsent(owner, HeadCollection::new);
    }

    public static @Nullable HeadCollection get(UUID owner) {
        return collectionsByOwner.get(owner);
    }

    public static void open(Player player) {
        HeadCollection collection = getOrCreate(player.getUniqueId());
        player.openInventory(new HeadCollectionInventory(collection, player.getName()).getInventory());
    }

    // Slot rules - only heads may enter, taking things back out is never restricted

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof HeadCollectionInventory collection)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Someone else's collection shared in chat - a frozen snapshot, not a container
        if (!collection.isEditable()) {
            event.setCancelled(true);
            return;
        }

        if (event.getClickedInventory() == top) {
            // Anything that would drop an item into a collection slot has to be a head; pickups,
            // drops and shift-clicks back out leave nothing behind and are always fine
            ItemStack incoming = incomingItem(event, player);
            if (incoming != null && !isHead(incoming)) reject(event, player);
            return;
        }

        // Shift-clicking from their own inventory is the one bottom-click that lands in the top
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && !isHead(event.getCurrentItem()))
            reject(event, player);
    }

    /**
     * The item this click would leave sitting in the clicked collection slot, or null if the
     * click only ever takes items out (or does nothing at all).
     */
    private static @Nullable ItemStack incomingItem(InventoryClickEvent event, Player player) {
        return switch (event.getAction()) {
            case PLACE_ALL, PLACE_SOME, PLACE_ONE, SWAP_WITH_CURSOR -> event.getCursor();
            case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> swapSource(event, player);
            // PICKUP_*, DROP_*, MOVE_TO_OTHER_INVENTORY (out of the collection), COLLECT_TO_CURSOR
            // (only ever gathers stacks matching the cursor) and NOTHING all take, never give
            default -> null;
        };
    }

    private static @Nullable ItemStack swapSource(InventoryClickEvent event, Player player) {
        if (event.getClick() == ClickType.SWAP_OFFHAND) return player.getInventory().getItemInOffHand();

        int button = event.getHotbarButton();
        return button < 0 ? null : player.getInventory().getItem(button);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof HeadCollectionInventory collection)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        boolean touchesCollection = event.getRawSlots().stream().anyMatch(slot -> slot < top.getSize());
        if (!touchesCollection) return;

        if (!collection.isEditable() || !isHead(event.getOldCursor())) {
            event.setCancelled(true);
            if (collection.isEditable()) sendOnlyHeads(player);
        }
    }

    private static void reject(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        sendOnlyHeads(player);
    }

    // Action bar rather than chat - a mis-drop in a GUI shouldn't push someone's conversation
    // off the screen, and the rule gets hit repeatedly while sorting
    private static void sendOnlyHeads(Player player) {
        player.sendActionBar(Messages.msg("head-collection.only-heads",
                "<error>Only heads can go in your head collection"));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof HeadCollectionInventory inventory)) return;
        if (!inventory.isEditable()) return; // a shared snapshot - never written back

        HeadCollection collection = getOrCreate(inventory.getOwner());
        collection.setContents(inventory.readContents());
        HeadCollectionRepository.getInstance().runAsyncSave(collection);
    }
}
