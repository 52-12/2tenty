package threeadd.glimpsy.feature.competition;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.util.ScheduleUtil;
import threeadd.glimpsy.util.manager.ListeningManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class CompetitionManager extends ListeningManager {

    private static @Nullable Competition competition;

    private static @Nullable LocalDateTime nextCompetitionTime;
    private static @Nullable BukkitTask nextCompetitionTask;
    private static final Set<UUID> messagesDisabled = ConcurrentHashMap.newKeySet();

    private static void scheduleNextCompetition() {

        if (nextCompetitionTask != null)
            nextCompetitionTask.cancel();
        competition = null;

        Random random = ThreadLocalRandom.current();
        int minutesToNextCompetition = random.nextInt(40, 60);
        nextCompetitionTime = LocalDateTime.now().plusMinutes(minutesToNextCompetition);

        List<Competition> registered = CompetitionRegistry.INSTANCE.getRegisteredItems();
        nextCompetitionTask = ScheduleUtil.scheduleSync(() -> {
            competition = registered.get(random.nextInt(registered.size()));
            competition.start();
        }, minutesToNextCompetition, ScheduleUtil.Unit.MINUTE);
    }

    public static boolean isMessagesEnabled(Player player) {
        return !messagesDisabled.contains(player.getUniqueId());
    }

    public static void setMessagesEnabled(Player player, boolean enabled) {
        UUID id = player.getUniqueId();

        if (enabled)
            messagesDisabled.remove(id);
        else
            messagesDisabled.add(id);

        CompetitionMessagesRepository.getInstance().setDisabledAsync(id, !enabled);
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
    public void onEnable() {
        scheduleNextCompetition();
    }

    @Override
    public void disable() {
        if (competition != null)
            competition.end();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();

        CompetitionMessagesRepository.getInstance().isDisabledAsync(id)
                .thenAccept(disabled -> {
                    if (disabled) messagesDisabled.add(id);
                    else messagesDisabled.remove(id);
                });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        messagesDisabled.remove(event.getPlayer().getUniqueId());
    }
}
