package threeadd.glimpsy.feature.stats;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.List;
import java.util.Locale;

@SuppressWarnings("UnstableApiUsage")
public class TopCommand extends CustomCommand {

    private static int listSize() {
        return ConfigManager.getInt("stats.top-list-size", 10);
    }

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx, StatCategory category) {
        List<StatsManager.TopEntry> entries = StatsManager.getTop(category, listSize());

        Component message = Messages.msg("stats.top-header", "<primary>--- {title} ---", "title", category.getTitle());

        if (entries.isEmpty()) {
            message = message.appendNewline()
                    .append(Messages.msg("stats.top-empty", "<muted>Nothing recorded yet"));
        }

        for (StatsManager.TopEntry entry : entries) {
            String name = entry.player().getName() != null ? entry.player().getName() : "Unknown";
            message = message.appendNewline().append(Messages.msg("stats.top-entry",
                    "<highlight>{position}. <white>{player} <muted>- <white>{value} <muted>{unit}",
                    "position", String.valueOf(entry.position()), "player", name,
                    "value", category.formatValue(entry.value()), "unit", category.getUnit()));
        }

        if (ctx.getSource().getSender() instanceof Player player) {
            int rank = StatsManager.getRank(category, player.getUniqueId());
            if (rank > 0) {
                PlayerStats own = StatsManager.getStats(player.getUniqueId());
                message = message.appendNewline().append(Messages.msg("stats.top-own-rank",
                        "<muted>Your rank: <white>#{rank} <muted>({value} {unit})",
                        "rank", String.valueOf(rank),
                        "value", category.formatValue(category.extract(own)), "unit", category.getUnit()));
            }
        }

        ctx.getSource().getSender().sendMessage(message);
        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString(
                "Shows the server leaderboards: /top fishes, /top kills, /top deaths, or /top playtime");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("top");

        for (StatCategory category : StatCategory.values()) {
            root = root.then(Commands.literal(category.name().toLowerCase(Locale.ROOT))
                    .executes(ctx -> run(ctx, category)));
        }

        return root;
    }
}
