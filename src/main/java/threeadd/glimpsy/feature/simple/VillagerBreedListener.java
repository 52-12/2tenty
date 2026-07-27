package threeadd.glimpsy.feature.simple;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import threeadd.glimpsy.util.config.ConfigManager;

public class VillagerBreedListener implements Listener {

    private static boolean breedingDisabled() {
        return ConfigManager.getBoolean("villager.disable-breeding", true);
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent event) {
        if (!breedingDisabled()) return;
        if (event.getEntityType() != EntityType.VILLAGER) return;
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.BREEDING) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onBreed(EntityBreedEvent event) {
        if (!breedingDisabled()) return;
        if (!(event.getEntity() instanceof Villager)) return;

        event.setCancelled(true);
    }
}
