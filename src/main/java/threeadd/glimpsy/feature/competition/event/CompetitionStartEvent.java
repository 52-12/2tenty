package threeadd.glimpsy.feature.competition.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import threeadd.glimpsy.feature.competition.Competition;

public class CompetitionStartEvent extends Event {

    private static final HandlerList list = new HandlerList();
    private final Competition competition;

    public CompetitionStartEvent(@NotNull Competition competition) {
        this.competition = competition;
    }

    public static HandlerList getHandlerList() {
        return list;
    }

    public Competition getCompetition() {
        return competition;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return list;
    }
}
