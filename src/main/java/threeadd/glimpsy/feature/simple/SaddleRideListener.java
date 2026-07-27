package threeadd.glimpsy.feature.simple;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.config.ConfigManager;

public class SaddleRideListener implements Listener {

    @EventHandler
    public void onMountAttempt(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        if (!(event.getRightClicked() instanceof Player who)) return;

        ItemStack helmet = who.getInventory().getHelmet();
        if (helmet == null || helmet.getType() != Material.SADDLE) return;
        if (player.getCooldown(Material.SADDLE) > 0) return;

        player.setCooldown(Material.SADDLE, ConfigManager.getInt("saddle-ride.cooldown-ticks", 20));

        new BukkitRunnable() {
            @Override
            public void run() {
                player.setSneaking(false);
                who.addPassenger(player);
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0F, 1.0F);
            }
        }.runTaskLater(Glimpsy.getInstance(), ConfigManager.getInt("saddle-ride.mount-delay-ticks", 20));
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (player.getCooldown(Material.SADDLE) > 0) return;

        for (Entity passenger : player.getPassengers()) {
            player.removePassenger(passenger);
        }
    }

    @EventHandler
    public void onSwim(EntityToggleSwimEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!event.isSwimming() || !player.isInWater()) return;

        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null || helmet.getType() != Material.SADDLE) return;

        for (Entity passenger : player.getPassengers()) {
            player.removePassenger(passenger);
        }
    }
}
