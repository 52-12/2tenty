package threeadd.glimpsy.feature.simple;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.config.ConfigManager;

/**
 * Makes tridents unobtainable. Drowned death drops are the only vanilla source of tridents
 * (they can't be crafted, fished, or found in loot chests), so stripping them from mob death
 * drops covers every acquisition path. Player deaths are exempt so a trident someone already
 * owns (e.g. staff-given) isn't deleted when they die.
 */
public class TridentDropListener implements Listener {

    private static boolean obtainable() {
        return ConfigManager.getBoolean("trident.obtainable", false);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (obtainable()) return;
        if (event.getEntity() instanceof Player) return;

        event.getDrops().removeIf(item -> item.getType() == Material.TRIDENT);
    }
}
