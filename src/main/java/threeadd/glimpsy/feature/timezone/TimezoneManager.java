package threeadd.glimpsy.feature.timezone;

import org.bukkit.entity.Player;
import threeadd.glimpsy.util.manager.Manager;

import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Per-player clock settings (zone, daylight savings, seconds, date/time layout), persisted. Used
 * to render the scoreboard's date/time the way each player asked for it. A player who has never
 * run /timezone falls back to the server's zone and the config.yml defaults.
 */
public class TimezoneManager extends Manager {

    private static final Map<UUID, TimezonePreferences> preferences = new ConcurrentHashMap<>();

    @Override
    public void enable() {
        preferences.putAll(TimezoneRepository.getInstance().loadAllSync());
    }

    /**
     * This player's settings, or a throwaway all-defaults set if they've never changed any -
     * deliberately not stored, so merely reading settings can't create a row.
     */
    public static TimezonePreferences get(UUID playerId) {
        TimezonePreferences existing = preferences.get(playerId);
        return existing != null ? existing : new TimezonePreferences();
    }

    public static TimezonePreferences get(Player player) {
        return get(player.getUniqueId());
    }

    public static ZoneId getZone(Player player) {
        return get(player).resolveZone();
    }

    public static boolean hasCustom(UUID playerId) {
        return preferences.containsKey(playerId);
    }

    /**
     * Applies a change to the player's settings and persists the result. Settings that end up
     * back on every default drop their row entirely rather than being stored as a no-op - which
     * also means a later change to the config.yml defaults still reaches them.
     */
    public static TimezonePreferences update(Player player, Consumer<TimezonePreferences> change) {
        TimezonePreferences prefs = preferences.computeIfAbsent(player.getUniqueId(),
                id -> new TimezonePreferences());
        change.accept(prefs);

        if (prefs.isAllDefaults()) {
            preferences.remove(player.getUniqueId());
            TimezoneRepository.getInstance().runAsyncDelete(player.getUniqueId());
        } else {
            TimezoneRepository.getInstance().runAsyncSave(player.getUniqueId(), prefs);
        }
        return prefs;
    }

    /** Back to the server zone and the config defaults, in one go. */
    public static void clear(Player player) {
        preferences.remove(player.getUniqueId());
        TimezoneRepository.getInstance().runAsyncDelete(player.getUniqueId());
    }
}
