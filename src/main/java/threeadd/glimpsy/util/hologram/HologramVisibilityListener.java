package threeadd.glimpsy.util.hologram;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;

// Spectators shouldn't see any hologram (nametags, or anything else built on Hologram).
public class HologramVisibilityListener implements Listener {

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Hologram.syncVisibility(event.getPlayer());
    }

    // Covers joining while already in spectator (e.g. reconnecting) - no gamemode change event
    // fires in that case since it was never anything else this session.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Hologram.syncVisibility(event.getPlayer());
    }
}
