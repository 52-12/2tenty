package threeadd.glimpsy.feature.map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threeadd.glimpsy.util.ScheduleUtil;
import threeadd.glimpsy.util.manager.Manager;

import java.time.Duration;
import java.time.LocalDateTime;

public class MapResetManager extends Manager {

    private static final Logger log = LoggerFactory.getLogger(MapResetManager.class);
    private static MapResetTime time;

    private static void schedule() {
        int ticks = getTicksUntil(time.getTime());

        ScheduleUtil.scheduleSync(() -> {
                    MapSetter.reset(Integer.MAX_VALUE);
                    MapResetManager.time = new MapResetTime();
                }, // use max thread
                ticks, ScheduleUtil.Unit.TICK
        );
    }

    private static int getTicksUntil(LocalDateTime future) {
        LocalDateTime now = LocalDateTime.now();

        long seconds = Duration.between(now, future).getSeconds();
        return Math.toIntExact(seconds * 20);
    }

    @Override
    public void enable() {
        time = MapResetRepository.getInstance().loadSync().orElse(new MapResetTime());
        log.info("Loaded Map Reset time at {}", time.getTime().toString());

        schedule();
    }

    @Override
    public void disable() {
        if (time == null) throw new IllegalStateException("No map reset time found to save");

        MapResetRepository.getInstance().saveSync(time);
    }
}
