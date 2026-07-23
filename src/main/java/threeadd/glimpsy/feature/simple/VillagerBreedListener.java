package threeadd.glimpsy.feature.simple;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreedEvent;

public class VillagerBreedListener implements Listener {

    @EventHandler
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() != EntityType.VILLAGER) return;
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.BREEDING) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onBreed(EntityBreedEvent event) {
        if (!(event.getEntity() instanceof Villager)) return;

        event.setCancelled(true);
    }
}
