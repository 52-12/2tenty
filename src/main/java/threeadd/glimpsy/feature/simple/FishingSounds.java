package threeadd.glimpsy.feature.simple;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.preferences.PlayerPreferences;

import java.util.UUID;

/**
 * Central place for the noisy "something bit your line" fishing-catch sounds. They're world-scoped
 * effects, so without this every nearby player hears every catch. A player who has turned fishing
 * sounds off stops hearing them (#16).
 */
public final class FishingSounds {

    private static final String MUTED_KEY = "fishing-sounds-muted";

    private FishingSounds() {
    }

    public static boolean isMuted(Player player) {
        return isMuted(player.getUniqueId());
    }

    public static boolean isMuted(UUID uuid) {
        return PlayerPreferences.get(uuid, MUTED_KEY, false);
    }

    public static void setMuted(Player player, boolean muted) {
        PlayerPreferences.set(player, MUTED_KEY, muted);
    }

    public static void playCatch(Location location, Sound sound, float volume, float pitch, Player source) {
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getUniqueId().equals(source.getUniqueId()) || !isMuted(player))
                player.playSound(location, sound, volume, pitch);
        }
    }
}
