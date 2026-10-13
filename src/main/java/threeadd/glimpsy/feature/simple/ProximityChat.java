package threeadd.glimpsy.feature.simple;

import org.bukkit.entity.Player;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.preferences.PlayerPreferences;

/**
 * Per-player opt-in for proximity chat. Off by default (a low playerbase makes distance-faded chat
 * more confusing than useful), turned on with /proximitychat. While on, the player sees others'
 * chat fade with distance and their @mentions highlighted; while off they see normal chat.
 */
public final class ProximityChat {

    private static final String ENABLED_KEY = "proximity-chat";

    private ProximityChat() {
    }

    /** Master switch - while false nobody gets the fade, however they've set their own toggle. */
    public static boolean globallyEnabled() {
        return ConfigManager.getBoolean("chat.proximity.enabled", false);
    }

    public static boolean isEnabledFor(Player player) {
        return globallyEnabled() && PlayerPreferences.get(player, ENABLED_KEY, false);
    }

    public static boolean isOptedIn(Player player) {
        return PlayerPreferences.get(player, ENABLED_KEY, false);
    }

    public static void setEnabled(Player player, boolean enabled) {
        PlayerPreferences.set(player, ENABLED_KEY, enabled);
    }
}
