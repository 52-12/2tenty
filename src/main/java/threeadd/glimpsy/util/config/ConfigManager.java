package threeadd.glimpsy.util.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import threeadd.glimpsy.Glimpsy;

import java.util.ArrayList;
import java.util.List;

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

    public static List<String> getStringList(String path) {
        return config().getStringList(path);
    }

    /**
     * Reads a customisable "loot pool" into a Material array, supporting two config formats:
     * <ul>
     *     <li><b>A plain list</b> of material names - each entry is one "ticket", so listing a
     *         material twice makes it twice as likely.</li>
     *     <li><b>A map</b> of {@code material: weight} - for explicit per-item chances (a weight
     *         of 5 is 5x as likely as a weight of 1).</li>
     * </ul>
     * Either way the result is an array where each material appears once per ticket, so callers
     * can just pick a random index for a correctly-weighted choice. Unknown material names are
     * skipped, and {@code fallback} is returned if nothing valid was configured.
     */
    public static Material[] getMaterialList(String path, Material[] fallback) {
        List<Material> materials = new ArrayList<>();

        ConfigurationSection weighted = config().getConfigurationSection(path);
        if (weighted != null) {
            // Map form: material -> weight
            for (String key : weighted.getKeys(false)) {
                Material material = Material.matchMaterial(key);
                if (material == null) continue;

                int weight = Math.max(0, weighted.getInt(key, 1));
                for (int i = 0; i < weight; i++) materials.add(material);
            }
        } else {
            // List form: each name is one ticket
            for (String name : config().getStringList(path)) {
                Material material = Material.matchMaterial(name);
                if (material != null) materials.add(material);
            }
        }

        return materials.isEmpty() ? fallback : materials.toArray(new Material[0]);
    }
}
