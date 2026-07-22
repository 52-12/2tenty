package threeadd.glimpsy.feature.entity;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ElderGuardian;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

public class GuardianListener implements Listener {

    @EventHandler
    public void onElderGuardianDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof ElderGuardian)) return;
        if (!(event.getEntity().getKiller() instanceof Player player)) return;

        int lootingLevel = player.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.LOOTING);
        double chance = 30 + (11.667 * lootingLevel);

        event.getDrops().add(new ItemStack(Material.HEART_OF_THE_SEA, 1));
        if (ThreadLocalRandom.current().nextDouble() * 100 <= chance) {
            event.getDrops().add(new ItemStack(Material.HEART_OF_THE_SEA, 1));
        }

        PotionEffect luck = player.getPotionEffect(PotionEffectType.LUCK);
        if (luck == null || luck.getAmplifier() < 1) return;

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.LUCK, luck.getDuration() + 20 * 60 * 4, 2));
    }

    @EventHandler
    public void onGuardianDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof org.bukkit.entity.Guardian) || entity instanceof ElderGuardian) return;
        if (!(entity.getKiller() instanceof Player player)) return;

        PotionEffect luck = player.getPotionEffect(PotionEffectType.LUCK);
        if (luck == null || luck.getAmplifier() > 1) return;

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.LUCK, luck.getDuration() + 20 * 60 * 4, 1));
    }
}
