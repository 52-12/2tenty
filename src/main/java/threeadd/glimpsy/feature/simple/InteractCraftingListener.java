package threeadd.glimpsy.feature.simple;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InteractCraftingListener implements Listener {

    // Per-player opt-out (default on for everyone), session-only like the other toggles
    private static final Set<UUID> disabled = ConcurrentHashMap.newKeySet();

    public static boolean isEnabled(Player player) {
        return !disabled.contains(player.getUniqueId());
    }

    /**
     * @return the new state (true if right-click crafting is now enabled for this player)
     */
    public static boolean toggle(Player player) {
        UUID id = player.getUniqueId();
        if (disabled.remove(id)) return true;

        disabled.add(id);
        return false;
    }

    @SuppressWarnings("UnstableApiUsage")
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_AIR))
            return;

        Player player = event.getPlayer();
        if (!isEnabled(player))
            return;
        if (!player.isSneaking())
            return;

        ItemStack tool = event.getItem();
        if (tool == null || !tool.getType().equals(Material.CRAFTING_TABLE))
            return;

        event.setCancelled(true);
        player.openInventory(MenuType.CRAFTING.create(player));
    }
}
