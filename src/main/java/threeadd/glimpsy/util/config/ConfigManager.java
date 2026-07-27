package threeadd.glimpsy.util.config;

import org.bukkit.configuration.file.FileConfiguration;
import threeadd.glimpsy.Glimpsy;

/**
 * Thin wrapper around the plugin's config.yml for gameplay-balance values (radii, multipliers,
 * chances, limits, cooldowns) - deliberately not for structural things like permission strings,
 * database schema, or command names.
 * <p>
 * Callers should always go through the get*() methods rather than caching a value in a static
 * field, so that {@link #reload()} (see {@code /2tentyreload}) takes effect immediately
 * everywhere without needing every feature to separately re-read anything.
 */
public class ConfigManager {

    public static void load() {
        Glimpsy.getInstance().saveDefaultConfig();
    }

    public static void reload() {
        Glimpsy.getInstance().reloadConfig();
    }

    private static FileConfiguration config() {
        return Glimpsy.getInstance().getConfig();
    }

    public static double getDouble(String path, double defaultValue) {
        return config().getDouble(path, defaultValue);
    }

    public static int getInt(String path, int defaultValue) {
        return config().getInt(path, defaultValue);
    }

    public static long getLong(String path, long defaultValue) {
        return config().getLong(path, defaultValue);
    }

    public static boolean getBoolean(String path, boolean defaultValue) {
        return config().getBoolean(path, defaultValue);
    }
}
