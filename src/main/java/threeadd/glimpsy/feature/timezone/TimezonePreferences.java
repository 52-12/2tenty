package threeadd.glimpsy.feature.timezone;

import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.util.config.ConfigManager;

import java.time.ZoneId;
import java.util.Locale;

/**
 * One player's clock settings: which zone, whether it follows daylight savings, whether seconds
 * are shown, and which date/time layout to use. Every field falls back to a server-wide default
 * from config.yml, so a player who has never touched /timezone still gets whatever the admin set.
 */
public class TimezonePreferences {

    /** How the date and time are laid out. */
    public enum Format {
        /** US zones get US layout, everyone else European - the historical behaviour. */
        AUTO,
        /** month/day/year + 12-hour clock. */
        US,
        /** day/month/year + 24-hour clock. */
        EU;

        public static Format parse(String raw, Format fallback) {
            if (raw == null) return fallback;
            try {
                return valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return fallback;
            }
        }
    }

    // null zone = follow the server default, which is what /timezone reset goes back to
    private @Nullable ZoneId zone;
    private boolean daylightSavings = defaultDaylightSavings();
    private boolean showSeconds = defaultShowSeconds();
    private Format format = defaultFormat();

    public static boolean defaultDaylightSavings() {
        return ConfigManager.getBoolean("timezone.default-daylight-savings", true);
    }

    public static boolean defaultShowSeconds() {
        return ConfigManager.getBoolean("timezone.default-show-seconds", true);
    }

    public static Format defaultFormat() {
        return Format.parse(ConfigManager.getString("timezone.default-format", "auto"), Format.AUTO);
    }

    /** The zone to actually render in - the server's own if the player never picked one. */
    public ZoneId resolveZone() {
        return zone != null ? zone : ZoneId.systemDefault();
    }

    public @Nullable ZoneId getZone() {
        return zone;
    }

    public void setZone(@Nullable ZoneId zone) {
        this.zone = zone;
    }

    public boolean isDaylightSavings() {
        return daylightSavings;
    }

    public void setDaylightSavings(boolean daylightSavings) {
        this.daylightSavings = daylightSavings;
    }

    public boolean isShowSeconds() {
        return showSeconds;
    }

    public void setShowSeconds(boolean showSeconds) {
        this.showSeconds = showSeconds;
    }

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    /** True when the player is on every default - nothing worth keeping a database row for. */
    public boolean isAllDefaults() {
        return zone == null
                && daylightSavings == defaultDaylightSavings()
                && showSeconds == defaultShowSeconds()
                && format == defaultFormat();
    }

    /** Whether US layout applies, resolving {@link Format#AUTO} against the zone. */
    public boolean usesUsFormat() {
        return switch (format) {
            case US -> true;
            case EU -> false;
            // The historical rule: American zones read month/day/year on a 12-hour clock
            case AUTO -> resolveZone().getId().startsWith("America/");
        };
    }
}
