package threeadd.glimpsy.feature.simple;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import threeadd.glimpsy.feature.achievement.Achievement;
import threeadd.glimpsy.feature.achievement.AchievementManager;

// Right-clicking a primed TNT entity with a water bucket defuses it - not a vanilla interaction,
// water only ever puts out fire in vanilla, but with TNT's blast radius bumped up a lot this
// session it's worth giving players a way to stop a mistimed fuse before it goes off.
public class TNTExtinguishListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof TNTPrimed tnt)) return;

        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        ItemStack item = hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();

        if (item.getType() != Material.WATER_BUCKET) return;

        event.setCancelled(true);

        tnt.getWorld().spawnParticle(Particle.CLOUD, tnt.getLocation(), 15, 0.3, 0.3, 0.3, 0.02);
        tnt.getWorld().playSound(tnt.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1f, 1f);
        tnt.remove();

        AchievementManager.award(player, Achievement.COUNTER_TERRORISTS_WIN);

        if (player.getGameMode() == GameMode.CREATIVE) return;

        ItemStack emptyBucket = new ItemStack(Material.BUCKET);
        if (hand == EquipmentSlot.OFF_HAND)
            player.getInventory().setItemInOffHand(emptyBucket);
        else
            player.getInventory().setItemInMainHand(emptyBucket);
    }
}
