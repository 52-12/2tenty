package threeadd.glimpsy.feature.stats;

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
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class StatsCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int runSelf(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        sendStats(ctx, player);
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int runOther(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        PlayerSelectorArgumentResolver resolver = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
        List<Player> targets = resolver.resolve(ctx.getSource());
        if (targets.isEmpty()) {
            ctx.getSource().getSender().sendMessage(Messages.msg("misc.no-matching-player", "<error>No matching player found"));
            return SUCCESS;
        }

        sendStats(ctx, targets.getFirst());
        return SUCCESS;
    }

    private static void sendStats(CommandContext<CommandSourceStack> ctx, Player target) {
        PlayerStats stats = StatsManager.getStats(target.getUniqueId());

        Component message = Messages.msg("stats.stats-header", "<primary>--- {player}'s Stats ---", "player", target.getName());

        for (StatCategory category : StatCategory.values()) {
            long value = stats != null ? category.extract(stats) : 0;
            String label = category.getTitle().replace("Top ", "");
            message = message.appendNewline().append(Messages.msg("stats.stats-entry",
                    "<highlight>{label}: <white>{value}", "label", label, "value", category.formatValue(value)));
        }

        ctx.getSource().getSender().sendMessage(message);
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString(
                "Shows your fish caught, kills, deaths, and playtime - or another player's with /stats <player>");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("stats")
                .executes(StatsCommand::runSelf)
                .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(StatsCommand::runOther));
    }
}
