package threeadd.glimpsy.feature.teams;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A group of 2+ mutually allied teams. Two-team alliances never need a name - their members are
 * just displayed under their own team's name everywhere. Once an alliance grows past 2 teams,
 * it needs a shared name (set via /team rename while allied) before another team can join it.
 */
public class Alliance {

    private final UUID id;
    private String rawName;
    private final Set<UUID> teamIds = ConcurrentHashMap.newKeySet();

    public Alliance(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public String getRawName() {
        return rawName;
    }

    public void setRawName(String rawName) {
        this.rawName = rawName;
    }

    public Set<UUID> getTeamIds() {
        return teamIds;
    }
}
