package threeadd.glimpsy.feature.spawn;

import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threeadd.glimpsy.util.manager.ListeningManager;

import java.util.Optional;

/**
 * Vanilla ignores the Y you set via /setworldspawn for brand-new players and death-respawns
 * with no bed/anchor - it re-derives the Y by scanning for the highest block at the spawn X/Z
 * instead. This forces the exact stored location in both of those cases.
 */
@SuppressWarnings("UnstableApiUsage")
public class WorldSpawnManager extends ListeningManager {

    private static final Logger log = LoggerFactory.getLogger(WorldSpawnManager.class);
    private static Location customSpawn;

    public static Optional<Location> getCustomSpawn() {
        return Optional.ofNullable(customSpawn);
    }

    public static void setCustomSpawn(Location location) {
        customSpawn = location.clone();

        // Keeps the vanilla world-spawn marker/compass roughly aligned, but actual placement
        // never relies on this - vanilla re-derives Y from the highest block at this X/Z,
        // which is exactly what the event handlers below bypass.
        location.getWorld().setSpawnLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ());

        WorldSpawnRepository.getInstance().saveSync(customSpawn);
    }

    @Override
    public void onEnable() {
        WorldSpawnRepository.getInstance().loadSync().ifPresent(location -> customSpawn = location);

        if (customSpawn != null)
            log.info("Loaded custom world spawn at {}", customSpawn);
    }

    @EventHandler
    public void onFirstJoinSpawnLocation(AsyncPlayerSpawnLocationEvent event) {
        if (customSpawn == null) return;
        if (!event.isNewPlayer()) return; // don't relocate returning players

        event.setSpawnLocation(customSpawn);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (customSpawn == null) return;
        if (event.isBedSpawn() || event.isAnchorSpawn()) return; // respect personal spawn points

        event.setRespawnLocation(customSpawn);
    }
}
