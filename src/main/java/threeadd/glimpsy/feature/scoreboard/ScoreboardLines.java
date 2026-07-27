package threeadd.glimpsy.feature.scoreboard;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import threeadd.glimpsy.feature.map.MapResetManager;
import threeadd.glimpsy.feature.stats.PlayerStats;
import threeadd.glimpsy.feature.stats.StatsManager;
import threeadd.glimpsy.feature.timezone.TimezoneManager;
import threeadd.glimpsy.util.config.Messages;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the sidebar's title and body lines from live data. Every line is a message key, so the
 * whole board is re-themeable/translatable from messages.yml. The stats shown are the viewing
 * player's own (matching /stats).
 */
final class ScoreboardLines {

    private ScoreboardLines() {
    }

    static final int LINE_COUNT = 12;

    static Component title() {
        return Messages.msg("scoreboard.title", "<b><primary>{server}</b>", "server", Messages.brandName());
    }

    static List<Component> build(Player player) {
        PlayerStats stats = StatsManager.getStats(player.getUniqueId());
        int kills = stats != null ? stats.getKills() : 0;
        int deaths = stats != null ? stats.getDeaths() : 0;
        int fishes = stats != null ? stats.getFishCaught() : 0;

        ZoneId zone = TimezoneManager.get(player); // player's own timezone
        ZonedDateTime now = ZonedDateTime.now(zone);

        // American zones: month/day/year date + 12-hour clock. Everyone else (European style):
        // day/month/year date + 24-hour clock.
        boolean american = zone.getId().startsWith("America/");
        String datePattern = american
                ? Messages.get("scoreboard.date-format-us", "MM/dd/yyyy")
                : Messages.get("scoreboard.date-format-eu", "dd/MM/yyyy");
        String timePattern = american
                ? Messages.get("scoreboard.time-format-12h", "h:mm:ss a")
                : Messages.get("scoreboard.time-format-24h", "HH:mm:ss");

        String date = now.format(formatter(datePattern, "dd/MM/yyyy"));
        String time = now.format(formatter(timePattern, "HH:mm:ss"));

        String tps = formatTps(Bukkit.getTPS()[0]);
        String players = String.valueOf(Bukkit.getOnlinePlayers().size());

        String reset;
        if (!MapResetManager.isAutoEnabled()) reset = Messages.get("scoreboard.reset-off", "Off");
        else if (MapResetManager.isPaused()) reset = Messages.get("scoreboard.reset-paused", "Paused");
        else reset = MapResetManager.formatShort();

        String ip = Messages.get("scoreboard.ip", "2tenty.MineHut.gg");

        List<Component> lines = new ArrayList<>(LINE_COUNT);
        lines.add(Messages.msg("scoreboard.line-date", "<muted>Date <white>{date}", "date", date));
        lines.add(Messages.msg("scoreboard.line-time", "<muted>Time <white>{time}", "time", time));
        lines.add(Component.empty());
        lines.add(Messages.msg("scoreboard.line-kills", "<muted>Kills <white>{kills}", "kills", String.valueOf(kills)));
        lines.add(Messages.msg("scoreboard.line-deaths", "<muted>Deaths <white>{deaths}", "deaths", String.valueOf(deaths)));
        lines.add(Messages.msg("scoreboard.line-fishes", "<muted>Fishes <white>{fishes}", "fishes", String.valueOf(fishes)));
        lines.add(Messages.msg("scoreboard.line-reset", "<muted>Map reset <white>{reset}", "reset", reset));
        lines.add(Component.empty());
        lines.add(Messages.msg("scoreboard.line-tps", "<muted>TPS <white>{tps}", "tps", tps));
        lines.add(Messages.msg("scoreboard.line-players", "<muted>Players <white>{players}", "players", players));
        lines.add(Component.empty());
        lines.add(Messages.msg("scoreboard.line-ip", "<primary>{ip}", "ip", ip));
        return lines;
    }

    // Bad patterns from a config typo fall back to the default instead of breaking the board
    private static DateTimeFormatter formatter(String pattern, String fallback) {
        try {
            return DateTimeFormatter.ofPattern(pattern, Locale.US);
        } catch (IllegalArgumentException e) {
            return DateTimeFormatter.ofPattern(fallback, Locale.US);
        }
    }

    private static String formatTps(double tpsValue) {
        String formatted = String.format(Locale.US, "%.2f", Math.min(tpsValue, 20.0));
        return formatted.endsWith(".00") ? formatted.substring(0, formatted.length() - 3) : formatted;
    }
}
