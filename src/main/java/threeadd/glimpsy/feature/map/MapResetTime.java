package threeadd.glimpsy.feature.map;

import threeadd.glimpsy.util.config.ConfigManager;

import java.time.LocalDateTime;

public class MapResetTime {

    public static final String key = "mapreset";
    private final LocalDateTime time;

    public MapResetTime() {
        this(key, LocalDateTime.now().plusHours(intervalHours()));
    }

    // Only used by the no-arg constructor, which is only ever called after config is loaded
    // (in MapResetManager#onEnable and after a reset) - never during plugin bootstrap.
    private static long intervalHours() {
        return ConfigManager.getLong("map-reset.interval-hours", 72); // 3 days
    }

    protected MapResetTime(String key, LocalDateTime time) {
        if (!key.equalsIgnoreCase(MapResetTime.key))
            throw new IllegalStateException();

        this.time = time;
    }

    public LocalDateTime getTime() {
        return time;
    }
}
