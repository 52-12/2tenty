package threeadd.glimpsy.feature.teams;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.banner.Pattern;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threeadd.glimpsy.util.ScheduleUtil;
import threeadd.glimpsy.util.manager.ListeningManager;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeamManager extends ListeningManager {

    private static final Logger log = LoggerFactory.getLogger(TeamManager.class);
    private static final long INVITE_EXPIRY_MILLIS = 5 * 60 * 1000L;

    private static final Map<UUID, TeamData> teamsById = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> playerTeamId = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingInvite> pendingInvites = new ConcurrentHashMap<>();

    private record PendingInvite(UUID teamId, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    public record Result(boolean success, String message) {
        static Result ok(String message) {
            return new Result(true, message);
        }

        static Result fail(String message) {
            return new Result(false, message);
        }
    }

    // Lookups

    public static @Nullable TeamData getTeam(Player player) {
        UUID teamId = playerTeamId.get(player.getUniqueId());
        return teamId == null ? null : teamsById.get(teamId);
    }

    public static @Nullable String getTeamName(Player player) {
        return getTeamName(player.getUniqueId());
    }

    // UUID-keyed since callers may be running off the main thread (e.g. TabManager's async
    // packet building), where touching a live Player object isn't safe.
    public static @Nullable String getTeamName(UUID uuid) {
        UUID teamId = playerTeamId.get(uuid);
        if (teamId == null) return null;

        TeamData team = teamsById.get(teamId);
        return team == null ? null : team.getRawName();
    }

    public static boolean isSameTeam(Player a, Player b) {
        UUID teamA = playerTeamId.get(a.getUniqueId());
        UUID teamB = playerTeamId.get(b.getUniqueId());

        return teamA != null && teamA.equals(teamB);
    }

    private static @Nullable TeamData findTeamByPlainName(String plainName) {
        for (TeamData team : teamsById.values()) {
            String existingPlain = PlainTextComponentSerializer.plainText()
                    .serialize(MiniMessage.miniMessage().deserialize(team.getRawName()));

            if (existingPlain.equalsIgnoreCase(plainName)) return team;
        }
        return null;
    }

    private static @Nullable TeamData findTeamByShieldSkin(List<Pattern> patterns) {
        for (TeamData team : teamsById.values())
            if (!team.getShieldPatterns().isEmpty() && team.getShieldPatterns().equals(patterns))
                return team;
        return null;
    }

    // Team lifecycle

    public static Result createTeam(Player owner, String rawName, String plainName) {
        if (playerTeamId.containsKey(owner.getUniqueId()))
            return Result.fail("You're already on a team - leave it first with /teams leave");

        if (findTeamByPlainName(plainName) != null)
            return Result.fail("A team with that name already exists");

        TeamData team = new TeamData(UUID.randomUUID(), rawName, owner.getUniqueId());
        teamsById.put(team.getId(), team);
        playerTeamId.put(owner.getUniqueId(), team.getId());

        TeamRepository.getInstance().runAsyncSave(team);
        return Result.ok("Your team is now: " + rawName);
    }

    public static Result renameTeam(Player player, String rawName, String plainName) {
        TeamData team = getTeam(player);
        if (team == null) return Result.fail("You aren't on a team");
        if (!team.getOwner().equals(player.getUniqueId())) return Result.fail("Only the team owner can do that");

        TeamData existing = findTeamByPlainName(plainName);
        if (existing != null && !existing.getId().equals(team.getId()))
            return Result.fail("A team with that name already exists");

        team.setRawName(rawName);
        TeamRepository.getInstance().runAsyncSave(team);
        return Result.ok("Your team is now: " + rawName);
    }

    /**
     * Staff override - joins the player to an existing team by name, or creates one with the
     * player as owner if no team with that name exists yet. Takes them off any current team.
     */
    public static Result forceJoinOrCreate(Player target, String rawName, String plainName) {
        // Leave (and possibly disband) any current team first, then look up the target team -
        // otherwise re-pointing an owner at their own team would disband it out from under
        // the lookup we already made.
        leaveTeamSilently(target);
        TeamData team = findTeamByPlainName(plainName);

        if (team == null) {
            team = new TeamData(UUID.randomUUID(), rawName, target.getUniqueId());
            teamsById.put(team.getId(), team);
        } else {
            team.getMembers().add(target.getUniqueId());
        }

        playerTeamId.put(target.getUniqueId(), team.getId());
        TeamRepository.getInstance().runAsyncSave(team);

        return Result.ok("done");
    }

    public static Result leaveTeam(Player player) {
        TeamData team = getTeam(player);
        if (team == null) return Result.fail("You aren't on a team");

        boolean disbanded = leaveTeamSilently(player);
        return Result.ok(disbanded ? "You disbanded your team" : "You left your team");
    }

    // Also disbands if the leaving player is the team's owner - otherwise the team would be
    // left with an owner who isn't even a member anymore (e.g. via forceJoinOrCreate/accept
    // moving an existing owner onto a different team).
    private static boolean leaveTeamSilently(Player player) {
        TeamData team = getTeam(player);
        if (team == null) return false;

        if (team.getOwner().equals(player.getUniqueId())) {
            disbandTeam(team);
            return true;
        }

        team.getMembers().remove(player.getUniqueId());
        team.getTrustedInviters().remove(player.getUniqueId());
        playerTeamId.remove(player.getUniqueId());
        TeamRepository.getInstance().runAsyncSave(team);
        return false;
    }

    private static void disbandTeam(TeamData team) {
        for (UUID member : team.getMembers())
            playerTeamId.remove(member);

        teamsById.remove(team.getId());
        TeamRepository.getInstance().runAsyncDelete(team.getId());
    }

    // Invites

    public static Result invite(Player inviter, Player target) {
        TeamData team = getTeam(inviter);
        if (team == null) return Result.fail("You aren't on a team");

        if (!team.canInvite(inviter.getUniqueId()))
            return Result.fail("You don't have permission to invite people to your team");

        if (isSameTeam(inviter, target))
            return Result.fail(target.getName() + " is already on your team");

        pendingInvites.put(target.getUniqueId(), new PendingInvite(team.getId(), System.currentTimeMillis() + INVITE_EXPIRY_MILLIS));

        Component accept = Component.text("[Accept]", NamedTextColor.GREEN, TextDecoration.BOLD)
                .clickEvent(ClickEvent.callback(audience -> {
                    if (audience instanceof Player p) sendResult(p, acceptInvite(p));
                }, ClickCallback.Options.builder().uses(1).build()));

        Component deny = Component.text("[Deny]", NamedTextColor.RED, TextDecoration.BOLD)
                .clickEvent(ClickEvent.callback(audience -> {
                    if (audience instanceof Player p) sendResult(p, denyInvite(p));
                }, ClickCallback.Options.builder().uses(1).build()));

        target.sendMessage(Component.text(inviter.getName() + " invited you to their team: ")
                .append(MiniMessage.miniMessage().deserialize(team.getRawName()))
                .appendSpace().append(accept).appendSpace().append(deny));

        return Result.ok("Invited " + target.getName() + " to your team");
    }

    public static Result acceptInvite(Player player) {
        PendingInvite invite = pendingInvites.get(player.getUniqueId());
        if (invite == null || invite.isExpired()) {
            pendingInvites.remove(player.getUniqueId());
            return Result.fail("You don't have a pending team invite");
        }

        TeamData team = teamsById.get(invite.teamId());
        pendingInvites.remove(player.getUniqueId());
        if (team == null) return Result.fail("That team no longer exists");

        leaveTeamSilently(player);

        team.getMembers().add(player.getUniqueId());
        playerTeamId.put(player.getUniqueId(), team.getId());
        TeamRepository.getInstance().runAsyncSave(team);

        return Result.ok("You joined " + PlainTextComponentSerializer.plainText()
                .serialize(MiniMessage.miniMessage().deserialize(team.getRawName())) + "'s team");
    }

    public static Result denyInvite(Player player) {
        PendingInvite invite = pendingInvites.remove(player.getUniqueId());
        if (invite == null || invite.isExpired())
            return Result.fail("You don't have a pending team invite");

        return Result.ok("You declined the team invite");
    }

    // Settings

    public static Result setInviteMode(Player player, TeamData.InviteMode mode) {
        TeamData team = getTeam(player);
        if (team == null) return Result.fail("You aren't on a team");
        if (!team.getOwner().equals(player.getUniqueId())) return Result.fail("Only the team owner can do that");

        team.setInviteMode(mode);
        TeamRepository.getInstance().runAsyncSave(team);
        return Result.ok("Invite permission set to " + mode.name());
    }

    public static Result setTrusted(Player player, Player target, boolean trusted) {
        TeamData team = getTeam(player);
        if (team == null) return Result.fail("You aren't on a team");
        if (!team.getOwner().equals(player.getUniqueId())) return Result.fail("Only the team owner can do that");
        if (!team.getMembers().contains(target.getUniqueId())) return Result.fail(target.getName() + " isn't on your team");

        if (trusted) team.getTrustedInviters().add(target.getUniqueId());
        else team.getTrustedInviters().remove(target.getUniqueId());

        TeamRepository.getInstance().runAsyncSave(team);
        return Result.ok(target.getName() + " is now " + (trusted ? "trusted to invite" : "no longer trusted to invite"));
    }

    public static Result setShieldSkin(Player player, List<Pattern> patterns) {
        TeamData team = getTeam(player);
        if (team == null) return Result.fail("You aren't on a team");
        if (!team.canModifyShieldSkin(player.getUniqueId()))
            return Result.fail("Only the team owner or trusted members can do that");

        TeamData existing = findTeamByShieldSkin(patterns);
        if (existing != null && !existing.getId().equals(team.getId()))
            return Result.fail("Another team is already using that shield design");

        team.setShieldPatterns(patterns);
        TeamRepository.getInstance().runAsyncSave(team);
        ShieldSkinManager.reapplyTeam(team);

        return Result.ok("Your team's shield design has been updated");
    }

    public static Result clearShieldSkin(Player player) {
        TeamData team = getTeam(player);
        if (team == null) return Result.fail("You aren't on a team");
        if (!team.canModifyShieldSkin(player.getUniqueId()))
            return Result.fail("Only the team owner or trusted members can do that");

        team.setShieldPatterns(List.of());
        TeamRepository.getInstance().runAsyncSave(team);
        ShieldSkinManager.reapplyTeam(team);

        return Result.ok("Your team's shield design has been cleared");
    }

    public static Result kickMember(Player actor, Player target) {
        TeamData team = getTeam(actor);
        if (team == null) return Result.fail("You aren't on a team");

        if (!isSameTeam(actor, target)) return Result.fail(target.getName() + " isn't on your team");

        if (!team.canKick(actor.getUniqueId(), target.getUniqueId())) {
            return Result.fail(target.getUniqueId().equals(team.getOwner())
                    ? "You can't remove the team founder"
                    : "You don't have permission to remove people from your team");
        }

        team.getMembers().remove(target.getUniqueId());
        team.getTrustedInviters().remove(target.getUniqueId());
        playerTeamId.remove(target.getUniqueId());
        TeamRepository.getInstance().runAsyncSave(team);

        target.sendMessage(ComponentParser.parseRichString("<red>You were removed from your team"));
        return Result.ok("Removed " + target.getName() + " from your team");
    }

    private static void sendResult(Player player, Result result) {
        player.sendMessage(ComponentParser.parseRichString(
                (result.success() ? "<green>" : "<red>") + result.message()));
    }

    @Override
    public void onEnable() {
        teamsById.putAll(TeamRepository.getInstance().loadAllSync());

        for (TeamData team : teamsById.values())
            for (UUID member : team.getMembers())
                playerTeamId.put(member, team.getId());

        log.info("Loaded {} team(s)", teamsById.size());

        ScheduleUtil.scheduleSync(() -> pendingInvites.values().removeIf(PendingInvite::isExpired), 5, 5, ScheduleUtil.Unit.MINUTE);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        if (isSameTeam(attacker, victim))
            event.setCancelled(true);
    }
}
