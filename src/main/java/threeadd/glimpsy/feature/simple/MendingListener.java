package threeadd.glimpsy.feature.simple;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemMendEvent;
import threeadd.glimpsy.util.config.ConfigManager;

/**
 * Lets the Mending enchantment be switched off via config. When disabled, Mending items simply
 * don't repair - cancelling the mend event makes the triggering XP orb count as normal
 * experience for the player instead, so no XP is lost. The enchantment can still sit on items
 * (from villager trades, loot, etc.), it just does nothing.
 */
public class MendingListener implements Listener {

    private static boolean mendingEnabled() {
        return ConfigManager.getBoolean("mending.enabled", true);
    }

    @EventHandler
    public void onMend(PlayerItemMendEvent event) {
        if (!mendingEnabled()) event.setCancelled(true);
    }
}
