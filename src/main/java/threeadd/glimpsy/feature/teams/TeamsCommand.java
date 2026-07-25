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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class TeamsCommand extends CustomCommand {

    private static Player resolveTarget(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        PlayerSelectorArgumentResolver resolver = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
        List<Player> targets = resolver.resolve(ctx.getSource());
        return targets.isEmpty() ? null : targets.getFirst();
    }

    private static void reply(CommandContext<CommandSourceStack> ctx, TeamManager.Result result) {
        ctx.getSource().getSender().sendMessage(ComponentParser.parseRichString(
                (result.success() ? "<green>" : "<red>") + result.message()));
    }

    @SuppressWarnings("SameReturnValue")
    private static int createTeam(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        String rawName = StringArgumentType.getString(ctx, "name");

        TeamNameValidator.Result validation = TeamNameValidator.validate(rawName);
        if (!validation.valid()) {
            player.sendMessage(ComponentParser.parseRichString("<red>" + validation.errorMessage()));
            return SUCCESS;
        }

        if (!SimpleWordFilter.isAppropriate(validation.plainText())) {
            player.sendMessage(ComponentParser.parseRichString("<red>That name isn't allowed"));
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
            player.sendMessage(ComponentParser.parseRichString("<red>" + validation.errorMessage()));
            return SUCCESS;
        }

        if (!SimpleWordFilter.isAppropriate(validation.plainText())) {
            player.sendMessage(ComponentParser.parseRichString("<red>That name isn't allowed"));
            return SUCCESS;
        }

        reply(ctx, TeamManager.renameTeam(player, rawName, validation.plainText()));
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
            player.sendMessage(ComponentParser.parseRichString("<red>No matching player found"));
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
            player.sendMessage(ComponentParser.parseRichString("<red>You aren't on a team"));
            return SUCCESS;
        }

        Component list = Component.text("Team members: ");
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
            player.sendMessage(ComponentParser.parseRichString("<red>No matching player found"));
            return SUCCESS;
        }

        reply(ctx, TeamManager.setTrusted(player, target, trusted));
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int kick(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        Player target = resolveTarget(ctx);
        if (target == null) {
            player.sendMessage(ComponentParser.parseRichString("<red>No matching player found"));
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
                                .executes(TeamsCommand::kick)));
    }

    @Override
    public List<String> getAliases() {
        // Shadows vanilla's own /team command under the bare name - vanilla's version is
        // still reachable via /minecraft:team.
        return List.of("team");
    }
}
