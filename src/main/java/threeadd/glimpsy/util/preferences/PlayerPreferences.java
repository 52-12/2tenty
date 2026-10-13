package threeadd.glimpsy.util.preferences;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory mirror of each online player's stored preferences, populated on join (see
 * {@link PlayerPreferencesManager}) and written through on change. Reads fall back to the caller's
 * default while a player is offline / not loaded, so callers just pass their feature's default.
 */
public final class PlayerPreferences {

    private static final Map<UUID, Map<String, Boolean>> cache = new ConcurrentHashMap<>();

    private PlayerPreferences() {
    }

    public static boolean get(Player player, String key, boolean defaultValue) {
        return get(player.getUniqueId(), key, defaultValue);
    }

    public static boolean get(UUID uuid, String key, boolean defaultValue) {
        Map<String, Boolean> values = cache.get(uuid);
        return values == null ? defaultValue : values.getOrDefault(key, defaultValue);
    }

    public static void set(Player player, String key, boolean value) {
        UUID uuid = player.getUniqueId();

        cache.computeIfAbsent(uuid, x -> new ConcurrentHashMap<>()).put(key, value);
        PlayerPreferenceRepository.getInstance().setAsync(uuid, key, value);
    }

    static void load(UUID uuid, Map<String, Boolean> values) {
        cache.put(uuid, new ConcurrentHashMap<>(values));
    }

    static void unload(UUID uuid) {
        cache.remove(uuid);
    }
}
