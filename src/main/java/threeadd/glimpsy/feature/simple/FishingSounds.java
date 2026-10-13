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
 * {@link FishingBobberTracker} - the packet filter runs off-thread and can't query the world. A
 * reeled-in bobber lingers a moment so the reel sound that arrives with it still counts as "yours".
 */
public final class FishingSounds {

    private static final String MUTED_KEY = "fishing-sounds-muted";
    private static final long BOBBER_LINGER_MILLIS = 1500;

    private static final Map<UUID, Location> activeBobbers = new ConcurrentHashMap<>();
    private static final Map<UUID, CachedBobber> lastBobbers = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> hookedAt = new ConcurrentHashMap<>();

    // Short: long enough to cover the pull sound, short enough that the fisher recasting elsewhere
    // won't be caught by it.
    private static final long HOOK_WINDOW_MILLIS = 800;

    private record CachedBobber(Location location, long at) {
    }

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
        if (location != null) {
            Location copy = location.clone();
            activeBobbers.put(uuid, copy);
            lastBobbers.put(uuid, new CachedBobber(copy, System.currentTimeMillis()));
            return;
        }

        Location previous = activeBobbers.remove(uuid);
        if (previous != null) lastBobbers.put(uuid, new CachedBobber(previous, System.currentTimeMillis()));
    }

    /** The player's own bobber - the live one, or its last position for a moment after reel-in. */
    public static Location getOwnBobber(UUID uuid) {
        Location active = activeBobbers.get(uuid);
        if (active != null) return active;

        CachedBobber cached = lastBobbers.get(uuid);
        if (cached != null && System.currentTimeMillis() - cached.at() <= BOBBER_LINGER_MILLIS)
            return cached.location();

        return null;
    }

    /** Every currently-active bobber location, for matching generic splash variants to a bobber. */
    public static Iterable<Location> getAllBobbers() {
        return activeBobbers.values();
    }

    /** Records that a rod just hooked this player, so the pull sound plays for them briefly. */
    public static void markHooked(Player player) {
        hookedAt.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public static boolean wasRecentlyHooked(UUID uuid) {
        Long at = hookedAt.get(uuid);
        return at != null && System.currentTimeMillis() - at <= HOOK_WINDOW_MILLIS;
    }

    public static void playCatch(Location location, Sound sound, float volume, float pitch, Player source) {
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getUniqueId().equals(source.getUniqueId()) || !isMuted(player))
                player.playSound(location, sound, volume, pitch);
        }
    }
}
