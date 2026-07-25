package threeadd.glimpsy.feature.nickname;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.text.ComponentParser;

public class ColorNickCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int set(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        String rawName = StringArgumentType.getString(ctx, "name");

        NicknameValidator.Result result = NicknameValidator.validate(rawName, player.getName());
        if (!result.valid()) {
            player.sendMessage(ComponentParser.parseRichString("<red>" + result.errorMessage()));
            return SUCCESS;
        }

        NicknameManager.setNickname(player, rawName);
        player.sendMessage(Component.text("Your name is now: ").append(result.parsed()));

        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int reset(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        NicknameManager.clearNickname(player);
        player.sendMessage(ComponentParser.parseRichString("Your name color was reset"));

        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Color your username with MiniMessage tags (max 24 characters, must contain your real username)");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("colornick")
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(ColorNickCommand::set))
                .then(Commands.literal("reset")
                        .executes(ColorNickCommand::reset));
    }
}
