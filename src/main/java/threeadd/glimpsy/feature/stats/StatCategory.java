package threeadd.glimpsy.feature.stats;

import java.util.function.ToLongFunction;

public enum StatCategory {

    FISHES("Top Fishes", "fish caught", stats -> stats.getFishCaught()),
    KILLS("Top Kills", "kills", stats -> stats.getKills()),
    DEATHS("Top Deaths", "deaths", stats -> stats.getDeaths()),
    PLAYTIME("Top Playtime", "playtime", PlayerStats::getPlaytimeMillis);

    private final String title;
    private final String unit;
    private final ToLongFunction<PlayerStats> extractor;

    StatCategory(String title, String unit, ToLongFunction<PlayerStats> extractor) {
        this.title = title;
        this.unit = unit;
        this.extractor = extractor;
    }

    public String getTitle() {
        return title;
    }

    public String getUnit() {
        return unit;
    }

    public long extract(PlayerStats stats) {
        return extractor.applyAsLong(stats);
    }

    public String formatValue(long value) {
        if (this != PLAYTIME) return String.valueOf(value);

        long totalMinutes = value / 60_000;
        long days = totalMinutes / (60 * 24);
        long hours = (totalMinutes / 60) % 24;
        long minutes = totalMinutes % 60;

        if (days > 0) return days + "d " + hours + "h " + minutes + "m";
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }
}
