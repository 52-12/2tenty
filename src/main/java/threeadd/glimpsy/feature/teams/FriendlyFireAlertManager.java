package threeadd.glimpsy.feature.teams;

import org.bukkit.entity.Player;
import threeadd.glimpsy.util.preferences.PlayerPreferences;

/**
 * Per-player toggle for friendly fire alerts (see TeamManager#alertFriendlyFire). Enabled by
 * default and persisted across restarts (#3).
 */
public class FriendlyFireAlertManager {

    private static final String ENABLED_KEY = "friendly-fire-alerts";

    public static boolean isEnabled(Player player) {
        return PlayerPreferences.get(player, ENABLED_KEY, true);
    }

    /**
     * @return the new state (true if alerts are now enabled)
     */
    public static boolean toggle(Player player) {
        boolean nowEnabled = !isEnabled(player);
        PlayerPreferences.set(player, ENABLED_KEY, nowEnabled);
        return nowEnabled;
    }
}
