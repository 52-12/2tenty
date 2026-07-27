package threeadd.glimpsy.feature.simple;

import org.bukkit.entity.Creeper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import threeadd.glimpsy.util.config.ConfigManager;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Replaces the less_creepers.zip datapack (which only ever covered plains, and the void world
 * only has plains anyway) with a plugin-side reduction that works everywhere without needing a
 * per-biome datapack edit.
 */
public class CreeperSpawnListener implements Listener {

    private static double spawnCancelChance() {
        return ConfigManager.getDouble("creeper-spawn.cancel-chance", 0.5); // fewer natural creeper spawns
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Creeper)) return;
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;

        if (ThreadLocalRandom.current().nextDouble() < spawnCancelChance())
            event.setCancelled(true);
    }
}
