package threeadd.glimpsy.feature.simple;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import threeadd.glimpsy.util.preferences.PlayerPreferences;

public class InteractCraftingListener implements Listener {

    private static final String ENABLED_KEY = "right-click-crafting";

    // Per-player opt-out (default on for everyone), now persisted across restarts (#3).
    public static boolean isEnabled(Player player) {
        return PlayerPreferences.get(player, ENABLED_KEY, true);
    }

    /**
     * @return the new state (true if right-click crafting is now enabled for this player)
     */
    public static boolean toggle(Player player) {
        boolean nowEnabled = !isEnabled(player);
        PlayerPreferences.set(player, ENABLED_KEY, nowEnabled);
        return nowEnabled;
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
