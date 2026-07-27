package threeadd.glimpsy.feature.timezone;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.text.ComponentParser;

import java.time.ZoneId;
import java.util.Optional;

public class TimezoneCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int show(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        player.sendMessage(Messages.msg("timezone.current",
                "<primary>Your timezone is <white>{zone}", "zone", TimezoneManager.get(player).getId()));
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int reset(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        TimezoneManager.clear(player);
        player.sendMessage(Messages.msg("timezone.reset",
                "<success>Your timezone was reset to the server default (<white>{zone}<success>)",
                "zone", ZoneId.systemDefault().getId()));
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int set(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        String input = StringArgumentType.getString(ctx, "zone");

        Optional<ZoneId> parsed = Timezones.parse(input);
        if (parsed.isEmpty()) {
            player.sendMessage(Messages.msg("timezone.invalid",
                    "<error>Unknown timezone '{input}' - use an abbreviation like EST, PST, GMT, CET, JST, or a UTC offset like 500 / 0530 / -800",
                    "input", input));
            return SUCCESS;
        }

        ZoneId zone = parsed.get();
        TimezoneManager.set(player, zone);
        player.sendMessage(Messages.msg("timezone.set",
                "<success>Your timezone is now <white>{zone}", "zone", zone.getId()));
        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString(
                "Set the timezone your scoreboard clock uses, e.g. /timezone EST or /timezone 0530 - /timezone reset goes back to the server default");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("timezone")
                .executes(TimezoneCommand::show)
                .then(Commands.literal("reset").executes(TimezoneCommand::reset))
                // greedyString (not string) so colon offsets like +03:30 aren't rejected -
                // Brigadier's unquoted-string rules don't allow the ':' character
                .then(Commands.argument("zone", StringArgumentType.greedyString())
                        .executes(TimezoneCommand::set));
    }
}
