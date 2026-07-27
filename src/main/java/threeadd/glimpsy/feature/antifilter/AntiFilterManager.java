package threeadd.glimpsy.feature.antifilter;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.manager.ListeningManager;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player anti-drop / anti-pickup filter. Each filter is a set of protected materials, shown
 * in a menu of display-only marker copies (see {@link AntiFilterInventory}). While a material is
 * protected, that player can't manually drop (anti-drop) or auto-pick-up (anti-pickup) items of
 * that type. The menu never holds or hands out real items - placing one just copies its type in.
 */
public class AntiFilterManager extends ListeningManager {

    private static final Map<UUID, Map<AntiFilterType, Set<Material>>> filters = new ConcurrentHashMap<>();

    private static boolean enabled() {
        return ConfigManager.getBoolean("anti-filter.enabled", true);
    }

    public static boolean isEnabled() {
        return enabled();
    }

    @Override
    public void onEnable() {
        filters.putAll(AntiFilterRepository.getInstance().loadAllSync());
    }

    public static void open(Player player, AntiFilterType type) {
        AntiFilterInventory gui = new AntiFilterInventory(player.getUniqueId(), type);
        gui.render(getSet(player.getUniqueId(), type));
        player.openInventory(gui.getInventory());
    }

    private static Set<Material> getSet(UUID playerId, AntiFilterType type) {
        return filters.computeIfAbsent(playerId, x -> new EnumMap<>(AntiFilterType.class))
                .computeIfAbsent(type, x -> new HashSet<>());
    }

    private static boolean isProtected(UUID playerId, AntiFilterType type, Material material) {
        Map<AntiFilterType, Set<Material>> byType = filters.get(playerId);
        if (byType == null) return false;
        Set<Material> set = byType.get(type);
        return set != null && set.contains(material);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AntiFilterInventory gui)) return;

        event.setCancelled(true); // this is a menu - no real item ever moves
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Set<Material> set = getSet(gui.getOwner(), gui.getType());
        boolean changed = false;

        if (event.getClickedInventory() == event.getInventory()) {
            // A click in the menu: on a marker removes that type; with an item on the cursor adds it
            ItemStack marker = event.getCurrentItem();
            ItemStack cursor = event.getCursor();

            if (marker != null && !marker.getType().isAir()) {
                changed = set.remove(marker.getType());
            } else if (cursor != null && !cursor.getType().isAir() && cursor.getType().isItem()) {
                changed = set.add(cursor.getType());
            }
        } else {
            // A click on any item in the player's own inventory adds its type (no shift needed).
            // Everything's cancelled, so the item never actually moves.
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && !clicked.getType().isAir() && clicked.getType().isItem())
                changed = set.add(clicked.getType());
        }

        if (changed) {
            AntiFilterRepository.getInstance().runAsyncSave(gui.getOwner(), gui.getType(), set);
            gui.render(set);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof AntiFilterInventory)
            event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!enabled()) return;

        Player player = event.getPlayer();
        ItemStack stack = event.getItemDrop().getItemStack();
        if (!isProtected(player.getUniqueId(), AntiFilterType.DROP, stack.getType())) return;

        // Prevent the drop by despawning the dropped entity and putting the stack straight back
        // into the inventory (rather than cancelling, which can lose the item if a pickup fills
        // the freed slot before the cancel returns it). This all happens synchronously in this
        // handler, so no pickup can interleave. If the inventory is genuinely full, re-spawn the
        // item on the ground instead of deleting it.
        event.getItemDrop().remove();
        for (ItemStack leftover : player.getInventory().addItem(stack).values())
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof AntiFilterInventory)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        // Safety for the rare case of an item left on the cursor when the menu closes: give it
        // back to the inventory, and if that's full drop it via the world (not a player-drop, so
        // anti-drop can't swallow it) rather than letting it vanish.
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            player.setItemOnCursor(null);
            for (ItemStack leftover : player.getInventory().addItem(cursor).values())
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!enabled()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        if (isProtected(player.getUniqueId(), AntiFilterType.PICKUP,
                event.getItem().getItemStack().getType()))
            event.setCancelled(true);
    }
}
