package threeadd.glimpsy.feature.teams;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.block.banner.Pattern;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityKnockbackByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threeadd.glimpsy.util.ScheduleUtil;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.manager.ListeningManager;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeamManager extends ListeningManager {

    private static final Logger log = LoggerFactory.getLogger(TeamManager.class);

    private static long inviteExpiryMillis() {
        return ConfigManager.getLong("teams.invite-expiry-minutes", 5) * 60 * 1000L;
    }

    // Friendly fire between teammates/allies is softened, not fully blocked.
    private static double allyDamageMultiplier() {
        return ConfigManager.getDouble("teams.ally-damage-multiplier", 0.25); // 75% reduction
    }

    private static double allyKnockbackMultiplier() {
        return ConfigManager.getDouble("teams.ally-knockback-multiplier", 0.5); // 50% reduction
    }

    // Actually killing (not just damaging) a teammate this many times gets you removed from the
    // team. Allies are a separate team, not a kickable membership, so this only applies to real
    // teammates.
    private static int friendlyKillLimit() {
        return ConfigManager.getInt("teams.friendly-kill-limit", 3);
    }

    private static final Map<UUID, TeamData> teamsById = new ConcurrentHashMap<>();
    private static final Map<UUID, Alliance> alliancesById = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> playerTeamId = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingInvite> pendingInvites = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> friendlyKillCounts = new ConcurrentHashMap<>();

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

    public static @Nullable TeamData getTeamById(UUID teamId) {
        return teamsById.get(teamId);
    }

    public static Collection<TeamData> getAllTeams() {
        return Collections.unmodifiableCollection(teamsById.values());
    }

    public static @Nullable String getTeamName(Player player) {
        return getTeamName(player.getUniqueId());
    }

    // UUID-keyed since callers may be running off the main thread (e.g. TabManager's async
    // packet building), where touching a live Player object isn't safe.
    // Shows the alliance's shared name instead of the team's own name once one is set - an
    // unnamed (still just 2-team) alliance still shows each team's own name.
    public static @Nullable String getTeamName(UUID uuid) {
        UUID teamId = playerTeamId.get(uuid);
        if (teamId == null) return null;

        TeamData team = teamsById.get(teamId);
        if (team == null) return null;

        Alliance alliance = getAlliance(team);
        if (alliance != null && alliance.getRawName() != null) return alliance.getRawName();

        return team.getRawName();
    }

    public static boolean isSameTeam(Player a, Player b) {
        return isSameTeam(a.getUniqueId(), b.getUniqueId());
    }

    // UUID-keyed so callers can check against offline players (e.g. a trade chest's owner)
    public static boolean isSameTeam(UUID a, UUID b) {
        UUID teamA = playerTeamId.get(a);
        UUID teamB = playerTeamId.get(b);

        return teamA != null && teamA.equals(teamB);
    }

    // UUID-keyed like getTeamName(UUID) - the other party (e.g. a bed's owner) may be offline.
    public static boolean isAllied(UUID a, UUID b) {
        UUID teamIdA = playerTeamId.get(a);
        UUID teamIdB = playerTeamId.get(b);
        if (teamIdA == null || teamIdB == null || teamIdA.equals(teamIdB)) return false;

        TeamData teamA = teamsById.get(teamIdA);
        TeamData teamB = teamsById.get(teamIdB);
        if (teamA == null || teamB == null) return false;

        UUID allianceIdA = teamA.getAllianceId();
        return allianceIdA != null && allianceIdA.equals(teamB.getAllianceId());
    }

    public static boolean isSameTeamOrAllied(UUID a, UUID b) {
        UUID teamIdA = playerTeamId.get(a);
        UUID teamIdB = playerTeamId.get(b);

        if (teamIdA != null && teamIdA.equals(teamIdB)) return true;
        return isAllied(a, b);
    }

    public static boolean isSameTeamOrAllied(Player a, Player b) {
        return isSameTeamOrAllied(a.getUniqueId(), b.getUniqueId());
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
            return Result.fail(Messages.get("teams.already-on-team", "You're already on a team - leave it first with /teams leave"));

        if (findTeamByPlainName(plainName) != null)
            return Result.fail(Messages.get("teams.name-taken", "A team with that name already exists"));

        TeamData team = new TeamData(UUID.randomUUID(), rawName, owner.getUniqueId());
        teamsById.put(team.getId(), team);
        playerTeamId.put(owner.getUniqueId(), team.getId());

        TeamRepository.getInstance().runAsyncSave(team);
        return Result.ok(Messages.get("teams.team-set", "<white>Your team is now: {name}", "name", rawName));
    }

    // While your team is in an alliance, renaming isn't meaningful for the team itself (its own
    // name is hidden behind the alliance's shared name everywhere anyway) - redirect to naming
    // the alliance instead.
    public static Result renameTeam(Player player, String rawName, String plainName) {
        TeamData team = getTeam(player);
        if (team == null) return Result.fail(Messages.get("teams.not-on-team", "You aren't on a team"));

        if (getAlliance(team) != null) return setAllianceName(player, rawName, plainName);

        if (!team.getOwner().equals(player.getUniqueId())) return Result.fail(Messages.get("teams.owner-only", "Only the team owner can do that"));

        TeamData existing = findTeamByPlainName(plainName);
        if (existing != null && !existing.getId().equals(team.getId()))
            return Result.fail(Messages.get("teams.name-taken", "A team with that name already exists"));

        team.setRawName(rawName);
        TeamRepository.getInstance().runAsyncSave(team);
        return Result.ok(Messages.get("teams.team-set", "<white>Your team is now: {name}", "name", rawName));
    }

    /**
     * Staff override (/team replace) - renames the team (or its named alliance, same
     * redirect-if-allied behavior as {@link #renameTeam}) targeted either by team name or by an
     * online member's name. Skips the owner check; the caller skips the length limit and word
     * filter, same as the old /teamset overrides did.
     */
    public static Result adminRename(TeamData team, String rawName, String plainName) {
        Alliance alliance = getAlliance(team);
        if (alliance != null) {
            Alliance existingAlliance = findAllianceByPlainName(plainName);
            if (existingAlliance != null && !existingAlliance.getId().equals(alliance.getId()))
                return Result.fail(Messages.get("teams.alliance-name-taken", "Another alliance already has that name"));

            alliance.setRawName(rawName);
            TeamRepository.getInstance().runAsyncSaveAlliance(alliance);
            return Result.ok(Messages.get("teams.admin-alliance-renamed", "Alliance renamed to: {name}", "name", rawName));
        }

        TeamData existingTeam = findTeamByPlainName(plainName);
        if (existingTeam != null && !existingTeam.getId().equals(team.getId()))
            return Result.fail(Messages.get("teams.name-taken", "A team with that name already exists"));

        team.setRawName(rawName);
        TeamRepository.getInstance().runAsyncSave(team);
        return Result.ok(Messages.get("teams.admin-team-renamed", "Team renamed to: {name}", "name", rawName));
    }

    public static @Nullable TeamData findTeam(String plainName) {
        return findTeamByPlainName(plainName);
    }

    /**
     * Staff override (/team replace on a player who isn't on a team) - joins them to an
     * existing team with this name, or creates one with them as owner if none exists.
     * Bypasses invites, like the old /teamset did.
     */
    public static Result forceJoin(Player target, String rawName, String plainName) {
        TeamData team = findTeamByPlainName(plainName);

        if (team == null) {
            team = new TeamData(UUID.randomUUID(), rawName, target.getUniqueId());
            teamsById.put(team.getId(), team);
        } else {
            team.getMembers().add(target.getUniqueId());
        }

        playerTeamId.put(target.getUniqueId(), team.getId());
        TeamRepository.getInstance().runAsyncSave(team);

        return Result.ok(Messages.get("teams.force-joined", "{player} is now on team: {name}",
                "player", target.getName(), "name", team.getRawName()));
    }

    // Admin-set per-name overrides on top of SimpleWordFilter (/team blacklist and whitelist),
    // stored lowercased.
    private static final Set<String> blacklistedNames = ConcurrentHashMap.newKeySet();
    private static final Set<String> whitelistedNames = ConcurrentHashMap.newKeySet();

    // Case-insensitive and whitespace-tolerant: rules are stored and matched through this
    // one normalization, so "/team blacklist FOO" blocks "foo", " Foo ", etc.
    private static String normalizeNameRule(String plainName) {
        return plainName.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * A blacklisted name is always blocked, a whitelisted one always allowed (bypassing
     * {@link SimpleWordFilter}); anything else falls through to the filter.
     */
    public static boolean isNameAllowed(String plainName) {
        String normalized = normalizeNameRule(plainName);
        if (blacklistedNames.contains(normalized)) return false;
        if (whitelistedNames.contains(normalized)) return true;
        return SimpleWordFilter.isAppropriate(plainName);
    }

    public static Result blacklistName(String plainName) {
        String normalized = normalizeNameRule(plainName);
        whitelistedNames.remove(normalized);
        if (!blacklistedNames.add(normalized)) return Result.fail(Messages.get("teams.already-blacklisted", "That name is already blacklisted"));

        TeamRepository.getInstance().runAsyncSaveNameRule(normalized, "BLACKLIST");
        return Result.ok(Messages.get("teams.blacklisted", "Blacklisted team name: {name}", "name", plainName));
    }

    public static Result whitelistName(String plainName) {
        String normalized = normalizeNameRule(plainName);
        blacklistedNames.remove(normalized);
        if (!whitelistedNames.add(normalized)) return Result.fail(Messages.get("teams.already-whitelisted", "That name is already whitelisted"));

        TeamRepository.getInstance().runAsyncSaveNameRule(normalized, "WHITELIST");
        return Result.ok(Messages.get("teams.whitelisted", "Whitelisted team name: {name}", "name", plainName));
    }

    public static Result leaveTeam(Player player) {
        TeamData team = getTeam(player);
        if (team == null) return Result.fail(Messages.get("teams.not-on-team", "You aren't on a team"));

        boolean disbanded = leaveTeamSilently(player);
        return Result.ok(disbanded
                ? Messages.get("teams.disbanded", "You disbanded your team")
                : Messages.get("teams.left", "You left your team"));
    }

    // Also disbands if the leaving player is the team's owner - otherwise the team would be
    // left with an owner who isn't even a member anymore (e.g. accept moving an existing
    // owner onto a different team).
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
        friendlyKillCounts.remove(player.getUniqueId());
        TeamRepository.getInstance().runAsyncSave(team);
        return false;
    }

    private static void disbandTeam(TeamData team) {
        for (UUID member : team.getMembers())
            playerTeamId.remove(member);

        removeFromAlliance(team);

        teamsById.remove(team.getId());
        TeamRepository.getInstance().runAsyncDelete(team.getId());
    }

    // Pulls a team out of its current alliance (if any), dissolving the alliance entirely if
    // that leaves it with one or zero teams left in it.
    private static void removeFromAlliance(TeamData team) {
        UUID allianceId = team.getAllianceId();
        if (allianceId == null) return;

        Alliance alliance = alliancesById.get(allianceId);
        team.setAllianceId(null);

        if (alliance == null) return;
        alliance.getTeamIds().remove(team.getId());

        if (alliance.getTeamIds().size() <= 1) {
            for (UUID remainingTeamId : alliance.getTeamIds()) {
                TeamData remaining = teamsById.get(remainingTeamId);
                if (remaining == null) continue;

                remaining.setAllianceId(null);
                TeamRepository.getInstance().runAsyncSave(remaining);
            }

            alliancesById.remove(allianceId);
            TeamRepository.getInstance().runAsyncDeleteAlliance(allianceId);
        }
    }

    // Invites

    public static Result invite(Player inviter, Player target) {
        TeamData team = getTeam(inviter);
        if (team == null) return Result.fail(Messages.get("teams.not-on-team", "You aren't on a team"));

        if (!team.canInvite(inviter.getUniqueId()))
            return Result.fail(Messages.get("teams.no-invite-permission", "You don't have permission to invite people to your team"));

        if (isSameTeam(inviter, target))
            return Result.fail(Messages.get("teams.already-on-your-team", "{player} is already on your team", "player", target.getName()));

        pendingInvites.put(target.getUniqueId(), new PendingInvite(team.getId(), System.currentTimeMillis() + inviteExpiryMillis()));

        Component accept = Messages.msg("teams.button-accept", "<green><b>[Accept]</b>")
                .clickEvent(ClickEvent.callback(audience -> {
                    if (audience instanceof Player p) sendResult(p, acceptInvite(p));
                }, ClickCallback.Options.builder().uses(1).build()));

        Component deny = Messages.msg("teams.button-deny", "<red><b>[Deny]</b>")
                .clickEvent(ClickEvent.callback(audience -> {
                    if (audience instanceof Player p) sendResult(p, denyInvite(p));
                }, ClickCallback.Options.builder().uses(1).build()));

        target.sendMessage(Messages.msg("teams.invited", "{player} invited you to their team: ", "player", inviter.getName())
                .append(MiniMessage.miniMessage().deserialize(team.getRawName()))
                .appendSpace().append(accept).appendSpace().append(deny));

        return Result.ok(Messages.get("teams.invite-sent", "Invited {player} to your team", "player", target.getName()));
    }

    public static Result acceptInvite(Player player) {
        PendingInvite invite = pendingInvites.get(player.getUniqueId());
        if (invite == null || invite.isExpired()) {
            pendingInvites.remove(player.getUniqueId());
            return Result.fail(Messages.get("teams.no-pending-invite", "You don't have a pending team invite"));
        }

        TeamData team = teamsById.get(invite.teamId());
        pendingInvites.remove(player.getUniqueId());
        if (team == null) return Result.fail(Messages.get("teams.team-gone", "That team no longer exists"));

        leaveTeamSilently(player);

        team.getMembers().add(player.getUniqueId());
        playerTeamId.put(player.getUniqueId(), team.getId());
        TeamRepository.getInstance().runAsyncSave(team);

        return Result.ok(Messages.get("teams.joined", "You joined {team}'s team", "team",
                PlainTextComponentSerializer.plainText().serialize(MiniMessage.miniMessage().deserialize(team.getRawName()))));
    }

    public static Result denyInvite(Player player) {
        PendingInvite invite = pendingInvites.remove(player.getUniqueId());
        if (invite == null || invite.isExpired())
            return Result.fail(Messages.get("teams.no-pending-invite", "You don't have a pending team invite"));

        return Result.ok(Messages.get("teams.invite-declined", "You declined the team invite"));
    }

    // Settings

    public static Result setInviteMode(Player player, TeamData.InviteMode mode) {
        TeamData team = getTeam(player);
        if (team == null) return Result.fail(Messages.get("teams.not-on-team", "You aren't on a team"));
        if (!team.getOwner().equals(player.getUniqueId())) return Result.fail(Messages.get("teams.owner-only", "Only the team owner can do that"));

        team.setInviteMode(mode);
        TeamRepository.getInstance().runAsyncSave(team);
        return Result.ok(Messages.get("teams.invite-mode-set", "Invite permission set to {mode}", "mode", mode.name()));
    }

    public static Result setTrusted(Player player, Player target, boolean trusted) {
        TeamData team = getTeam(player);
        if (team == null) return Result.fail(Messages.get("teams.not-on-team", "You aren't on a team"));
        if (!team.getOwner().equals(player.getUniqueId())) return Result.fail(Messages.get("teams.owner-only", "Only the team owner can do that"));
        if (!team.getMembers().contains(target.getUniqueId())) return Result.fail(Messages.get("teams.not-on-your-team", "{player} isn't on your team", "player", target.getName()));

        if (trusted) team.getTrustedInviters().add(target.getUniqueId());
        else team.getTrustedInviters().remove(target.getUniqueId());

        TeamRepository.getInstance().runAsyncSave(team);
        return Result.ok(trusted
                ? Messages.get("teams.trusted", "{player} is now trusted to invite", "player", target.getName())
                : Messages.get("teams.untrusted", "{player} is now no longer trusted to invite", "player", target.getName()));
    }

    public static Result setShieldSkin(Player player, List<Pattern> patterns) {
        TeamData team = getTeam(player);
        if (team == null) return Result.fail(Messages.get("teams.not-on-team", "You aren't on a team"));
        if (!team.canModifyShieldSkin(player.getUniqueId()))
            return Result.fail(Messages.get("teams.owner-or-trusted-only", "Only the team owner or trusted members can do that"));

        TeamData existing = findTeamByShieldSkin(patterns);
        if (existing != null && !existing.getId().equals(team.getId()))
            return Result.fail(Messages.get("teams.shield-design-taken", "Another team is already using that shield design"));

        team.setShieldPatterns(patterns);
        TeamRepository.getInstance().runAsyncSave(team);
        ShieldSkinManager.reapplyTeam(team);

        return Result.ok(Messages.get("teams.shield-updated", "Your team's shield design has been updated"));
    }

    public static Result clearShieldSkin(Player player) {
        TeamData team = getTeam(player);
        if (team == null) return Result.fail(Messages.get("teams.not-on-team", "You aren't on a team"));
        if (!team.canModifyShieldSkin(player.getUniqueId()))
            return Result.fail(Messages.get("teams.owner-or-trusted-only", "Only the team owner or trusted members can do that"));

        team.setShieldPatterns(List.of());
        TeamRepository.getInstance().runAsyncSave(team);
        ShieldSkinManager.reapplyTeam(team);

        return Result.ok(Messages.get("teams.shield-cleared", "Your team's shield design has been cleared"));
    }

    // Alliances

    // Self-heals a degenerate alliance (dangling reference, or one that dropped to 0-1 teams
    // without going through removeFromAlliance's cleanup) instead of ever handing back
    // something that would render as an empty "allied with " with no other team.
    public static @Nullable Alliance getAlliance(TeamData team) {
        if (team.getAllianceId() == null) return null;

        Alliance alliance = alliancesById.get(team.getAllianceId());
        if (alliance == null) {
            team.setAllianceId(null);
            TeamRepository.getInstance().runAsyncSave(team);
            return null;
        }

        if (alliance.getTeamIds().size() <= 1) {
            team.setAllianceId(null);
            TeamRepository.getInstance().runAsyncSave(team);

            alliancesById.remove(alliance.getId());
            TeamRepository.getInstance().runAsyncDeleteAlliance(alliance.getId());
            return null;
        }

        return alliance;
    }

    private static @Nullable Alliance findAllianceByPlainName(String plainName) {
        for (Alliance alliance : alliancesById.values()) {
            if (alliance.getRawName() == null) continue;

            String existingPlain = PlainTextComponentSerializer.plainText()
                    .serialize(MiniMessage.miniMessage().deserialize(alliance.getRawName()));

            if (existingPlain.equalsIgnoreCase(plainName)) return alliance;
        }
        return null;
    }

    /**
     * Proposes an alliance with the named team. A team can only be in one alliance at a time.
     * If the target team has already proposed an alliance back to this one, this call
     * completes it immediately - otherwise it just registers a one-sided offer that the other
     * team needs to reciprocate with their own /team alliance command (or the [Yes] button, see
     * {@link #acceptAllianceOffer}).
     * <p>
     * Joining a third (or later) team into an existing alliance requires that alliance to
     * already have a shared name (see {@link #setAllianceName}).
     */
    public static Result proposeAlliance(Player actor, String targetPlainName) {
        TeamData team = getTeam(actor);
        if (team == null) return Result.fail(Messages.get("teams.not-on-team", "You aren't on a team"));
        if (!team.canModifyAlliance(actor.getUniqueId()))
            return Result.fail(Messages.get("teams.owner-or-trusted-only", "Only the team owner or trusted members can do that"));

        TeamData target = findTeamByPlainName(targetPlainName);
        if (target == null) return Result.fail(Messages.get("teams.no-team-found", "No team found with that name"));
        if (target.getId().equals(team.getId())) return Result.fail(Messages.get("teams.ally-self", "You can't ally with your own team"));

        Result mergeCheck = checkCanMerge(team, target);
        if (mergeCheck != null) return mergeCheck;

        if (team.getId().equals(target.getAllianceOfferTo()))
            return completeAllianceMerge(team, target);

        // Not reciprocated (yet) - register our offer, replacing any previous one. A team can
        // only track one outbound offer at a time, so silently overwriting it would leave
        // whoever we'd previously offered to with a dead [Yes]/[No] prompt that still looks
        // valid - tell them explicitly instead.
        UUID previousOfferTo = team.getAllianceOfferTo();
        if (previousOfferTo != null && !previousOfferTo.equals(target.getId()))
            notifyOfferWithdrawn(team, previousOfferTo);

        team.setAllianceOfferTo(target.getId());
        TeamRepository.getInstance().runAsyncSave(team);

        notifyAllianceOffer(team, target);
        return Result.ok(Messages.get("teams.offer-sent",
                "Alliance offer sent to {team} - they need to run /team alliance too to confirm it",
                "team", target.getRawName()));
    }

    /**
     * Accepts a specific pending offer by the proposing team's id, used by the [Yes] button in
     * {@link #notifyAllianceOffer}. Unlike {@link #proposeAlliance}, this never falls back to
     * silently sending a fresh offer of its own if the original one turned out to be stale (e.g.
     * the proposing team moved on to someone else in the meantime) - it just reports that the
     * offer isn't valid anymore.
     */
    public static Result acceptAllianceOffer(Player actor, UUID proposingTeamId) {
        TeamData team = getTeam(actor);
        if (team == null) return Result.fail(Messages.get("teams.not-on-team", "You aren't on a team"));
        if (!team.canModifyAlliance(actor.getUniqueId()))
            return Result.fail(Messages.get("teams.owner-or-trusted-only", "Only the team owner or trusted members can do that"));

        TeamData proposingTeam = getTeamById(proposingTeamId);
        if (proposingTeam == null || !team.getId().equals(proposingTeam.getAllianceOfferTo()))
            return Result.fail(Messages.get("teams.offer-invalid", "That alliance offer is no longer valid"));

        Result mergeCheck = checkCanMerge(proposingTeam, team);
        if (mergeCheck != null) return mergeCheck;

        return completeAllianceMerge(proposingTeam, team);
    }

    // Null if team/target can merge right now; otherwise the failure Result to return.
    private static @Nullable Result checkCanMerge(TeamData team, TeamData target) {
        Alliance teamAlliance = getAlliance(team);
        Alliance targetAlliance = getAlliance(target);

        if (teamAlliance != null && teamAlliance.equals(targetAlliance))
            return Result.fail(Messages.get("teams.already-allied", "Your team is already allied with {team}", "team", target.getRawName()));
        if (teamAlliance != null && targetAlliance != null)
            return Result.fail(Messages.get("teams.both-allied", "Both teams are already in different alliances - have one team leave theirs first"));

        Alliance alliance = teamAlliance != null ? teamAlliance : targetAlliance;
        int resultingSize = (alliance == null ? 1 : alliance.getTeamIds().size()) + 1;
        if (resultingSize > 2 && (alliance == null || alliance.getRawName() == null)) {
            return Result.fail(Messages.get("teams.alliance-needs-name",
                    "This alliance needs a name before another team can join - have a member run /team rename, then try again"));
        }

        return null;
    }

    // At most one side already has an alliance (checked by checkCanMerge), so this always
    // resolves to either forming a brand new 2-team alliance, or one lone team joining the
    // other side's existing alliance.
    private static Result completeAllianceMerge(TeamData team, TeamData target) {
        Alliance teamAlliance = getAlliance(team);
        Alliance targetAlliance = getAlliance(target);
        Alliance alliance = teamAlliance != null ? teamAlliance : targetAlliance;

        // If one side already had an alliance, the other side (whichever had none) is the team
        // that's newly joining it.
        TeamData joiningTeam = teamAlliance != null ? target : (targetAlliance != null ? team : null);

        if (alliance == null) {
            alliance = new Alliance(UUID.randomUUID());
            alliancesById.put(alliance.getId(), alliance);
        }

        // Adding a side that's already a member (e.g. the existing side of the pair) is a
        // harmless no-op - Set#add and re-setting the same allianceId are both idempotent.
        alliance.getTeamIds().add(team.getId());
        alliance.getTeamIds().add(target.getId());
        team.setAllianceId(alliance.getId());
        target.setAllianceId(alliance.getId());
        team.setAllianceOfferTo(null);
        target.setAllianceOfferTo(null);

        TeamRepository.getInstance().runAsyncSave(team);
        TeamRepository.getInstance().runAsyncSave(target);
        TeamRepository.getInstance().runAsyncSaveAlliance(alliance);

        announceAlliance(team, target, joiningTeam, alliance);
        return Result.ok(Messages.get("teams.now-allied", "Your team is now allied with {team}", "team", target.getRawName()));
    }

    public static Result setAllianceName(Player actor, String rawName, String plainName) {
        TeamData team = getTeam(actor);
        if (team == null) return Result.fail(Messages.get("teams.not-on-team", "You aren't on a team"));
        if (!team.canModifyAlliance(actor.getUniqueId()))
            return Result.fail(Messages.get("teams.owner-or-trusted-only", "Only the team owner or trusted members can do that"));

        Alliance alliance = getAlliance(team);
        if (alliance == null) return Result.fail(Messages.get("teams.not-in-alliance", "Your team isn't in an alliance"));

        Alliance existingAlliance = findAllianceByPlainName(plainName);
        if (existingAlliance != null && !existingAlliance.getId().equals(alliance.getId()))
            return Result.fail(Messages.get("teams.alliance-name-taken", "Another alliance already has that name"));

        if (findTeamByPlainName(plainName) != null)
            return Result.fail(Messages.get("teams.team-name-conflict", "A team already has that name"));

        alliance.setRawName(rawName);
        TeamRepository.getInstance().runAsyncSaveAlliance(alliance);

        return Result.ok(Messages.get("teams.alliance-named", "Your alliance is now named: {name}", "name", rawName));
    }

    public static Result secedeAlliance(Player actor) {
        TeamData team = getTeam(actor);
        if (team == null) return Result.fail(Messages.get("teams.not-on-team", "You aren't on a team"));
        if (!team.canModifyAlliance(actor.getUniqueId()))
            return Result.fail(Messages.get("teams.owner-or-trusted-only", "Only the team owner or trusted members can do that"));

        if (team.getAllianceId() == null) {
            if (team.getAllianceOfferTo() == null)
                return Result.fail(Messages.get("teams.not-allied", "Your team isn't allied with anyone"));

            team.setAllianceOfferTo(null);
            TeamRepository.getInstance().runAsyncSave(team);
            return Result.ok(Messages.get("teams.offer-withdrawn", "Alliance offer withdrawn"));
        }

        removeFromAlliance(team);
        TeamRepository.getInstance().runAsyncSave(team);

        Bukkit.broadcast(Messages.msg("teams.secession-prefix", "<alert>")
                .append(MiniMessage.miniMessage().deserialize(team.getRawName()))
                .append(Messages.msg("teams.secession-suffix", "<alert> has declared secession from its alliance")));

        return Result.ok(Messages.get("teams.seceded", "Your team has declared secession and is back on its own"));
    }

    // joiningTeam is non-null when this merge grew an already-existing alliance (a 3rd+ team
    // joining) rather than forming a brand new 2-team one - in that case, and only once the
    // alliance actually has a name, the announcement highlights the alliance's name instead of
    // spelling out every existing member team.
    private static void announceAlliance(TeamData teamA, TeamData teamB, @Nullable TeamData joiningTeam, Alliance alliance) {
        Component message;

        if (joiningTeam != null && alliance.getRawName() != null) {
            message = MiniMessage.miniMessage().deserialize(joiningTeam.getRawName())
                    .append(Messages.msg("teams.announce-joined-mid", "<#63C14F> has joined the "))
                    .append(MiniMessage.miniMessage().deserialize(alliance.getRawName()))
                    .append(Messages.msg("teams.announce-joined-suffix", "<#63C14F> alliance!"));
        } else {
            message = Messages.msg("teams.announce-formed-prefix", "<#63C14F>Alliance formed: ")
                    .append(MiniMessage.miniMessage().deserialize(teamA.getRawName()))
                    .append(Messages.msg("teams.announce-formed-mid", "<#63C14F> and "))
                    .append(MiniMessage.miniMessage().deserialize(teamB.getRawName()))
                    .append(Messages.msg("teams.announce-formed-suffix", "<#63C14F> are now allied"));
        }

        Bukkit.broadcast(message);
    }

    // Buttons go out to every member of the target team at once, so they can't be limited to a
    // single total use - an untrusted member clicking first (and getting rejected by the
    // permission check) would otherwise permanently burn the one use and lock out whoever was
    // actually meant to respond. Validity is enforced by the current offer state instead, in
    // acceptAllianceOffer/declineAllianceOffer.
    private static void notifyAllianceOffer(TeamData proposing, TeamData target) {
        Component accept = Messages.msg("teams.button-yes", "<green><b>[Yes]</b>")
                .clickEvent(ClickEvent.callback(audience -> {
                    if (audience instanceof Player p) sendResult(p, acceptAllianceOffer(p, proposing.getId()));
                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).build()));

        Component deny = Messages.msg("teams.button-no", "<red><b>[No]</b>")
                .clickEvent(ClickEvent.callback(audience -> {
                    if (audience instanceof Player p) sendResult(p, declineAllianceOffer(p, proposing.getId()));
                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).build()));

        Component message = MiniMessage.miniMessage().deserialize(proposing.getRawName())
                .append(Messages.msg("teams.offer-received", "<primary> has offered your team an alliance"))
                .appendSpace().append(accept).appendSpace().append(deny);

        notifyTeam(target, message);
    }

    private static void notifyOfferWithdrawn(TeamData proposing, UUID previousTargetId) {
        TeamData previousTarget = getTeamById(previousTargetId);
        if (previousTarget == null) return;

        notifyTeam(previousTarget, MiniMessage.miniMessage().deserialize(proposing.getRawName())
                .append(Messages.msg("teams.offer-withdrawn-notice", "<error> withdrew their alliance offer to your team")));
    }

    public static Result declineAllianceOffer(Player actor, UUID proposingTeamId) {
        TeamData actorTeam = getTeam(actor);
        if (actorTeam == null) return Result.fail(Messages.get("teams.not-on-team", "You aren't on a team"));
        if (!actorTeam.canModifyAlliance(actor.getUniqueId()))
            return Result.fail(Messages.get("teams.owner-or-trusted-only", "Only the team owner or trusted members can do that"));

        TeamData proposingTeam = getTeamById(proposingTeamId);
        if (proposingTeam == null || !actorTeam.getId().equals(proposingTeam.getAllianceOfferTo()))
            return Result.fail(Messages.get("teams.offer-invalid", "That alliance offer is no longer valid"));

        proposingTeam.setAllianceOfferTo(null);
        TeamRepository.getInstance().runAsyncSave(proposingTeam);

        notifyTeam(proposingTeam, Messages.msg("teams.offer-declined-notice-prefix", "<error>Your alliance offer to ")
                .append(MiniMessage.miniMessage().deserialize(actorTeam.getRawName()))
                .append(Messages.msg("teams.offer-declined-notice-suffix", "<error> was declined")));

        return Result.ok(Messages.get("teams.offer-declined", "Alliance offer declined"));
    }

    private static void notifyTeam(TeamData team, Component message) {
        for (UUID member : team.getMembers()) {
            Player player = Bukkit.getPlayer(member);
            if (player != null) player.sendMessage(message);
        }
    }

    public static Result kickMember(Player actor, Player target) {
        TeamData team = getTeam(actor);
        if (team == null) return Result.fail(Messages.get("teams.not-on-team", "You aren't on a team"));

        if (!isSameTeam(actor, target)) return Result.fail(Messages.get("teams.not-on-your-team", "{player} isn't on your team", "player", target.getName()));

        if (!team.canKick(actor.getUniqueId(), target.getUniqueId())) {
            return Result.fail(target.getUniqueId().equals(team.getOwner())
                    ? Messages.get("teams.cant-remove-founder", "You can't remove the team founder")
                    : Messages.get("teams.no-kick-permission", "You don't have permission to remove people from your team"));
        }

        team.getMembers().remove(target.getUniqueId());
        team.getTrustedInviters().remove(target.getUniqueId());
        playerTeamId.remove(target.getUniqueId());
        friendlyKillCounts.remove(target.getUniqueId());
        TeamRepository.getInstance().runAsyncSave(team);

        target.sendMessage(Messages.msg("teams.you-were-removed", "<error>You were removed from your team"));
        return Result.ok(Messages.get("teams.member-removed", "Removed {player} from your team", "player", target.getName()));
    }

    private static void sendResult(Player player, Result result) {
        player.sendMessage(ComponentParser.parseRichString(
                Messages.brand((result.success() ? "<success>" : "<error>") + result.message())));
    }

    @Override
    public void onEnable() {
        teamsById.putAll(TeamRepository.getInstance().loadAllSync());
        alliancesById.putAll(TeamRepository.getInstance().loadAlliancesSync());

        // Alliance membership itself isn't stored on the alliance row - each team just points at
        // its alliance_id - so rebuild every Alliance's team set from that, or it'd load back in
        // with 0 members and immediately get self-healed away as degenerate.
        for (TeamData team : teamsById.values()) {
            for (UUID member : team.getMembers())
                playerTeamId.put(member, team.getId());

            if (team.getAllianceId() != null) {
                Alliance alliance = alliancesById.get(team.getAllianceId());
                if (alliance != null) alliance.getTeamIds().add(team.getId());
            }
        }

        TeamRepository.getInstance().loadNameRulesSync().forEach((name, rule) -> {
            if ("BLACKLIST".equals(rule)) blacklistedNames.add(name);
            else whitelistedNames.add(name);
        });

        log.info("Loaded {} team(s), {} alliance(s)", teamsById.size(), alliancesById.size());

        ScheduleUtil.scheduleSync(() -> pendingInvites.values().removeIf(PendingInvite::isExpired), 5, 5, ScheduleUtil.Unit.MINUTE);
    }

    // Friendly fire between teammates/allies is softened rather than fully blocked - reduced
    // damage here, reduced knockback in onKnockback below, plus a heads-up to both parties so
    // it's clear a hit landed rather than silently vanishing.
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (attacker.getUniqueId().equals(victim.getUniqueId())) return; // self-inflicted damage isn't friendly fire
        if (!isSameTeamOrAllied(attacker, victim)) return;

        event.setDamage(event.getDamage() * allyDamageMultiplier());
        alertFriendlyFire(attacker, victim);
    }

    @EventHandler
    public void onKnockback(EntityKnockbackByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getSourceEntity() instanceof Player attacker)) return;
        if (!isSameTeamOrAllied(attacker, victim)) return;

        event.setFinalKnockback(event.getFinalKnockback().multiply(allyKnockbackMultiplier()));
    }

    // Actually killing a teammate (not just softened friendly-fire damage) counts towards a
    // 3-strike limit - the killer gets removed from the team on the 3rd kill. Allies aren't
    // included since removing someone from another team's roster doesn't make sense here.
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;
        // Dying to your own firework (or any other self-inflicted source Paper attributes back to
        // you) isn't friendly fire - isSameTeam(self, self) is always true, so this has to be
        // filtered out before the team comparison or every self-kill becomes a strike.
        if (killer.getUniqueId().equals(victim.getUniqueId())) return;
        if (!isSameTeam(killer, victim)) return;

        int count = friendlyKillCounts.merge(killer.getUniqueId(), 1, Integer::sum);
        if (count < friendlyKillLimit()) {
            int remaining = friendlyKillLimit() - count;
            killer.sendMessage(Messages.msg("teams.friendly-kill-warning",
                    "<error>Warning: killing {remaining} more teammate{s} will get you removed from your team",
                    "remaining", String.valueOf(remaining), "s", remaining == 1 ? "" : "s"));
            return;
        }

        TeamData team = getTeam(killer);
        friendlyKillCounts.remove(killer.getUniqueId());
        if (team == null) return; // already off the team somehow - nothing to remove them from

        boolean disbanded = leaveTeamSilently(killer);
        killer.sendMessage(Messages.msg("teams.friendly-kill-removed",
                "<error>You were removed from your team for killing {limit} teammates",
                "limit", String.valueOf(friendlyKillLimit())));

        if (!disbanded) {
            notifyTeam(team, Messages.msg("teams.friendly-kill-removed-broadcast",
                    "<error>{player} was removed from the team for killing {limit} teammates",
                    "player", killer.getName(), "limit", String.valueOf(friendlyKillLimit())));
        }
    }

    // Notifies every member of both the attacker's and victim's teams, not just the two people
    // involved - respects each recipient's own /togglefriendlyfirealerts preference.
    private static void alertFriendlyFire(Player attacker, Player victim) {
        Component message = Messages.msg("teams.friendly-fire-alert",
                "<error>{attacker} hit teammate/ally {victim} (damage reduced)",
                "attacker", attacker.getName(), "victim", victim.getName());

        TeamData attackerTeam = getTeam(attacker);
        TeamData victimTeam = getTeam(victim);

        if (attackerTeam != null) notifyTeamIfAlertsEnabled(attackerTeam, message);
        if (victimTeam != null && (attackerTeam == null || !attackerTeam.getId().equals(victimTeam.getId())))
            notifyTeamIfAlertsEnabled(victimTeam, message);
    }

    private static void notifyTeamIfAlertsEnabled(TeamData team, Component message) {
        for (UUID member : team.getMembers()) {
            Player player = Bukkit.getPlayer(member);
            if (player != null && FriendlyFireAlertManager.isEnabled(player)) player.sendMessage(message);
        }
    }
}
