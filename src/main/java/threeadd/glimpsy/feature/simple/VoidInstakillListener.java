package threeadd.glimpsy.feature.simple;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import threeadd.glimpsy.util.config.ConfigManager;

/**
 * Vanilla void damage ticks for a small amount every half-second, which combined with high
 * health/absorption can take a couple seconds to actually kill. This makes the first tick
 * always lethal instead, by overriding the damage amount to something no amount of armor,
 * absorption, or enchants could survive.
 */
public class VoidInstakillListener implements Listener {

    private static double lethalDamage() {
        return ConfigManager.getDouble("void-instakill.damage-amount", 10000.0);
    }

    @EventHandler
    public void onVoidDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) return;
        if (!(event.getEntity() instanceof Player)) return;

        event.setDamage(lethalDamage());
    }
}
