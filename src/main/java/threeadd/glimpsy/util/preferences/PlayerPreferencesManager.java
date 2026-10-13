package threeadd.glimpsy.util.preferences;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import threeadd.glimpsy.util.manager.ListeningManager;

/** Loads a player's stored preferences on join and drops the cache entry on quit. */
public class PlayerPreferencesManager extends ListeningManager {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerPreferences.load(event.getPlayer().getUniqueId(),
                PlayerPreferenceRepository.getInstance().loadSync(event.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PlayerPreferences.unload(event.getPlayer().getUniqueId());
    }
}
