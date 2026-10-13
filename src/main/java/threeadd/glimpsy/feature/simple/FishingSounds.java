package threeadd.glimpsy.feature.simple;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.preferences.PlayerPreferences;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central place for the noisy "something bit your line" fishing-catch sounds. They're world-scoped
 * effects, so without this every nearby player hears every catch. A player who has turned fishing
 * sounds off stops hearing other players' (#16).
 * <p>
 * Also holds each player's own bobber location, kept current on the main thread by
 * {@link FishingBobberTracker} - the packet filter runs off-thread and can't query the world.
 */
public final class FishingSounds {

    private static final String MUTED_KEY = "fishing-sounds-muted";

    private static final Map<UUID, Location> ownBobbers = new ConcurrentHashMap<>();

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

    static void setOwnBobber(UUID uuid, Location location) {
        if (location == null) ownBobbers.remove(uuid);
        else ownBobbers.put(uuid, location.clone());
    }

    public static Location getOwnBobber(UUID uuid) {
        return ownBobbers.get(uuid);
    }

    public static void playCatch(Location location, Sound sound, float volume, float pitch, Player source) {
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getUniqueId().equals(source.getUniqueId()) || !isMuted(player))
                player.playSound(location, sound, volume, pitch);
        }
    }
}
