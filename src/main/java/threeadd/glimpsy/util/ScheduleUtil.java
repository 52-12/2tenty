package threeadd.glimpsy.util;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threeadd.glimpsy.Glimpsy;

import java.util.function.Consumer;

public class ScheduleUtil {

    private static final Logger log = LoggerFactory.getLogger(ScheduleUtil.class);

    private static Runnable wrap(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Throwable t) {
                log.error("Something went wrong in task", t);
            }
        };
    }

    private static Consumer<BukkitTask> wrap(Consumer<BukkitTask> task) {
        return bukkitTask -> {
            try {
                task.accept(bukkitTask);
            } catch (Throwable t) {
                log.error("Something went wrong in task", t);
            }
        };
    }

    public static BukkitTask scheduleSync(Runnable task) {
        return Bukkit.getScheduler().runTask(Glimpsy.getInstance(), wrap(task));
    }

    public static BukkitTask scheduleSync(Runnable task, int delay, Unit unit) {
        return Bukkit.getScheduler().runTaskLater(
                Glimpsy.getInstance(),
                wrap(task),
                unit.toTicks(delay)
        );
    }

    public static BukkitTask scheduleSync(Runnable task, int delay, int period, Unit unit) {
        return Bukkit.getScheduler().runTaskTimer(
                Glimpsy.getInstance(),
                wrap(task),
                unit.toTicks(delay),
                unit.toTicks(period)
        );
    }

    public static void scheduleSync(Consumer<BukkitTask> task) {
        Bukkit.getScheduler().runTask(Glimpsy.getInstance(), wrap(task));
    }

    public static void scheduleSync(Consumer<BukkitTask> task, int delay, Unit unit) {
        Bukkit.getScheduler().runTaskLater(
                Glimpsy.getInstance(),
                wrap(task),
                unit.toTicks(delay)
        );
    }

    public static void scheduleSync(Consumer<BukkitTask> task, int delay, int period, Unit unit) {
        Bukkit.getScheduler().runTaskTimer(
                Glimpsy.getInstance(),
                wrap(task),
                unit.toTicks(delay),
                unit.toTicks(period)
        );
    }

    public static void scheduleAsync(Consumer<BukkitTask> task) {
        Bukkit.getScheduler().runTaskAsynchronously(Glimpsy.getInstance(), wrap(task));
    }

    public static void scheduleAsync(Consumer<BukkitTask> task, int delay, Unit unit) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(
                Glimpsy.getInstance(),
                wrap(task),
                unit.toTicks(delay)
        );
    }

    public static void scheduleAsync(Consumer<BukkitTask> task, int delay, int period, Unit unit) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                Glimpsy.getInstance(),
                wrap(task),
                unit.toTicks(delay),
                unit.toTicks(period)
        );
    }

    public static BukkitTask scheduleAsync(Runnable task) {
        return Bukkit.getScheduler().runTaskAsynchronously(Glimpsy.getInstance(), wrap(task));
    }

    public static BukkitTask scheduleAsync(Runnable task, int delay, Unit unit) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(
                Glimpsy.getInstance(),
                wrap(task),
                unit.toTicks(delay)
        );
    }

    public static BukkitTask scheduleAsync(Runnable task, int delay, int period, Unit unit) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(
                Glimpsy.getInstance(),
                wrap(task),
                unit.toTicks(delay),
                unit.toTicks(period)
        );
    }

    public enum Unit {

        TICK(1),
        MILLISECOND(1.0 / 50.0),
        SECOND(20),
        MINUTE(20 * 60),
        HOUR(20 * 60 * 60),
        DAY(20 * 60 * 60 * 24);

        private final double ticks;

        Unit(double ticks) {
            this.ticks = ticks;
        }

        public static long convert(long value, Unit from, Unit to) {
            long ticks = from.toTicks(value);
            return Math.round(ticks / to.ticks);
        }

        public long toTicks(long amount) {
            return Math.round(amount * ticks);
        }
    }
}
