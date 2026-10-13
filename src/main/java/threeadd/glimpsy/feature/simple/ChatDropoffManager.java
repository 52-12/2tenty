package threeadd.glimpsy.feature.simple;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player "chat drop-off distance" (set with /chatdropoff). While set, the player only receives
 * chat from senders within that many blocks; senders further away (or in another world) are
 * dropped for them. Session-only, 0 = no limit. See {@link ChatListener}.
 */
public class ChatDropoffManager {

    private static final Map<UUID, Double> cutoffs = new ConcurrentHashMap<>();

    /** @return the player's cutoff distance in blocks, or 0 for no limit. */
    public static double getCutoff(Player player) {
        return cutoffs.getOrDefault(player.getUniqueId(), 0.0);
    }

    public static void setCutoff(Player player, double distance) {
        if (distance <= 0) cutoffs.remove(player.getUniqueId());
        else cutoffs.put(player.getUniqueId(), distance);
    }

    public static void clear(Player player) {
        cutoffs.remove(player.getUniqueId());
    }
}
