package threeadd.glimpsy.feature.stats;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.util.manager.ListeningManager;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StatsManager extends ListeningManager {

    // Concurrent so the async chat thread can safely read stats for the [kills]/[playtime]/... chat placeholders
    private static final Map<UUID, PlayerStats> statsById = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> sessionStarts = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        statsById.putAll(StatsRepository.getInstance().loadAllSync());

        // On a plugin reload players are already online and never fire PlayerJoinEvent
        for (Player player : Bukkit.getOnlinePlayers())
            sessionStarts.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @Override
    public void disable() {
        // Shutdown - the async scheduler is no longer available, so save on this thread
        for (Player player : Bukkit.getOnlinePlayers())
            foldSessionPlaytime(player.getUniqueId());

        for (PlayerStats stats : statsById.values())
            StatsRepository.getInstance().saveSync(stats);
    }

    private static PlayerStats getOrCreate(UUID playerId) {
        return statsById.computeIfAbsent(playerId, PlayerStats::new);
    }

    public static @Nullable PlayerStats getStats(UUID playerId) {
        // Only fold this player's own session - folding every session here made every scoreboard
        // read O(players) and, called per-player-per-second, O(players^2) per second.
        foldSessionPlaytime(playerId);
        return statsById.get(playerId);
    }

    // Moves the running session's elapsed time into the stored playtime total, so stored
    // values are always current at read time without waiting for the player to log out
    private static void foldSessionPlaytime(UUID playerId) {
        Long start = sessionStarts.get(playerId);
        if (start == null) return;

        long now = System.currentTimeMillis();
        getOrCreate(playerId).addPlaytimeMillis(now - start);
        sessionStarts.put(playerId, now);
    }

    private static void foldAllSessionPlaytime() {
        for (UUID playerId : sessionStarts.keySet())
            foldSessionPlaytime(playerId);
    }

    public record TopEntry(OfflinePlayer player, long value, int position) {
    }

    public static List<TopEntry> getTop(StatCategory category, int limit) {
        foldAllSessionPlaytime();

        List<PlayerStats> sorted = statsById.values().stream()
                .filter(stats -> category.extract(stats) > 0)
                .sorted(Comparator.comparingLong(category::extract).reversed())
                .limit(limit)
                .toList();

        return sorted.stream()
                .map(stats -> new TopEntry(
                        Bukkit.getOfflinePlayer(stats.getPlayerId()),
                        category.extract(stats),
                        sorted.indexOf(stats) + 1))
                .toList();
    }

    /**
     * @return The 1-based rank of the player in this category, or -1 if they have no score
     */
    public static int getRank(StatCategory category, UUID playerId) {
        foldAllSessionPlaytime();

        PlayerStats own = statsById.get(playerId);
        if (own == null || category.extract(own) <= 0) return -1;

        long ownValue = category.extract(own);
        int rank = 1;
        for (PlayerStats stats : statsById.values()) {
            if (!stats.getPlayerId().equals(playerId) && category.extract(stats) > ownValue)
                rank++;
        }
        return rank;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        sessionStarts.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        foldSessionPlaytime(playerId);
        sessionStarts.remove(playerId);

        PlayerStats stats = statsById.get(playerId);
        if (stats != null)
            StatsRepository.getInstance().runAsyncSave(stats);
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        PlayerStats stats = getOrCreate(event.getPlayer().getUniqueId());
        stats.incrementFishCaught();
        StatsRepository.getInstance().runAsyncSave(stats);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();

        PlayerStats victimStats = getOrCreate(victim.getUniqueId());
        victimStats.incrementDeaths();
        StatsRepository.getInstance().runAsyncSave(victimStats);

        if (event.getDamageSource().getCausingEntity() instanceof Player attacker
                && !attacker.getUniqueId().equals(victim.getUniqueId())) {
            PlayerStats attackerStats = getOrCreate(attacker.getUniqueId());
            attackerStats.incrementKills();
            StatsRepository.getInstance().runAsyncSave(attackerStats);
        }
    }
}
