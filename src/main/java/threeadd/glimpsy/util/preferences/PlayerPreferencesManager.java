package threeadd.glimpsy.util.preferences;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import threeadd.glimpsy.util.manager.ListeningManager;

import java.util.UUID;

/** Loads a player's stored preferences on join and drops the cache entry on quit. */
public class PlayerPreferencesManager extends ListeningManager {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();

        // Async: a blocking query on the main thread for every join is a guaranteed tick stall,
        // especially with a single-connection pool. Defaults apply until it lands.
        PlayerPreferenceRepository.getInstance().loadAsync(id)
                .thenAccept(values -> PlayerPreferences.load(id, values));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PlayerPreferences.unload(event.getPlayer().getUniqueId());
    }
}
