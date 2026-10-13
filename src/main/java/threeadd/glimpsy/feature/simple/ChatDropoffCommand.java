package threeadd.glimpsy.feature.simple;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.text.ComponentParser;

public class ChatDropoffCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        int distance = IntegerArgumentType.getInteger(ctx, "distance");
        ChatDropoffManager.setCutoff(player, distance);

        player.sendMessage(distance <= 0
                ? Messages.msg("chat-dropoff.disabled", "<success>Chat range limit removed - you'll see all chat again")
                : Messages.msg("chat-dropoff.set", "<success>You'll now only see chat from within {distance} blocks",
                        "distance", String.valueOf(distance)));
        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Only see chat from within a block range (0 to see all)");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("chatdropoff")
                .then(Commands.argument("distance", IntegerArgumentType.integer(0))
                        .executes(ChatDropoffCommand::run));
    }
}
