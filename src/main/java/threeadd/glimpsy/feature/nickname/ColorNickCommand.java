package threeadd.glimpsy.feature.nickname;

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

public class ColorNickCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int set(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        String rawName = StringArgumentType.getString(ctx, "name");

        NicknameValidator.Result result = NicknameValidator.validate(rawName, player.getName());
        if (!result.valid()) {
            player.sendMessage(ComponentParser.parseRichString(Messages.brand("<error>" + result.errorMessage())));
            return SUCCESS;
        }

        NicknameManager.setNickname(player, rawName);
        player.sendMessage(Messages.msg("nicknames.set", "Your name is now: ").append(result.parsed()));

        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int reset(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        NicknameManager.clearNickname(player);
        player.sendMessage(Messages.msg("nicknames.reset", "Your name color was reset"));

        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Colour your username with MiniMessage tags - the plain text must still be exactly your username");
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
