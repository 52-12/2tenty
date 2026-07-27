package threeadd.glimpsy.feature.teams;

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

import java.util.List;

public class TeamChatCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int send(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        String message = StringArgumentType.getString(ctx, "message");

        if (!TeamChatManager.send(player, message))
            player.sendMessage(Messages.msg("teams.not-on-team-error", "<error>You aren't on a team"));

        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int toggle(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        boolean nowEnabled = TeamChatManager.toggle(player);
        player.sendMessage(nowEnabled
                ? Messages.msg("teams.chat-enabled", "<success>Team chat enabled - your messages now go to your team only")
                : Messages.msg("teams.chat-disabled", "<error>Team chat disabled - your messages go to public chat again"));

        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Send a message to your team, or /teamchat toggle to redirect all your chat there");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("teamchat")
                .then(Commands.literal("toggle")
                        .executes(TeamChatCommand::toggle))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(TeamChatCommand::send));
    }

    @Override
    public List<String> getAliases() {
        return List.of("tc");
    }
}
