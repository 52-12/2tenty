package threeadd.glimpsy.feature.competition;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.feature.competition.event.CompetitionEndEvent;
import threeadd.glimpsy.feature.competition.event.CompetitionStartEvent;
import threeadd.glimpsy.util.ScheduleUtil;
import threeadd.glimpsy.util.text.ComponentParser;

import java.time.LocalDateTime;
import java.util.*;

public abstract class Competition implements Listener {

    protected final Component identifier;
    private final Map<UUID, Integer> scores = new HashMap<>();
    private final NavigableSet<UUID> sortedPlayers = new TreeSet<>(
            Comparator.<UUID>comparingInt(id -> scores.getOrDefault(id, 0))
                    .reversed()
                    .thenComparing(UUID::compareTo)
    );
    private LocalDateTime endTime;
    private BukkitTask endTask;
    protected Competition(Component identifier) {
        this.identifier = identifier;
        Bukkit.getPluginManager().registerEvents(this, Glimpsy.getInstance());
    }

    public Component getIdentifier() {
        return identifier;
    }

    protected void increment(Player player) {
        UUID id = player.getUniqueId();

        sortedPlayers.remove(id);
        int newScore = scores.getOrDefault(id, 0) + 1;
        scores.put(id, newScore);
        sortedPlayers.add(id);
    }

    public void start() {
        Bukkit.getPluginManager().callEvent(new CompetitionStartEvent(this));

        int minutesToEnd = 30;
        endTime = LocalDateTime.now().plusMinutes(minutesToEnd);
        endTask = ScheduleUtil.scheduleSync(this::end, minutesToEnd, ScheduleUtil.Unit.MINUTE);

        Bukkit.broadcast(identifier.append(ComponentParser.parseRichString(" Competition <green>Started")));
    }

    public void end() {
        endTask.cancel();
        endTask = null;
        endTime = null;

        CompetitionManager.endCompetition(this);

        @Nullable PlayerEntry winningEntry = null;
        if (!getSortedEntries().isEmpty())
            winningEntry = getSortedEntries().getFirst();

        Bukkit.getPluginManager().callEvent(new CompetitionEndEvent(this, winningEntry));

        Bukkit.broadcast(identifier.append(ComponentParser.parseRichString(" Competition <red>Ended ")
                .append(ComponentParser.parseRichString((winningEntry == null
                        ? "No One"
                        : getSortedEntries().getFirst().player().getName())
                        + " has won!"))));
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public List<PlayerEntry> getSortedEntries() {
        return sortedPlayers.stream()
                .map(this::getEntry)
                .toList();
    }

    public @Nullable PlayerEntry getEntry(UUID id) {
        int pos = getPosition(id);
        if (pos == -1) return null;
        int score = scores.get(id);

        OfflinePlayer player = Bukkit.getOfflinePlayer(id);
        return new PlayerEntry(player, score, getPosition(id));
    }

    /**
     * Get the position of the entry
     *
     * @param id The entry whose position to retrieve
     * @return The position (not same as list index), -1 if not in the list
     * @apiNote The position is not equal to the list index (index + 1)
     */
    private int getPosition(UUID id) {
        int index = 1;
        for (UUID other : sortedPlayers) {
            if (other.equals(id))
                return index;
            index++;
        }
        return -1;
    }

    /**
     * An entry in this competition, internally saved with a TreeSet and HashMap
     *
     * @param player   The offline player instance linked to this entry
     * @param score    The score this entry has
     * @param position The position (Not list index [index + 1]) of this entry
     * @apiNote The position is not equal to the list index (index + 1)
     */
    public record PlayerEntry(OfflinePlayer player, Integer score, Integer position) {
    }
}
