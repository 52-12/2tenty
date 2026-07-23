package threeadd.glimpsy.feature.discord;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.text.ComponentParser;

public class DiscordMessagesCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        boolean newState = !DiscordManager.isMessagesEnabled(player);
        DiscordManager.setMessagesEnabled(player, newState);

        player.sendMessage(ComponentParser.parseRichString(
                "Discord announcements are now " + (newState ? "<green>enabled" : "<red>disabled") + " for you"));

        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Toggle whether you personally receive periodic Discord invite reminders");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("togglediscord")
                .executes(DiscordMessagesCommand::run);
    }
}
