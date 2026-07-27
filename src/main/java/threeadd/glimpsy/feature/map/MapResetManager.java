package threeadd.glimpsy.feature.map;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threeadd.glimpsy.feature.spawn.WorldSpawnManager;
import threeadd.glimpsy.util.ScheduleUtil;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.manager.ListeningManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class MapResetManager extends ListeningManager {

    private static final Logger log = LoggerFactory.getLogger(MapResetManager.class);
    private static MapResetTime time;

    // 0 = no reset has ever been recorded (fresh install) - never redirects anyone in that case.
    private static long lastResetMillis = 0;

    // 0 = no full reset has ever been recorded - never clears anyone's inventory in that case.
    private static long fullResetMillis = 0;

    // The pending auto-reset task, so it can be cancelled on pause. Null when unscheduled.
    private static BukkitTask scheduledTask;
    private static boolean paused = false;
    // Seconds left on the countdown at the moment it was paused (only meaningful while paused).
    private static long pausedRemainingSeconds = 0;

    private static boolean autoResetEnabled() {
        return ConfigManager.getBoolean("map-reset.enabled", true);
    }

    private static void schedule() {
        cancelScheduled();
        if (!autoResetEnabled() || paused) return;

        scheduledTask = ScheduleUtil.scheduleSync(() -> {
                    MapSetter.reset(Integer.MAX_VALUE);
                    // Roll the target forward a full interval and persist it, so a restart before
                    // the next reset doesn't lose the schedule.
                    MapResetManager.time = new MapResetTime();
                    MapResetRepository.getInstance().saveSync(time);
                    schedule();
                },
                getTicksUntil(time.getTime()), ScheduleUtil.Unit.TICK
        );
    }

    private static void cancelScheduled() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
        }
    }

    private static int getTicksUntil(LocalDateTime future) {
        long seconds = Math.max(0, Duration.between(LocalDateTime.now(), future).getSeconds());
        long ticks = seconds * 20;
        return (int) Math.min(ticks, Integer.MAX_VALUE);
    }

    public static long getRemainingSeconds() {
        if (paused) return pausedRemainingSeconds;
        return Math.max(0, Duration.between(LocalDateTime.now(), time.getTime()).getSeconds());
    }

    public static boolean isPaused() {
        return paused;
    }

    public static boolean isAutoEnabled() {
        return autoResetEnabled();
    }

    private static long intervalSeconds() {
        return ConfigManager.getLong("map-reset.interval-hours", 72) * 3600;
    }

    /** Coarse form for the scoreboard - days and hours only, no minutes/seconds. */
    public static String formatShort() {
        long total = getRemainingSeconds();
        long days = total / 86400;
        long hours = (total % 86400) / 3600;

        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h";
        return "<1h";
    }

    /** Full form for /whenismapreset - down to the second. */
    public static String formatLong() {
        long total = getRemainingSeconds();
        long days = total / 86400;
        long hours = (total % 86400) / 3600;
        long minutes = (total % 3600) / 60;
        long seconds = total % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0 || days > 0) sb.append(hours).append("h ");
        if (minutes > 0 || hours > 0 || days > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString();
    }

    /**
     * Adds (or, with a negative value, removes) time from the countdown.
     *
     * @return the remaining seconds after the change
     */
    public static long addSeconds(long delta) {
        if (paused) {
            pausedRemainingSeconds = Math.max(0, pausedRemainingSeconds + delta);
            MapResetRepository.getInstance().saveResetPausedSync(true, pausedRemainingSeconds);
        } else {
            LocalDateTime newTime = time.getTime().plusSeconds(delta);
            if (newTime.isBefore(LocalDateTime.now())) newTime = LocalDateTime.now();
            time = new MapResetTime(MapResetTime.key, newTime);
            MapResetRepository.getInstance().saveSync(time);
            schedule();
        }
        return getRemainingSeconds();
    }

    /** Restarts the countdown at a full interval (called after a manual world/full reset). */
    public static void resetCountdown() {
        if (paused) {
            pausedRemainingSeconds = intervalSeconds();
            MapResetRepository.getInstance().saveResetPausedSync(true, pausedRemainingSeconds);
        } else {
            time = new MapResetTime();
            MapResetRepository.getInstance().saveSync(time);
            schedule();
        }
    }

    /** @return true if it paused now, false if it was already paused. */
    public static boolean pause() {
        if (paused) return false;

        pausedRemainingSeconds = getRemainingSeconds();
        paused = true;
        cancelScheduled();
        MapResetRepository.getInstance().saveResetPausedSync(true, pausedRemainingSeconds);
        return true;
    }

    /** @return true if it resumed now, false if it wasn't paused. */
    public static boolean resume() {
        if (!paused) return false;

        paused = false;
        // Continue from where it froze: the new target is now + whatever was left.
        time = new MapResetTime(MapResetTime.key, LocalDateTime.now().plusSeconds(pausedRemainingSeconds));
        MapResetRepository.getInstance().saveSync(time);
        MapResetRepository.getInstance().saveResetPausedSync(false, 0);
        schedule();
        return true;
    }

    // Called by MapSetter once a reset actually finishes - lets onJoin tell apart players who
    // were online for it (already teleported to spawn there and then) from ones who were
    // offline and would otherwise rejoin to their old, now-void, saved position.
    public static void recordReset() {
        LocalDateTime now = LocalDateTime.now();
        lastResetMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        MapResetRepository.getInstance().saveLastResetSync(now);
    }

    // Records a full reset (world + inventories), so players who were offline for it get their
    // inventory cleared on rejoin - mirrors recordReset's spawn-redirect handling.
    public static void recordFullReset() {
        LocalDateTime now = LocalDateTime.now();
        fullResetMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        MapResetRepository.getInstance().saveFullResetSync(now);
    }

    @Override
    public void onEnable() {
        time = MapResetRepository.getInstance().loadSync().orElse(new MapResetTime());

        lastResetMillis = MapResetRepository.getInstance().loadLastResetSync()
                .map(dt -> dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .orElse(0L);

        fullResetMillis = MapResetRepository.getInstance().loadFullResetSync()
                .map(dt -> dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .orElse(0L);

        MapResetRepository.getInstance().loadResetPausedSync().ifPresent(state -> {
            paused = state.paused();
            pausedRemainingSeconds = state.remainingSeconds();
        });

        // If the countdown finished while the server was offline, start a fresh interval instead
        // of wiping the moment the server boots (which nobody would be online to see anyway).
        if (!paused && time.getTime().isBefore(LocalDateTime.now())) {
            time = new MapResetTime();
            MapResetRepository.getInstance().saveSync(time);
        }

        schedule();
        log.info("Loaded Map Reset time at {} (paused={}, auto={})", time.getTime(), paused, autoResetEnabled());
    }

    @Override
    public void disable() {
        if (time == null) throw new IllegalStateException("No map reset time found to save");

        MapResetRepository.getInstance().saveSync(time);
    }

    // A player who was offline during the last reset has a saved position from before the world
    // got wiped - their last-seen time (still reflecting the PREVIOUS session, since it isn't
    // updated again until they quit) predates the reset, so redirect them to spawn instead of
    // letting them fall into what's now just void air.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) return; // brand new players already join fresh at spawn

        long lastSeen = player.getLastSeen(); // still reflects the PREVIOUS session at join time

        // Offline through a full reset -> their saved inventory is from before the wipe, clear it
        if (fullResetMillis != 0 && lastSeen < fullResetMillis) {
            ScheduleUtil.scheduleSync(() -> player.getInventory().clear());
        }

        // Offline through a (world) reset -> their saved position is now void, send them to spawn
        if (lastResetMillis != 0 && lastSeen < lastResetMillis) {
            Location spawnLocation = WorldSpawnManager.getCustomSpawn().orElse(player.getWorld().getSpawnLocation());
            ScheduleUtil.scheduleSync(() -> player.teleportAsync(spawnLocation));
        }
    }
}
