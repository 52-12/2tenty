package threeadd.glimpsy.feature.timezone;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Parses player timezone input as either a UTC offset ("500", "0500", "-530", "+5", "5:30") or a
 * timezone abbreviation (EST, PST, CET, JST, ...). Abbreviations map to IANA zones so daylight
 * savings is applied automatically; offsets are fixed (no DST, as expected). The region/city
 * format (America/New_York) is deliberately not accepted.
 */
public final class Timezones {

    private Timezones() {
    }

    // Abbreviation -> IANA zone id. IANA zones switch between standard/daylight automatically, so
    // both the standard and daylight abbreviations (EST/EDT, CET/CEST, ...) map to the same zone.
    private static final Map<String, String> ABBREVIATIONS = Map.ofEntries(
            Map.entry("UTC", "UTC"), Map.entry("GMT", "UTC"), Map.entry("Z", "UTC"),
            Map.entry("WET", "Europe/Lisbon"), Map.entry("WEST", "Europe/Lisbon"),
            Map.entry("BST", "Europe/London"),
            Map.entry("CET", "Europe/Paris"), Map.entry("CEST", "Europe/Paris"),
            Map.entry("EET", "Europe/Athens"), Map.entry("EEST", "Europe/Athens"),
            Map.entry("MSK", "Europe/Moscow"),
            Map.entry("ET", "America/New_York"), Map.entry("EST", "America/New_York"), Map.entry("EDT", "America/New_York"),
            Map.entry("CT", "America/Chicago"), Map.entry("CST", "America/Chicago"), Map.entry("CDT", "America/Chicago"),
            Map.entry("MT", "America/Denver"), Map.entry("MST", "America/Denver"), Map.entry("MDT", "America/Denver"),
            Map.entry("PT", "America/Los_Angeles"), Map.entry("PST", "America/Los_Angeles"), Map.entry("PDT", "America/Los_Angeles"),
            Map.entry("AKST", "America/Anchorage"), Map.entry("AKDT", "America/Anchorage"),
            Map.entry("HST", "Pacific/Honolulu"),
            Map.entry("AST", "America/Halifax"), Map.entry("ADT", "America/Halifax"),
            Map.entry("NST", "America/St_Johns"), Map.entry("NDT", "America/St_Johns"),
            Map.entry("BRT", "America/Sao_Paulo"),
            Map.entry("ART", "America/Argentina/Buenos_Aires"),
            Map.entry("IST", "Asia/Kolkata"),
            Map.entry("PKT", "Asia/Karachi"),
            Map.entry("GST", "Asia/Dubai"),
            Map.entry("JST", "Asia/Tokyo"),
            Map.entry("KST", "Asia/Seoul"),
            Map.entry("HKT", "Asia/Hong_Kong"),
            Map.entry("SGT", "Asia/Singapore"),
            Map.entry("ICT", "Asia/Bangkok"),
            Map.entry("WIB", "Asia/Jakarta"),
            Map.entry("AEST", "Australia/Sydney"), Map.entry("AEDT", "Australia/Sydney"),
            Map.entry("ACST", "Australia/Adelaide"), Map.entry("ACDT", "Australia/Adelaide"),
            Map.entry("AWST", "Australia/Perth"),
            Map.entry("NZST", "Pacific/Auckland"), Map.entry("NZDT", "Pacific/Auckland"));

    public static Optional<ZoneId> parse(String input) {
        if (input == null || input.isBlank()) return Optional.empty();

        String trimmed = input.trim();
        if (trimmed.contains("/")) return Optional.empty(); // region/city format not accepted

        String iana = ABBREVIATIONS.get(trimmed.toUpperCase(Locale.ROOT));
        if (iana != null) return Optional.of(ZoneId.of(iana));

        if (trimmed.matches("[+-]?\\d.*")) return parseOffset(trimmed);
        return Optional.empty();
    }

    // "500" -> +05:00, "0500" -> +05:00, "530" -> +05:30, "1030" -> +10:30, "-530" -> -05:30,
    // "5:30" -> +05:30. The colon is inserted automatically for the digit-only forms.
    private static Optional<ZoneId> parseOffset(String input) {
        int sign = 1;
        String s = input;
        if (s.startsWith("+")) s = s.substring(1);
        else if (s.startsWith("-")) {
            sign = -1;
            s = s.substring(1);
        }

        int hours;
        int minutes;
        if (s.contains(":")) {
            String[] parts = s.split(":", 2);
            if (!parts[0].matches("\\d{1,2}") || !parts[1].matches("\\d{2}")) return Optional.empty();
            hours = Integer.parseInt(parts[0]);
            minutes = Integer.parseInt(parts[1]);
        } else if (s.matches("\\d{1,4}")) {
            switch (s.length()) {
                case 1, 2 -> {
                    hours = Integer.parseInt(s);
                    minutes = 0;
                }
                case 3 -> {
                    hours = Integer.parseInt(s.substring(0, 1));
                    minutes = Integer.parseInt(s.substring(1));
                }
                default -> {
                    hours = Integer.parseInt(s.substring(0, 2));
                    minutes = Integer.parseInt(s.substring(2));
                }
            }
        } else {
            return Optional.empty();
        }

        if (minutes >= 60 || hours > 18) return Optional.empty();

        try {
            return Optional.of(ZoneOffset.ofHoursMinutes(sign * hours, sign * minutes));
        } catch (Exception e) {
            return Optional.empty(); // out of the ±18:00 range ZoneOffset allows
        }
    }
}
