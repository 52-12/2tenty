package threeadd.glimpsy.feature.map;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapResetCommand extends CustomCommand {

    private static final Pattern DURATION = Pattern.compile("(\\d+)([dhms])");

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        if (!Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit")) {
            ctx.getSource().getSender().sendMessage(ComponentParser.parseRichString(
                    "Map reset requires the FastAsyncWorldEdit plugin, which isn't installed on this server"));
            return SUCCESS;
        }

        if (!MapSetter.reset(Integer.MAX_VALUE)) {
            ctx.getSource().getSender().sendMessage(ComponentParser.parseRichString(
                    "A map reset is already in progress, or no world border is set"));
            return SUCCESS;
        }

        // A manual reset restarts the automatic countdown from a fresh interval
        MapResetManager.resetCountdown();
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int pause(CommandContext<CommandSourceStack> ctx) {
        boolean paused = MapResetManager.pause();
        ctx.getSource().getSender().sendMessage(paused
                ? Messages.msg("map-reset.paused", "<success>Map reset countdown paused at {time}",
                        "time", MapResetManager.formatLong())
                : Messages.msg("map-reset.already-paused", "<error>The map reset countdown is already paused"));
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int resume(CommandContext<CommandSourceStack> ctx) {
        boolean resumed = MapResetManager.resume();
        ctx.getSource().getSender().sendMessage(resumed
                ? Messages.msg("map-reset.resumed", "<success>Map reset countdown resumed - resets in {time}",
                        "time", MapResetManager.formatLong())
                : Messages.msg("map-reset.not-paused", "<error>The map reset countdown isn't paused"));
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int addTime(CommandContext<CommandSourceStack> ctx, boolean remove) {
        String input = StringArgumentType.getString(ctx, "duration");
        long seconds = parseDurationSeconds(input);
        if (seconds < 0) {
            ctx.getSource().getSender().sendMessage(Messages.msg("map-reset.bad-duration",
                    "<error>Invalid duration '{input}' - use e.g. 2d, 5h, 30m, 45s or 1h30m", "input", input));
            return SUCCESS;
        }

        MapResetManager.addSeconds(remove ? -seconds : seconds);
        ctx.getSource().getSender().sendMessage(remove
                ? Messages.msg("map-reset.removed", "<success>Removed {amount} - the map now resets in {time}",
                        "amount", input, "time", MapResetManager.formatLong())
                : Messages.msg("map-reset.added", "<success>Added {amount} - the map now resets in {time}",
                        "amount", input, "time", MapResetManager.formatLong()));
        return SUCCESS;
    }

    // Parses a compact duration like "1d2h30m" into seconds; -1 if the whole string didn't parse.
    private static long parseDurationSeconds(String input) {
        Matcher matcher = DURATION.matcher(input.toLowerCase());
        long total = 0;
        int matchedTo = 0;
        boolean any = false;

        while (matcher.find()) {
            any = true;
            long value = Long.parseLong(matcher.group(1));
            total += switch (matcher.group(2)) {
                case "d" -> value * 86400;
                case "h" -> value * 3600;
                case "m" -> value * 60;
                default -> value;
            };
            matchedTo = matcher.end();
        }

        return (any && matchedTo == input.length()) ? total : -1;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString(
                "Reset the map now, or manage the automatic countdown: /mapreset pause, resume, add <time>, remove <time>");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("mapreset")
                .requires(source -> source.getSender().hasPermission("2tenty.command.mapreset"))
                .executes(MapResetCommand::run)
                .then(Commands.literal("pause").executes(MapResetCommand::pause))
                .then(Commands.literal("resume").executes(MapResetCommand::resume))
                .then(Commands.literal("add")
                        .then(Commands.argument("duration", StringArgumentType.string())
                                .executes(ctx -> addTime(ctx, false))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("duration", StringArgumentType.string())
                                .executes(ctx -> addTime(ctx, true))));
    }

    @Override
    public List<String> getAliases() {
        return List.of("worldreset");
    }

    @Override
    public boolean isVisibleTo(Player viewer) {
        return viewer.hasPermission("2tenty.command.mapreset");
    }

    // Same node string as the auto-derived one - must stay OP or the blanket TRUE
    // registration would open this staff command to everyone
    @Override
    public PermissionDefault getPermissionDefault() {
        return PermissionDefault.OP;
    }
}
