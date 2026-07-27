package threeadd.glimpsy.feature.simple;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import threeadd.glimpsy.util.config.ConfigManager;

/**
 * Vanilla TNT explodes with a radius of 4 - this bumps it up a lot for a much bigger blast.
 * Scoped to TNT specifically, not other explosives (creepers, beds, etc.). With that much bigger
 * radius, vanilla's own (fairly gentle) distance falloff isn't steep enough on its own, so TNT
 * damage also gets scaled down further the farther the target is from the blast - full (vanilla)
 * damage at ground zero, fading down to none right at the edge of TNT_RADIUS.
 */
public class TNTExplosionRadiusListener implements Listener {

    private static float tntRadius() {
        return (float) ConfigManager.getDouble("tnt.explosion-radius", 12.0); // 3x vanilla
    }

    // Scales what's left after falloff, for players only - block destruction is untouched.
    private static double playerDamageMultiplier() {
        return ConfigManager.getDouble("tnt.player-damage-multiplier", 0.5);
    }

    @EventHandler
    public void onPrime(ExplosionPrimeEvent event) {
        if (event.getEntity().getType() != EntityType.TNT) return;

        event.setRadius(tntRadius());
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof TNTPrimed tnt)) return;

        double distance = event.getEntity().getLocation().distance(tnt.getLocation());
        double linearFalloff = Math.max(0, 1 - (distance / tntRadius()));
        double falloff = linearFalloff * linearFalloff * linearFalloff; // cubed - much steeper drop-off past close range

        double multiplier = event.getEntity() instanceof Player ? playerDamageMultiplier() : 1.0;
        event.setDamage(event.getDamage() * falloff * multiplier);
    }
}
