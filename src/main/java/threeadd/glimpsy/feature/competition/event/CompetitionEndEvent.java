package threeadd.glimpsy.feature.competition.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.feature.competition.Competition;

public class CompetitionEndEvent extends Event {

    private static final HandlerList list = new HandlerList();
    private final Competition competition;
    private final @Nullable Competition.PlayerEntry winningEntry;

    public CompetitionEndEvent(@NotNull Competition competition, @Nullable Competition.PlayerEntry winningEntry) {
        this.competition = competition;
        this.winningEntry = winningEntry;
    }

    public static HandlerList getHandlerList() {
        return list;
    }

    public Competition getCompetition() {
        return competition;
    }

    public @Nullable Competition.PlayerEntry getWinningEntry() {
        return winningEntry;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return list;
    }
}
