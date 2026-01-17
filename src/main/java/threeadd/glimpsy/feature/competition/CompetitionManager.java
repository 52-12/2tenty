package threeadd.glimpsy.feature.competition;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.util.ScheduleUtil;
import threeadd.glimpsy.util.manager.Manager;
import threeadd.glimpsy.util.text.ComponentParser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class CompetitionManager extends Manager {

    private static @Nullable Competition competition;

    private static @Nullable LocalDateTime nextCompetitionTime;
    private static @Nullable BukkitTask nextCompetitionTask;

    private static void scheduleNextCompetition() {

        if (nextCompetitionTask != null)
            nextCompetitionTask.cancel();
        competition = null;

        Random random = ThreadLocalRandom.current();
        int minutesToNextCompetition = random.nextInt(40, 60);
        nextCompetitionTime = LocalDateTime.now().plusMinutes(minutesToNextCompetition);

        List<Competition> registered = CompetitionRegistry.INSTANCE.getRegisteredItems();
        nextCompetitionTask = ScheduleUtil.scheduleSync(() -> {
            if (Bukkit.getOnlinePlayers().size() < 10) {
                scheduleNextCompetition();
                Bukkit.getServer().broadcast(
                        ComponentParser.parseRichString("Skipping Competition, not enough players (at least 10 required)..."));
            }

            competition = registered.get(random.nextInt(registered.size()));
            competition.start();
        }, minutesToNextCompetition, ScheduleUtil.Unit.MINUTE);
    }

    public static void endCompetition(Competition competition) {
        if (competition != CompetitionManager.competition)
            throw new IllegalStateException("Attempted to end non-running comp");

        CompetitionManager.competition = null;

        if (!Bukkit.getServer().isStopping()) // Scheduling tasks on server stop will throw an error
            scheduleNextCompetition();
    }

    public static @Nullable LocalDateTime getNextCompetitionTime() {
        return nextCompetitionTime;
    }

    public static @Nullable Competition getCompetition() {
        return competition;
    }

    @Override
    public void enable() {
        scheduleNextCompetition();
    }

    @Override
    public void disable() {
        if (competition != null)
            competition.end();
    }
}
