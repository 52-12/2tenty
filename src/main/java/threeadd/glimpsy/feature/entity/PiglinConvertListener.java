package threeadd.glimpsy.feature.entity;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.PiglinBarterEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.config.ConfigManager;

import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

public class PiglinConvertListener implements Listener {

    @EventHandler
    public void onBarter(PiglinBarterEvent event) {
        Iterator<ItemStack> outcome = event.getOutcome().iterator();
        while (outcome.hasNext()) {
            switch (outcome.next().getType()) {
                case OBSIDIAN, CRYING_OBSIDIAN, BLACKSTONE, GRAVEL -> outcome.remove();
                default -> {}
            }
        }
    }

    @EventHandler
    public void onNaturalSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() != EntityType.PIGLIN) return;
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onCureAttempt(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof PigZombie zombifiedPiglin)) return;
        if (!zombifiedPiglin.isAdult()) return;
        if (!zombifiedPiglin.hasPotionEffect(PotionEffectType.STRENGTH)) return;

        Player player = event.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() != Material.ENCHANTED_GOLDEN_APPLE) return;

        heldItem.subtract(1);
        zombifiedPiglin.getWorld().playSound(zombifiedPiglin, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0F, 1.0F);

        int minCureTicks = ConfigManager.getInt("piglin-convert.cure-duration-min-ticks", 20);
        int maxCureTicks = ConfigManager.getInt("piglin-convert.cure-duration-max-ticks", 100);
        int[] cureTicksRemaining = {ThreadLocalRandom.current().nextInt(minCureTicks, maxCureTicks)};
        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.WHITE, 5);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (zombifiedPiglin.isDead()) {
                    this.cancel();
                    return;
                }

                if (cureTicksRemaining[0] < 1) {
                    finishCure(zombifiedPiglin);
                    this.cancel();
                    return;
                }

                cureTicksRemaining[0] -= 1;
                zombifiedPiglin.getWorld().spawnParticle(
                        Particle.DUST, zombifiedPiglin.getLocation(), 1, dustOptions);
            }
        }.runTaskTimer(Glimpsy.getInstance(), 0L, 1L);
    }

    private void finishCure(PigZombie zombifiedPiglin) {
        zombifiedPiglin.setInvisible(true);

        Piglin piglin = (Piglin) zombifiedPiglin.getWorld().spawnEntity(
                zombifiedPiglin.getLocation(), EntityType.PIGLIN, CreatureSpawnEvent.SpawnReason.CUSTOM);
        piglin.setImmuneToZombification(true);
        piglin.setAdult();
        piglin.getWorld().playSound(piglin, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0F, 1.0F);

        zombifiedPiglin.remove();
    }
}
