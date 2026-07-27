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
public class PlaytimeCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int self(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        send(ctx, player);
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int other(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        PlayerSelectorArgumentResolver resolver = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
        List<Player> targets = resolver.resolve(ctx.getSource());
        if (targets.isEmpty()) {
            ctx.getSource().getSender().sendMessage(Messages.msg("misc.no-matching-player", "<error>No matching player found"));
            return SUCCESS;
        }

        send(ctx, targets.getFirst());
        return SUCCESS;
    }

    private static void send(CommandContext<CommandSourceStack> ctx, Player target) {
        PlayerStats stats = StatsManager.getStats(target.getUniqueId());
        String playtime = StatCategory.PLAYTIME.formatValue(stats != null ? stats.getPlaytimeMillis() : 0);

        ctx.getSource().getSender().sendMessage(Messages.msg("playtime.value",
                "<primary>{player}<muted>'s playtime: <white>{playtime}",
                "player", target.getName(), "playtime", playtime));
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Shows your total playtime, or another player's with /playtime <player>");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("playtime")
                .executes(PlaytimeCommand::self)
                .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(PlaytimeCommand::other));
    }
}
