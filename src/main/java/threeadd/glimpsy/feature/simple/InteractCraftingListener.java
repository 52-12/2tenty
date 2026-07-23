package threeadd.glimpsy.feature.simple;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import threeadd.glimpsy.feature.collection.CollectionManager;
import threeadd.glimpsy.util.text.ComponentParser;

public class InteractCraftingListener implements Listener {

    private static boolean enabled = true;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        InteractCraftingListener.enabled = enabled;
    }

    @SuppressWarnings("UnstableApiUsage")
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!enabled) return;

        if (!event.getAction().equals(Action.RIGHT_CLICK_AIR))
            return;

        ItemStack tool = event.getItem();
        if (tool == null || !tool.getType().equals(Material.CRAFTING_TABLE))
            return;

        Player player = event.getPlayer();

        event.setCancelled(true);
        int points = CollectionManager.getCollectionPoints(player);

        if (points < 2) {
            player.sendMessage(ComponentParser.parseRichString("You need more collection points (2 total)"));
            return;
        }

        player.openInventory(MenuType.CRAFTING.create(player));
    }
}
