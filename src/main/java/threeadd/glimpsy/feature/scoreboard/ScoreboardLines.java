package threeadd.glimpsy.feature.scoreboard;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import threeadd.glimpsy.feature.map.MapResetManager;
import threeadd.glimpsy.feature.stats.PlayerStats;
import threeadd.glimpsy.feature.stats.StatsManager;
import threeadd.glimpsy.feature.timezone.TimeDisplay;
import threeadd.glimpsy.feature.timezone.TimezoneManager;
import threeadd.glimpsy.feature.timezone.TimezonePreferences;
import threeadd.glimpsy.util.config.Messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the sidebar's title and body lines from live data. Every line is a message key, so the
 * whole board is re-themeable/translatable from messages.yml. The stats shown are the viewing
 * player's own (matching /stats).
 * <p>
 * The lines that are the same for every viewer (title, TPS, players, reset, IP) are built once per
 * tick into a {@link Shared} and reused, so only the genuinely per-player lines are rebuilt.
 */
final class ScoreboardLines {

    private ScoreboardLines() {
    }

    static final int LINE_COUNT = 12;

    record Shared(Component title, Component resetLine, Component tpsLine, Component playersLine, Component ipLine) {
    }

    static Component title() {
        return Messages.msg("scoreboard.title", "<b><primary>{server}</b>", "server", Messages.brandName());
    }

    /** The viewer-independent parts of the board - build once per tick, not per player. */
    static Shared shared() {
        String tps = formatTps(Bukkit.getTPS()[0]);
        String players = String.valueOf(Bukkit.getOnlinePlayers().size());

        String reset;
        if (!MapResetManager.isAutoEnabled()) reset = Messages.get("scoreboard.reset-off", "Off");
        else if (MapResetManager.isPaused()) reset = Messages.get("scoreboard.reset-paused", "Paused");
        else reset = MapResetManager.formatShort();

        String ip = Messages.get("scoreboard.ip", "2tenty.MineHut.gg");

        return new Shared(
                title(),
                Messages.msg("scoreboard.line-reset", "<muted>Map reset <white>{reset}", "reset", reset),
                Messages.msg("scoreboard.line-tps", "<muted>TPS <white>{tps}", "tps", tps),
                Messages.msg("scoreboard.line-players", "<muted>Players <white>{players}", "players", players),
                Messages.msg("scoreboard.line-ip", "<#2881C6>{ip}", "ip", ip));
    }

    static List<Component> build(Player player, Shared shared) {
        PlayerStats stats = StatsManager.getStats(player.getUniqueId());
        int kills = stats != null ? stats.getKills() : 0;
        int deaths = stats != null ? stats.getDeaths() : 0;
        int fishes = stats != null ? stats.getFishCaught() : 0;

        // The player's own zone, daylight-savings choice, seconds toggle and US/EU layout - all
        // of it lives behind /timezone, so the board just renders whatever they picked
        TimezonePreferences timeSettings = TimezoneManager.get(player);
        String date = TimeDisplay.date(timeSettings);
        String time = TimeDisplay.time(timeSettings);

        List<Component> lines = new ArrayList<>(LINE_COUNT);
        lines.add(Messages.msg("scoreboard.line-date", "<muted>Date <white>{date}", "date", date));
        lines.add(Messages.msg("scoreboard.line-time", "<muted>Time <white>{time}", "time", time));
        lines.add(Component.empty());
        lines.add(Messages.msg("scoreboard.line-kills", "<muted>Kills <white>{kills}", "kills", String.valueOf(kills)));
        lines.add(Messages.msg("scoreboard.line-deaths", "<muted>Deaths <white>{deaths}", "deaths", String.valueOf(deaths)));
        lines.add(Messages.msg("scoreboard.line-fishes", "<muted>Fishes <white>{fishes}", "fishes", String.valueOf(fishes)));
        lines.add(shared.resetLine());
        lines.add(Component.empty());
        lines.add(shared.tpsLine());
        lines.add(shared.playersLine());
        lines.add(Component.empty());
        lines.add(shared.ipLine());
        return lines;
    }

    private static String formatTps(double tpsValue) {
        String formatted = String.format(Locale.US, "%.2f", Math.min(tpsValue, 20.0));
        return formatted.endsWith(".00") ? formatted.substring(0, formatted.length() - 3) : formatted;
    }
}
