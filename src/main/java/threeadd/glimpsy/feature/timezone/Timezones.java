package threeadd.glimpsy.feature.timezone;

import org.bukkit.configuration.ConfigurationSection;
import threeadd.glimpsy.util.config.ConfigManager;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Parses player timezone input as either a UTC offset ("500", "0500", "-530", "+5", "5:30") or a
 * timezone abbreviation (EST, PST, CET, JST, ...). Abbreviations map to IANA zones so daylight
 * savings can be applied; offsets are fixed. The region/city format (America/New_York) is
 * deliberately not accepted as <i>input</i> - and it's never shown either, since every zone here
 * carries a friendly name ("Eastern Time") that's what players actually see.
 * <p>
 * The built-in table below covers the standard abbreviation list; admins can add to it or
 * override any entry through {@code timezone.abbreviations} in config.yml, which is also how
 * genuinely ambiguous abbreviations get resolved (CST is US Central here, but is also China's and
 * Cuba's - a server whose players mean one of those just remaps it).
 */
public final class Timezones {

    private Timezones() {
    }

    /** An abbreviation's target: the IANA zone it resolves to, and the name shown for it. */
    public record Zone(ZoneId zoneId, String displayName) {
    }

    // {abbreviations, IANA zone id, display name}. Standard and daylight abbreviations (EST/EDT,
    // CET/CEST, ...) both point at the same IANA zone - the zone itself decides which applies on
    // any given date, unless the player turns DST off.
    private static final String[][] BUILT_IN = {
            {"UTC,GMT,UT,Z,ZULU", "UTC", "Coordinated Universal Time"},

            // Europe / Africa
            {"WET,WEST", "Europe/Lisbon", "Western European Time"},
            {"BST,BT", "Europe/London", "British Time"},
            {"CET,CEST,MET,MEZ,MESZ", "Europe/Paris", "Central European Time"},
            {"EET,EEST", "Europe/Athens", "Eastern European Time"},
            {"TRT", "Europe/Istanbul", "Turkey Time"},
            {"MSK,MSD", "Europe/Moscow", "Moscow Time"},
            {"FET", "Europe/Minsk", "Further-Eastern European Time"},
            {"SAMT", "Europe/Samara", "Samara Time"},
            {"AZOT,AZOST", "Atlantic/Azores", "Azores Time"},
            {"CVT", "Atlantic/Cape_Verde", "Cape Verde Time"},
            {"WAT", "Africa/Lagos", "West Africa Time"},
            {"CAT", "Africa/Maputo", "Central Africa Time"},
            {"EAT", "Africa/Nairobi", "East Africa Time"},
            {"SAST", "Africa/Johannesburg", "South Africa Time"},

            // North America
            {"ET,EST,EDT", "America/New_York", "Eastern Time"},
            {"CT,CST,CDT", "America/Chicago", "Central Time"},
            {"MT,MST,MDT", "America/Denver", "Mountain Time"},
            {"PT,PST,PDT", "America/Los_Angeles", "Pacific Time"},
            {"AKT,AKST,AKDT", "America/Anchorage", "Alaska Time"},
            {"HT,HST,HAST", "Pacific/Honolulu", "Hawaii Time"},
            {"AT,AST,ADT", "America/Halifax", "Atlantic Time"},
            {"NT,NST,NDT", "America/St_Johns", "Newfoundland Time"},
            {"WGT,WGST", "America/Nuuk", "West Greenland Time"},

            // Central / South America
            {"BRT,BRST", "America/Sao_Paulo", "Brasilia Time"},
            {"AMT", "America/Manaus", "Amazon Time"},
            {"ART", "America/Argentina/Buenos_Aires", "Argentina Time"},
            {"CLT,CLST", "America/Santiago", "Chile Time"},
            {"COT", "America/Bogota", "Colombia Time"},
            {"PET", "America/Lima", "Peru Time"},
            {"VET", "America/Caracas", "Venezuela Time"},
            {"ECT", "America/Guayaquil", "Ecuador Time"},
            {"BOT", "America/La_Paz", "Bolivia Time"},
            {"PYT,PYST", "America/Asuncion", "Paraguay Time"},
            {"UYT,UYST", "America/Montevideo", "Uruguay Time"},

            // Middle East / Central & South Asia
            {"GST", "Asia/Dubai", "Gulf Time"},
            {"IRST,IRDT", "Asia/Tehran", "Iran Time"},
            {"IDT", "Asia/Jerusalem", "Israel Time"},
            {"GET", "Asia/Tbilisi", "Georgia Time"},
            {"AZT", "Asia/Baku", "Azerbaijan Time"},
            {"AFT", "Asia/Kabul", "Afghanistan Time"},
            {"PKT", "Asia/Karachi", "Pakistan Time"},
            {"UZT", "Asia/Tashkent", "Uzbekistan Time"},
            {"ALMT", "Asia/Almaty", "Kazakhstan Time"},
            {"IST", "Asia/Kolkata", "India Time"},
            {"NPT", "Asia/Kathmandu", "Nepal Time"},
            {"BDT", "Asia/Dhaka", "Bangladesh Time"},
            {"BTT", "Asia/Thimphu", "Bhutan Time"},
            {"SLST", "Asia/Colombo", "Sri Lanka Time"},
            {"MMT", "Asia/Yangon", "Myanmar Time"},

            // East & Southeast Asia
            {"ICT", "Asia/Bangkok", "Indochina Time"},
            {"WIB", "Asia/Jakarta", "Western Indonesia Time"},
            {"WITA", "Asia/Makassar", "Central Indonesia Time"},
            {"WIT", "Asia/Jayapura", "Eastern Indonesia Time"},
            {"MYT", "Asia/Kuala_Lumpur", "Malaysia Time"},
            {"BNT", "Asia/Brunei", "Brunei Time"},
            {"SGT", "Asia/Singapore", "Singapore Time"},
            {"PHT", "Asia/Manila", "Philippine Time"},
            {"HKT", "Asia/Hong_Kong", "Hong Kong Time"},
            {"JST", "Asia/Tokyo", "Japan Time"},
            {"KST", "Asia/Seoul", "Korea Time"},

            // Siberia / Russian Far East
            {"YEKT", "Asia/Yekaterinburg", "Yekaterinburg Time"},
            {"OMST", "Asia/Omsk", "Omsk Time"},
            {"NOVT", "Asia/Novosibirsk", "Novosibirsk Time"},
            {"KRAT", "Asia/Krasnoyarsk", "Krasnoyarsk Time"},
            {"IRKT", "Asia/Irkutsk", "Irkutsk Time"},
            {"YAKT", "Asia/Yakutsk", "Yakutsk Time"},
            {"VLAT", "Asia/Vladivostok", "Vladivostok Time"},
            {"MAGT", "Asia/Magadan", "Magadan Time"},
            {"PETT", "Asia/Kamchatka", "Kamchatka Time"},
            {"ANAT", "Asia/Anadyr", "Anadyr Time"},

            // Indian Ocean
            {"MUT", "Indian/Mauritius", "Mauritius Time"},
            {"MVT", "Indian/Maldives", "Maldives Time"},
            {"SCT", "Indian/Mahe", "Seychelles Time"},
            {"RET", "Indian/Reunion", "Reunion Time"},

            // Oceania
            {"AWST,AWDT", "Australia/Perth", "Australian Western Time"},
            {"ACT,ACST,ACDT", "Australia/Adelaide", "Australian Central Time"},
            {"AET,AEST,AEDT", "Australia/Sydney", "Australian Eastern Time"},
            {"LHST,LHDT", "Australia/Lord_Howe", "Lord Howe Time"},
            {"PGT", "Pacific/Port_Moresby", "Papua New Guinea Time"},
            {"NZT,NZST,NZDT", "Pacific/Auckland", "New Zealand Time"},
            {"CHAST,CHADT", "Pacific/Chatham", "Chatham Time"},
            {"FJT", "Pacific/Fiji", "Fiji Time"},
            {"WST,SST", "Pacific/Apia", "Samoa Time"},
            {"CHST", "Pacific/Guam", "Chamorro Time"},
            {"TAHT", "Pacific/Tahiti", "Tahiti Time"},
            {"MART", "Pacific/Marquesas", "Marquesas Time"},
    };

    private record Table(Map<String, Zone> byAbbreviation, Map<String, String> nameByZoneId) {
    }

    /**
     * Built fresh on every lookup rather than cached, so a {@code /2tentyreload} takes effect
     * immediately (the same rule every other config read in the plugin follows). It's a few dozen
     * map entries, and every lookup is player-triggered - nowhere near a hot path.
     */
    private static Table table() {
        Map<String, Zone> byAbbreviation = new LinkedHashMap<>();
        Map<String, String> nameByZoneId = new LinkedHashMap<>();

        for (String[] entry : BUILT_IN)
            put(byAbbreviation, nameByZoneId, entry[0], entry[1], entry[2]);

        // config.yml is applied on top, so an admin can both add abbreviations and repoint a
        // built-in one at a different zone
        ConfigurationSection configured = ConfigManager.getSection("timezone.abbreviations");
        if (configured == null) return new Table(byAbbreviation, nameByZoneId);

        for (String abbreviation : configured.getKeys(false)) {
            String value = configured.getString(abbreviation);
            if (value == null || value.isBlank()) continue;

            // "Asia/Shanghai|China Time" - the name after the pipe is optional
            String[] parts = value.split("\\|", 2);
            put(byAbbreviation, nameByZoneId, abbreviation, parts[0].trim(),
                    parts.length > 1 ? parts[1].trim() : null);
        }

        return new Table(byAbbreviation, nameByZoneId);
    }

    private static void put(Map<String, Zone> byAbbreviation, Map<String, String> nameByZoneId,
                            String abbreviations, String zoneId, String displayName) {
        ZoneId zone;
        try {
            zone = ZoneId.of(zoneId);
        } catch (Exception e) {
            return; // a typo in config.yml shouldn't take the whole table down with it
        }

        String name = displayName == null || displayName.isBlank() ? prettify(zoneId) : displayName;
        nameByZoneId.put(zone.getId(), name);

        for (String abbreviation : abbreviations.split(",")) {
            String key = abbreviation.trim().toUpperCase(Locale.ROOT);
            if (!key.isEmpty()) byAbbreviation.put(key, new Zone(zone, name));
        }
    }

    public static Optional<ZoneId> parse(String input) {
        if (input == null || input.isBlank()) return Optional.empty();

        String trimmed = input.trim();
        if (trimmed.contains("/")) return Optional.empty(); // region/city format not accepted

        Zone zone = table().byAbbreviation().get(trimmed.toUpperCase(Locale.ROOT));
        if (zone != null) return Optional.of(zone.zoneId());

        if (trimmed.matches("[+-]?\\d.*")) return parseOffset(trimmed);
        return Optional.empty();
    }

    /** What a player is shown for a zone - "Eastern Time", never "America/New_York". */
    public static String displayName(ZoneId zone) {
        if (zone instanceof ZoneOffset offset) return offsetName(offset);

        String name = table().nameByZoneId().get(zone.getId());
        return name != null ? name : prettify(zone.getId());
    }

    private static String offsetName(ZoneOffset offset) {
        return offset.getTotalSeconds() == 0 ? "UTC" : "UTC" + offset.getId();
    }

    // Last resort for a zone with no table entry (the server default, usually) - even then
    // "America/New_York" becomes "New York" rather than being shown raw
    private static String prettify(String zoneId) {
        String tail = zoneId.substring(zoneId.lastIndexOf('/') + 1);
        return tail.replace('_', ' ');
    }

    /** False for fixed UTC offsets and for zones that simply don't shift (India, Japan, ...). */
    public static boolean observesDaylightSavings(ZoneId zone) {
        return !zone.getRules().isFixedOffset();
    }

    /**
     * The current time in this zone. With daylight savings switched off the zone's <i>standard</i>
     * offset is used year-round, so a player who doesn't want their clock jumping in spring simply
     * stops observing the shift - rather than having to re-pick a fixed UTC offset and lose the
     * zone's name along with it.
     */
    public static ZonedDateTime now(ZoneId zone, boolean daylightSavings) {
        Instant instant = Instant.now();
        if (daylightSavings) return ZonedDateTime.ofInstant(instant, zone);

        return ZonedDateTime.ofInstant(instant, zone.getRules().getStandardOffset(instant));
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
