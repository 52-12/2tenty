package threeadd.glimpsy.feature.timezone;

import org.bukkit.entity.Player;
import threeadd.glimpsy.util.manager.Manager;

import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player display timezone, persisted. Used to render the scoreboard's date/time in each
 * player's own local time. A player who hasn't set one falls back to the server's zone.
 */
public class TimezoneManager extends Manager {

    private static final Map<UUID, ZoneId> zones = new ConcurrentHashMap<>();

    @Override
    public void enable() {
        zones.putAll(TimezoneRepository.getInstance().loadAllSync());
    }

    public static ZoneId get(UUID playerId) {
        return zones.getOrDefault(playerId, ZoneId.systemDefault());
    }

    public static ZoneId get(Player player) {
        return get(player.getUniqueId());
    }

    public static boolean hasCustom(UUID playerId) {
        return zones.containsKey(playerId);
    }

    public static void set(Player player, ZoneId zone) {
        zones.put(player.getUniqueId(), zone);
        TimezoneRepository.getInstance().runAsyncSave(player.getUniqueId(), zone);
    }

    public static void clear(Player player) {
        zones.remove(player.getUniqueId());
        TimezoneRepository.getInstance().runAsyncDelete(player.getUniqueId());
    }
}
