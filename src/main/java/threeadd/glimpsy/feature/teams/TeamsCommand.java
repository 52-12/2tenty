package threeadd.glimpsy.feature.teams;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class TeamsCommand extends CustomCommand {

    // Gates the staff subcommands (blacklist/whitelist/replace - the old /teamset overrides,
    // now folded in here).
    private static final String ADMIN_PERMISSION = "2tenty.command.team.admin";

    private static Player resolveTarget(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        PlayerSelectorArgumentResolver resolver = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
        List<Player> targets = resolver.resolve(ctx.getSource());
        return targets.isEmpty() ? null : targets.getFirst();
    }

    private static void reply(CommandContext<CommandSourceStack> ctx, TeamManager.Result result) {
        ctx.getSource().getSender().sendMessage(ComponentParser.parseRichString(
                Messages.brand((result.success() ? "<success>" : "<error>") + result.message())));
    }

    @SuppressWarnings("SameReturnValue")
    private static int createTeam(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        String rawName = StringArgumentType.getString(ctx, "name");

        TeamNameValidator.Result validation = TeamNameValidator.validate(rawName);
        if (!validation.valid()) {
            player.sendMessage(ComponentParser.parseRichString(Messages.brand("<error>" + validation.errorMessage())));
            return SUCCESS;
        }

        if (!TeamManager.isNameAllowed(validation.plainText())) {
            player.sendMessage(Messages.msg("teams.name-not-allowed", "<error>That name isn't allowed"));
            return SUCCESS;
        }

        reply(ctx, TeamManager.createTeam(player, rawName, validation.plainText()));
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int rename(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        String rawName = StringArgumentType.getString(ctx, "name");

        TeamNameValidator.Result validation = TeamNameValidator.validate(rawName);
        if (!validation.valid()) {
            player.sendMessage(ComponentParser.parseRichString(Messages.brand("<error>" + validation.errorMessage())));
            return SUCCESS;
        }

        if (!TeamManager.isNameAllowed(validation.plainText())) {
            player.sendMessage(Messages.msg("teams.name-not-allowed", "<error>That name isn't allowed"));
            return SUCCESS;
        }

        reply(ctx, TeamManager.renameTeam(player, rawName, validation.plainText()));
        return SUCCESS;
    }

    // Staff subcommands - all require ADMIN_PERMISSION via .requires() below

    @SuppressWarnings("SameReturnValue")
    private static int blacklist(CommandContext<CommandSourceStack> ctx) {
        reply(ctx, TeamManager.blacklistName(StringArgumentType.getString(ctx, "name")));
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int whitelist(CommandContext<CommandSourceStack> ctx) {
        reply(ctx, TeamManager.whitelistName(StringArgumentType.getString(ctx, "name")));
        return SUCCESS;
    }

    // Resolves the replace target as a team name first, then as an online player's team
    private static @Nullable TeamData resolveReplaceTarget(String target) {
        TeamData team = TeamManager.findTeam(target);
        if (team != null) return team;

        Player targetPlayer = Bukkit.getPlayerExact(target);
        return targetPlayer == null ? null : TeamManager.getTeam(targetPlayer);
    }

    @SuppressWarnings("SameReturnValue")
    private static int replaceRename(CommandContext<CommandSourceStack> ctx) {
        String target = StringArgumentType.getString(ctx, "target");
        String rawName = StringArgumentType.getString(ctx, "newName");

        TeamNameValidator.Result validation = TeamNameValidator.validate(rawName, false);
        if (!validation.valid()) {
            ctx.getSource().getSender().sendMessage(ComponentParser.parseRichString(
                    Messages.brand("<error>" + validation.errorMessage())));
            return SUCCESS;
        }

        TeamData team = resolveReplaceTarget(target);
        if (team == null) {
            // A player target with no team gets force-joined onto the named team instead
            // (created with them as owner if it doesn't exist yet)
            Player targetPlayer = Bukkit.getPlayerExact(target);
            if (targetPlayer != null) {
                reply(ctx, TeamManager.forceJoin(targetPlayer, rawName, validation.plainText()));
                return SUCCESS;
            }

            ctx.getSource().getSender().sendMessage(Messages.msg("teams.replace-no-target",
                    "<error>No team or online player named {target}", "target", target));
            return SUCCESS;
        }

        reply(ctx, TeamManager.adminRename(team, rawName, validation.plainText()));
        return SUCCESS;
    }

    // No new name given - a player target is removed from their team instead
    @SuppressWarnings("SameReturnValue")
    private static int replaceRemove(CommandContext<CommandSourceStack> ctx) {
        String target = StringArgumentType.getString(ctx, "target");

        Player targetPlayer = Bukkit.getPlayerExact(target);
        if (targetPlayer == null) {
            ctx.getSource().getSender().sendMessage(Messages.msg("teams.replace-no-player",
                    "<error>No online player named {target} - add a new name to rename a team instead", "target", target));
            return SUCCESS;
        }

        TeamManager.Result result = TeamManager.leaveTeam(targetPlayer);
        ctx.getSource().getSender().sendMessage(ComponentParser.parseRichString(
                Messages.brand((result.success() ? "<success>" : "<error>") + targetPlayer.getName() + ": " + result.message())));

        if (result.success())
            targetPlayer.sendMessage(Messages.msg("teams.removed-by-staff", "<error>You were removed from your team by staff"));

        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int leave(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        reply(ctx, TeamManager.leaveTeam(player));
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int invite(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        Player target = resolveTarget(ctx);
        if (target == null) {
            player.sendMessage(Messages.msg("misc.no-matching-player", "<error>No matching player found"));
            return SUCCESS;
        }

        reply(ctx, TeamManager.invite(player, target));
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int accept(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        reply(ctx, TeamManager.acceptInvite(player));
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int deny(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        reply(ctx, TeamManager.denyInvite(player));
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int members(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        TeamData team = TeamManager.getTeam(player);
        if (team == null) {
            player.sendMessage(Messages.msg("teams.not-on-team-error", "<error>You aren't on a team"));
            return SUCCESS;
        }

        Component list = Messages.msg("teams.members-header", "Team members: ");
        boolean first = true;
        for (UUID member : team.getMembers()) {
            String name = Bukkit.getOfflinePlayer(member).getName();
            if (name == null) name = member.toString();

            if (!first) list = list.append(Component.text(", "));
            first = false;

            String suffix = member.equals(team.getOwner()) ? " (owner)" : team.getTrustedInviters().contains(member) ? " (trusted)" : "";
            list = list.append(Component.text(name + suffix));
        }

        player.sendMessage(list);
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int mode(CommandContext<CommandSourceStack> ctx, TeamData.InviteMode mode) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        reply(ctx, TeamManager.setInviteMode(player, mode));
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int trust(CommandContext<CommandSourceStack> ctx, boolean trusted) throws CommandSyntaxException {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        Player target = resolveTarget(ctx);
        if (target == null) {
            player.sendMessage(Messages.msg("misc.no-matching-player", "<error>No matching player found"));
            return SUCCESS;
        }

        reply(ctx, TeamManager.setTrusted(player, target, trusted));
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int list(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        Collection<TeamData> teams = TeamManager.getAllTeams();
        if (teams.isEmpty()) {
            player.sendMessage(Messages.msg("teams.no-teams", "<muted>No teams exist yet"));
            return SUCCESS;
        }

        player.sendMessage(Messages.msg("teams.list-header", "<primary>Teams:"));

        for (TeamData team : teams) {
            Component line = Component.text("- ")
                    .append(MiniMessage.miniMessage().deserialize(team.getRawName()))
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(memberList(team))
                    .append(allianceSuffix(team));

            player.sendMessage(line);
        }

        return SUCCESS;
    }

    private static Component memberList(TeamData team) {
        Component list = Component.empty();
        boolean first = true;

        for (UUID member : team.getMembers()) {
            String name = Bukkit.getOfflinePlayer(member).getName();
            if (name == null) name = member.toString();

            if (!first) list = list.append(Component.text(", ", NamedTextColor.GRAY));
            first = false;

            String suffix = member.equals(team.getOwner()) ? " (owner)"
                    : team.getTrustedInviters().contains(member) ? " (trusted)" : "";
            list = list.append(Component.text(name + suffix, NamedTextColor.GRAY));
        }

        return list;
    }

    private static Component allianceSuffix(TeamData team) {
        Alliance alliance = TeamManager.getAlliance(team);
        if (alliance == null) return Component.empty();

        if (alliance.getRawName() != null) {
            return Component.text(" [allied: ", NamedTextColor.AQUA)
                    .append(MiniMessage.miniMessage().deserialize(alliance.getRawName()))
                    .append(Component.text("]", NamedTextColor.AQUA));
        }

        List<String> otherNames = new ArrayList<>();
        for (UUID otherId : alliance.getTeamIds()) {
            if (otherId.equals(team.getId())) continue;

            TeamData other = TeamManager.getTeamById(otherId);
            if (other == null) continue;

            otherNames.add(PlainTextComponentSerializer.plainText()
                    .serialize(MiniMessage.miniMessage().deserialize(other.getRawName())));
        }

        return Component.text(" [allied with " + String.join(", ", otherNames) + "]", NamedTextColor.AQUA);
    }

    @SuppressWarnings("SameReturnValue")
    private static int alliance(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        String targetName = StringArgumentType.getString(ctx, "name");

        reply(ctx, TeamManager.proposeAlliance(player, targetName));
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int secede(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        reply(ctx, TeamManager.secedeAlliance(player));
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int kick(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        Player target = resolveTarget(ctx);
        if (target == null) {
            player.sendMessage(Messages.msg("misc.no-matching-player", "<error>No matching player found"));
            return SUCCESS;
        }

        reply(ctx, TeamManager.kickMember(player, target));
        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Create and manage your own team (max 12 characters, colors only)");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("teams")
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(TeamsCommand::createTeam)))
                .then(Commands.literal("rename")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(TeamsCommand::rename)))
                .then(Commands.literal("leave")
                        .executes(TeamsCommand::leave))
                .then(Commands.literal("list")
                        .executes(TeamsCommand::list))
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(TeamsCommand::invite)))
                .then(Commands.literal("accept")
                        .executes(TeamsCommand::accept))
                .then(Commands.literal("deny")
                        .executes(TeamsCommand::deny))
                .then(Commands.literal("members")
                        .executes(TeamsCommand::members))
                .then(Commands.literal("mode")
                        .then(Commands.literal("everyone")
                                .executes(ctx -> mode(ctx, TeamData.InviteMode.EVERYONE)))
                        .then(Commands.literal("owneronly")
                                .executes(ctx -> mode(ctx, TeamData.InviteMode.OWNER_ONLY)))
                        .then(Commands.literal("trusted")
                                .executes(ctx -> mode(ctx, TeamData.InviteMode.TRUSTED))))
                .then(Commands.literal("trust")
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(ctx -> trust(ctx, true))))
                .then(Commands.literal("untrust")
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(ctx -> trust(ctx, false))))
                .then(Commands.literal("kick")
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(TeamsCommand::kick)))
                .then(Commands.literal("alliance")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(TeamsCommand::alliance)))
                .then(Commands.literal("secede")
                        .executes(TeamsCommand::secede))
                .then(Commands.literal("blacklist")
                        .requires(source -> source.getSender().hasPermission(ADMIN_PERMISSION))
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(TeamsCommand::blacklist)))
                .then(Commands.literal("whitelist")
                        .requires(source -> source.getSender().hasPermission(ADMIN_PERMISSION))
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(TeamsCommand::whitelist)))
                .then(Commands.literal("replace")
                        .requires(source -> source.getSender().hasPermission(ADMIN_PERMISSION))
                        .then(Commands.argument("target", StringArgumentType.string())
                                .executes(TeamsCommand::replaceRemove)
                                .then(Commands.argument("newName", StringArgumentType.greedyString())
                                        .executes(TeamsCommand::replaceRename))));
    }

    @Override
    public List<String> getAliases() {
        // Shadows vanilla's own /team command under the bare name - vanilla's version is
        // still reachable via /minecraft:team.
        return List.of("team");
    }
}
