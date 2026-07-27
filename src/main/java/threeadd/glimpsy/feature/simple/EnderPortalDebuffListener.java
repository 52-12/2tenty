package threeadd.glimpsy.feature.simple;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;
import threeadd.glimpsy.util.config.ConfigManager;

public class EnderPortalDebuffListener implements Listener {

    @EventHandler
    public void onThrow(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (projectile.getType() != EntityType.ENDER_PEARL) return;

        ProjectileSource source = projectile.getShooter();
        if (source instanceof Player player) {
            player.setCooldown(Material.ENDER_PEARL, ConfigManager.getInt("ender-pearl-debuff.cooldown-ticks", 200));
        }
    }
}
