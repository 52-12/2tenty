package threeadd.glimpsy.feature.teams;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

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
    private String cachedPlainName;
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
        this.cachedPlainName = null;
    }

    /** The visible text of the raw MiniMessage name, parsed once and cached until it's renamed. */
    public String getPlainName() {
        if (cachedPlainName == null)
            cachedPlainName = PlainTextComponentSerializer.plainText()
                    .serialize(MiniMessage.miniMessage().deserialize(rawName));

        return cachedPlainName;
    }

    public Set<UUID> getTeamIds() {
        return teamIds;
    }
}
