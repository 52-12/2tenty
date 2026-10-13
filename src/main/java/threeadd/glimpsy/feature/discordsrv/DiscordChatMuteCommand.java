package threeadd.glimpsy.feature.discordsrv;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.text.ComponentParser;

public class DiscordChatMuteCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        boolean newState = !DiscordChatMuteManager.isMuted(player);
        DiscordChatMuteManager.setMuted(player, newState);

        player.sendMessage(ComponentParser.parseRichString(newState
                ? "<red>Discord chat muted - you won't see Discord messages here, and your chat won't be sent to Discord"
                : "<green>Discord chat unmuted - Discord messages show here and your chat goes to Discord again"));

        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Toggle Discord chat both ways for yourself: hide Discord messages here, and stop your own messages being sent to Discord");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("discordchatmute")
                .executes(DiscordChatMuteCommand::run);
    }
}
