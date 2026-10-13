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

        sendSummary(player, TimezoneManager.get(player));
        return SUCCESS;
    }

    /**
     * Every change echoes the full settings back, so a player always sees what the option they
     * just flipped did to their clock. This is the one and only place the zone gets named - the
     * individual "that's now on/off" confirmations deliberately don't repeat it, or a single
     * command would say "Eastern Time" three times over.
     */
    private static void sendSummary(Player player, TimezonePreferences prefs) {
        sendSummary(player, prefs, true);
    }

    /**
     * @param includeOptions false when the player only changed their zone - the dst/seconds/format
     *                       rundown is noise there, since they didn't touch any of them. The
     *                       option subcommands and a bare /timezone still show it.
     */
    private static void sendSummary(Player player, TimezonePreferences prefs, boolean includeOptions) {
        player.sendMessage(Messages.msg("timezone.current",
                "<primary>Your timezone is <white>{zone}<muted> - now <white>{time}<muted> on <white>{date}",
                "zone", Timezones.displayName(prefs.resolveZone()),
                "time", TimeDisplay.time(prefs),
                "date", TimeDisplay.date(prefs)));

        if (!includeOptions) return;

        player.sendMessage(Messages.msg("timezone.current-options",
                "<muted>Daylight savings <white>{dst}<muted>, seconds <white>{seconds}<muted>, format <white>{format}",
                "dst", onOff(prefs.isDaylightSavings()),
                "seconds", onOff(prefs.isShowSeconds()),
                "format", formatName(prefs)));
    }

    private static String onOff(boolean value) {
        // Deliberately not keyed "timezone.on"/"timezone.off" - YAML parses those bare keys as
        // booleans, so the yml overrides would never be found
        return value ? Messages.get("timezone.state-on", "on") : Messages.get("timezone.state-off", "off");
    }

    private static String formatName(TimezonePreferences prefs) {
        return switch (prefs.getFormat()) {
            case US -> Messages.get("timezone.format-us", "US (month/day/year, 12-hour)");
            case EU -> Messages.get("timezone.format-eu", "EU (day/month/year, 24-hour)");
            // Worth spelling out which way automatic actually landed for this player's zone
            case AUTO -> Messages.get("timezone.format-auto", "automatic ({resolved})",
                    "resolved", prefs.usesUsFormat()
                            ? Messages.get("timezone.format-us-short", "US")
                            : Messages.get("timezone.format-eu-short", "EU"));
        };
    }

    @SuppressWarnings("SameReturnValue")
    private static int reset(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        TimezoneManager.clear(player);
        player.sendMessage(Messages.msg("timezone.reset",
                "<success>Your clock settings were reset to the server default (<white>{zone}<success>)",
                "zone", Timezones.displayName(ZoneId.systemDefault())));
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
        TimezonePreferences prefs = TimezoneManager.update(player, p -> p.setZone(zone));

        // The summary below already names the new zone - no separate "your timezone is now X",
        // and no dst/seconds/format rundown either, since none of those were touched
        sendSummary(player, prefs, false);

        // A zone with no daylight savings at all makes the dst setting meaningless - say so
        // rather than letting someone toggle it forever wondering why nothing changes
        if (!Timezones.observesDaylightSavings(zone)) sendNoDaylightSavingsNotice(player);
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int setDaylightSavings(CommandContext<CommandSourceStack> ctx, Boolean value) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        // null = plain "/timezone dst", which flips whatever it's on now
        boolean enabled = value != null ? value : !TimezoneManager.get(player).isDaylightSavings();
        TimezonePreferences prefs = TimezoneManager.update(player, p -> p.setDaylightSavings(enabled));

        player.sendMessage(enabled
                ? Messages.msg("timezone.dst-on", "<success>Daylight savings is now followed - your clock shifts with the season")
                : Messages.msg("timezone.dst-off", "<success>Daylight savings is now ignored - your clock stays on standard time all year"));

        sendSummary(player, prefs);

        if (enabled && !Timezones.observesDaylightSavings(prefs.resolveZone()))
            sendNoDaylightSavingsNotice(player);
        return SUCCESS;
    }

    private static void sendNoDaylightSavingsNotice(Player player) {
        player.sendMessage(Messages.msg("timezone.no-dst",
                "<muted>That zone doesn't observe daylight savings, so the setting won't change anything"));
    }

    @SuppressWarnings("SameReturnValue")
    private static int setSeconds(CommandContext<CommandSourceStack> ctx, Boolean value) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        boolean enabled = value != null ? value : !TimezoneManager.get(player).isShowSeconds();
        TimezonePreferences prefs = TimezoneManager.update(player, p -> p.setShowSeconds(enabled));

        player.sendMessage(enabled
                ? Messages.msg("timezone.seconds-on", "<success>Your clock now shows seconds")
                : Messages.msg("timezone.seconds-off", "<success>Your clock no longer shows seconds"));

        sendSummary(player, prefs);
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int setFormat(CommandContext<CommandSourceStack> ctx, TimezonePreferences.Format format) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        TimezonePreferences prefs = TimezoneManager.update(player, p -> p.setFormat(format));

        player.sendMessage(Messages.msg("timezone.format-set",
                "<success>Your date and time now use the <white>{format}<success> format",
                "format", formatName(prefs)));

        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString(
                "Set the timezone your scoreboard clock uses, e.g. /timezone EST or /timezone 0530. "
                        + "/timezone dst turns daylight savings on or off, /timezone seconds hides the seconds, "
                        + "/timezone us and /timezone eu switch the date and clock format, and /timezone reset "
                        + "goes back to the server default");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("timezone")
                .executes(TimezoneCommand::show)
                .then(Commands.literal("reset").executes(TimezoneCommand::reset))
                .then(Commands.literal("dst")
                        .executes(ctx -> setDaylightSavings(ctx, null))
                        .then(Commands.literal("on").executes(ctx -> setDaylightSavings(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> setDaylightSavings(ctx, false))))
                .then(Commands.literal("seconds")
                        .executes(ctx -> setSeconds(ctx, null))
                        .then(Commands.literal("on").executes(ctx -> setSeconds(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> setSeconds(ctx, false))))
                .then(Commands.literal("us").executes(ctx -> setFormat(ctx, TimezonePreferences.Format.US)))
                .then(Commands.literal("eu").executes(ctx -> setFormat(ctx, TimezonePreferences.Format.EU)))
                .then(Commands.literal("auto").executes(ctx -> setFormat(ctx, TimezonePreferences.Format.AUTO)))
                // greedyString (not string) so colon offsets like +03:30 aren't rejected -
                // Brigadier's unquoted-string rules don't allow the ':' character. Declared last
                // so the literals above win when the input is exactly one of them.
                .then(Commands.argument("zone", StringArgumentType.greedyString())
                        .executes(TimezoneCommand::set));
    }
}
