package threeadd.glimpsy.feature.timezone;

import threeadd.glimpsy.util.config.Messages;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Renders a moment the way one player asked for it - US or European layout, with or without
 * seconds, honouring their daylight-savings choice. Shared by the scoreboard clock and
 * /timezone's own output so the two can never disagree about what "now" looks like.
 */
public final class TimeDisplay {

    private TimeDisplay() {
    }

    public static String date(TimezonePreferences prefs) {
        String pattern = prefs.usesUsFormat()
                ? Messages.get("scoreboard.date-format-us", "MM/dd/yyyy")
                : Messages.get("scoreboard.date-format-eu", "dd.MM.yyyy");

        return now(prefs).format(formatter(pattern, "dd.MM.yyyy"));
    }

    public static String time(TimezonePreferences prefs) {
        boolean seconds = prefs.isShowSeconds();

        String pattern;
        if (prefs.usesUsFormat())
            pattern = seconds
                    ? Messages.get("scoreboard.time-format-12h", "h:mm:ss a")
                    : Messages.get("scoreboard.time-format-12h-short", "h:mm a");
        else
            pattern = seconds
                    ? Messages.get("scoreboard.time-format-24h", "HH:mm:ss")
                    : Messages.get("scoreboard.time-format-24h-short", "HH:mm");

        return now(prefs).format(formatter(pattern, seconds ? "HH:mm:ss" : "HH:mm"));
    }

    private static ZonedDateTime now(TimezonePreferences prefs) {
        return Timezones.now(prefs.resolveZone(), prefs.isDaylightSavings());
    }

    // Bad patterns from a config typo fall back to the default instead of breaking the board
    private static DateTimeFormatter formatter(String pattern, String fallback) {
        try {
            return DateTimeFormatter.ofPattern(pattern, Locale.US);
        } catch (IllegalArgumentException e) {
            return DateTimeFormatter.ofPattern(fallback, Locale.US);
        }
    }
}
