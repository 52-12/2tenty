package threeadd.glimpsy.feature.teams;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.text.ComponentParser;

public class ShieldSkinCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int usage(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().getSender().sendMessage(ComponentParser.parseRichString(
                "Usage: /shieldskin <color> [pattern color]... e.g. /shieldskin red cross white - " +
                        "see /shieldskin colors and /shieldskin patterns for valid options"));
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int set(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        String raw = StringArgumentType.getString(ctx, "design");

        ShieldSkinDesignParser.Result parsed = ShieldSkinDesignParser.parse(raw);
        if (!parsed.valid()) {
            player.sendMessage(ComponentParser.parseRichString("<red>" + parsed.errorMessage()));
            return SUCCESS;
        }

        TeamManager.Result result = TeamManager.setShieldSkin(player, parsed.patterns());
        player.sendMessage(ComponentParser.parseRichString((result.success() ? "<green>" : "<red>") + result.message()));
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int clear(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        TeamManager.Result result = TeamManager.clearShieldSkin(player);
        player.sendMessage(ComponentParser.parseRichString((result.success() ? "<green>" : "<red>") + result.message()));
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int listColors(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().getSender().sendMessage(ComponentParser.parseRichString("Colors: " + ShieldSkinDesignParser.listColors()));
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int listPatterns(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().getSender().sendMessage(ComponentParser.parseRichString("Patterns: " + ShieldSkinDesignParser.listPatterns()));
        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString(
                "Set your team's shared shield design by typing a base color and pattern/color pairs (owner/trusted only)");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("shieldskin")
                .executes(ShieldSkinCommand::usage)
                .then(Commands.argument("design", StringArgumentType.greedyString())
                        .executes(ShieldSkinCommand::set))
                .then(Commands.literal("clear")
                        .executes(ShieldSkinCommand::clear))
                .then(Commands.literal("colors")
                        .executes(ShieldSkinCommand::listColors))
                .then(Commands.literal("patterns")
                        .executes(ShieldSkinCommand::listPatterns));
    }
}
