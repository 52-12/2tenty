package threeadd.glimpsy.feature.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import threeadd.glimpsy.util.ScheduleUtil;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.manager.ListeningManager;
import threeadd.glimpsy.util.preferences.PlayerPreferences;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player sidebar scoreboard, rebuilt/updated once a second. Toggleable per player (persisted,
 * shown by default) via /togglescoreboard, and switchable off entirely via the
 * scoreboard.enabled config.
 */
public class ScoreboardManager extends ListeningManager {

    private static final String HIDDEN_KEY = "scoreboard-hidden";
    private static final Map<UUID, SidebarBoard> boards = new ConcurrentHashMap<>();

    private static boolean enabled() {
        return ConfigManager.getBoolean("scoreboard.enabled", true);
    }

    private static int updateIntervalSeconds() {
        return Math.max(1, ConfigManager.getInt("scoreboard.update-interval-seconds", 1));
    }

    public static boolean isHidden(Player player) {
        return PlayerPreferences.get(player, HIDDEN_KEY, false);
    }

    /** @return the new state - true if the board is now shown, false if now hidden. */
    public static boolean toggle(Player player) {
        boolean nowHidden = !isHidden(player);
        PlayerPreferences.set(player, HIDDEN_KEY, nowHidden);

        refresh(player);
        return !nowHidden;
    }

    // Brings one player's board into line with the current state immediately (used by the toggle
    // command so it doesn't wait up to a second for the next tick).
    public static void refresh(Player player) {
        UUID id = player.getUniqueId();

        if (!enabled() || isHidden(player)) {
            SidebarBoard board = boards.remove(id);
            if (board != null) board.clear();
            return;
        }

        boards.computeIfAbsent(id, x -> new SidebarBoard(player)).update(player);
    }

    @Override
    public void onEnable() {
        ScheduleUtil.scheduleSync(this::tick, 0, updateIntervalSeconds(), ScheduleUtil.Unit.SECOND);
    }

    @Override
    public void disable() {
        for (SidebarBoard board : boards.values()) board.clear();
        boards.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Each remaining board drops the quitter from its nametag team on its next update()
        boards.remove(event.getPlayer().getUniqueId());
    }

    private void tick() {
        boolean globalEnabled = enabled();

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();

            if (!globalEnabled || isHidden(player)) {
                SidebarBoard board = boards.remove(id);
                if (board != null) board.clear();
                continue;
            }

            boards.computeIfAbsent(id, x -> new SidebarBoard(player)).update(player);
        }
    }
}
