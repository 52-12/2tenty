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
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.List;

/**
 * Staff-only override to set another player's team name directly. Deliberately skips
 * {@link SimpleWordFilter} - trusted staff assigning a name are the mitigation for the
 * Scunthorpe problem (an innocuous name getting blocked because it happens to contain a
 * banned substring, e.g. "Scunthorpe" containing "cunt"). The hard constraints (length,
 * decorations, character whitelist) from {@link TeamNameValidator} still always apply.
 */
@SuppressWarnings("UnstableApiUsage")
public class TeamSetCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        PlayerSelectorArgumentResolver resolver = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
        List<Player> targets = resolver.resolve(ctx.getSource());
        if (targets.isEmpty()) {
            ctx.getSource().getSender().sendMessage(ComponentParser.parseRichString("<red>No matching player found"));
            return SUCCESS;
        }
        Player target = targets.getFirst();

        String rawName = StringArgumentType.getString(ctx, "name");

        TeamNameValidator.Result result = TeamNameValidator.validate(rawName);
        if (!result.valid()) {
            ctx.getSource().getSender().sendMessage(ComponentParser.parseRichString("<red>" + result.errorMessage()));
            return SUCCESS;
        }

        // Joins an existing team with this name (bypassing invites too, another staff
        // override), or creates one with the target as owner if none exists yet.
        TeamManager.forceJoinOrCreate(target, rawName, result.plainText());

        ctx.getSource().getSender().sendMessage(
                Component.text(target.getName() + "'s team is now: ").append(result.parsed()));
        target.sendMessage(Component.text("Your team was set to: ").append(result.parsed()));

        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Force another player onto a team by name, bypassing invites and the word filter");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("teamset")
                .requires(source -> source.getSender().hasPermission("2tenty.command.teamset"))
                .then(Commands.argument("player", ArgumentTypes.player())
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(TeamSetCommand::run)));
    }

    @Override
    public boolean isVisibleTo(Player viewer) {
        return viewer.hasPermission("2tenty.command.teamset");
    }
}
