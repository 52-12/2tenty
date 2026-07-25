package threeadd.glimpsy.feature.teams;

import org.bukkit.block.banner.Pattern;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TeamData {

    public enum InviteMode {
        EVERYONE,   // any current member can invite
        OWNER_ONLY, // only the owner can invite (default)
        TRUSTED     // owner + whoever the owner has explicitly trusted
    }

    private final UUID id;
    private String rawName;
    private UUID owner;
    private InviteMode inviteMode = InviteMode.OWNER_ONLY;

    private final Set<UUID> members = ConcurrentHashMap.newKeySet();
    private final Set<UUID> trustedInviters = ConcurrentHashMap.newKeySet();

    // Empty = no custom shield design set, members keep plain shields. CopyOnWriteArrayList
    // since ShieldSkinManager's tick task reads this every second and it's simpler to make it
    // safe for that than to reason about which thread every future caller runs on.
    private final List<Pattern> shieldPatterns = new CopyOnWriteArrayList<>();

    public TeamData(UUID id, String rawName, UUID owner) {
        this.id = id;
        this.rawName = rawName;
        this.owner = owner;
        this.members.add(owner);
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

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public InviteMode getInviteMode() {
        return inviteMode;
    }

    public void setInviteMode(InviteMode inviteMode) {
        this.inviteMode = inviteMode;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public Set<UUID> getTrustedInviters() {
        return trustedInviters;
    }

    public List<Pattern> getShieldPatterns() {
        return shieldPatterns;
    }

    public void setShieldPatterns(List<Pattern> patterns) {
        shieldPatterns.clear();
        shieldPatterns.addAll(patterns);
    }

    // Same people who can manage invites/kicks (owner + trusted) can change the shared design.
    public boolean canModifyShieldSkin(UUID actor) {
        return actor.equals(owner) || trustedInviters.contains(actor);
    }

    public boolean canInvite(UUID player) {
        if (player.equals(owner)) return true;
        if (!members.contains(player)) return false;

        return switch (inviteMode) {
            case EVERYONE -> true;
            case OWNER_ONLY -> false;
            case TRUSTED -> trustedInviters.contains(player);
        };
    }

    // The founder/owner can never be kicked, by anyone. Otherwise the owner can remove anyone,
    // and trusted members can remove anyone except the owner.
    public boolean canKick(UUID actor, UUID target) {
        if (target.equals(owner)) return false;
        if (actor.equals(owner)) return true;

        return trustedInviters.contains(actor);
    }
}
