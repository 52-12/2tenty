package threeadd.glimpsy.feature.teams;

import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player toggle for friendly fire alerts (see TeamManager#alertFriendlyFire). Enabled by
 * default; session-only, not persisted.
 */
public class FriendlyFireAlertManager {

    private static final Set<UUID> disabled = ConcurrentHashMap.newKeySet();

    public static boolean isEnabled(Player player) {
        return !disabled.contains(player.getUniqueId());
    }

    /**
     * @return the new state (true if alerts are now enabled)
     */
    public static boolean toggle(Player player) {
        UUID id = player.getUniqueId();
        if (disabled.remove(id)) return true;

        disabled.add(id);
        return false;
    }
}
