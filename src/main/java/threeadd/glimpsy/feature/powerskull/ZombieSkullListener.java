package threeadd.glimpsy.feature.powerskull;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ZombieSkullListener implements Listener {

    @EventHandler
    public void onEquip(PlayerArmorChangeEvent event) {
        if (event.getSlotType() != PlayerArmorChangeEvent.SlotType.HEAD) return;

        Player player = event.getPlayer();
        ItemStack newItem = event.getNewItem();

        if (newItem.getType() != Material.ZOMBIE_HEAD) {
            PotionEffect saturation = player.getPotionEffect(PotionEffectType.SATURATION);
            if (saturation == null || !saturation.isInfinite()) return;

            player.removePotionEffect(PotionEffectType.SATURATION);
            return;
        }

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SATURATION, PotionEffect.INFINITE_DURATION, 1, true, true, true));
        player.playSound(player, Sound.ENTITY_ZOMBIE_AMBIENT, 1.0F, 1.0F);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null || helmet.getType() != Material.ZOMBIE_HEAD) return;

        player.playSound(player, Sound.ENTITY_ZOMBIE_HURT, 1.0F, 1.0F);
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getTarget() instanceof Player player)) return;

        // Charged Creepers and Guardians see through the Zombie Head disguise - otherwise
        // wearing one would trivialize the fished charged creeper/guardian encounters.
        if (event.getEntity() instanceof Creeper creeper && creeper.isPowered()) return;
        if (event.getEntity() instanceof Guardian) return;

        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null || helmet.getType() != Material.ZOMBIE_HEAD) return;

        event.setCancelled(true);
    }
}
