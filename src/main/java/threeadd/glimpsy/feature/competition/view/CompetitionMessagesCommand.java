package threeadd.glimpsy.feature.competition.view;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.feature.competition.CompetitionManager;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.text.ComponentParser;

public class CompetitionMessagesCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        boolean newState = !CompetitionManager.isMessagesEnabled(player);
        CompetitionManager.setMessagesEnabled(player, newState);

        player.sendMessage(ComponentParser.parseRichString(
                "Competition messages are now " + (newState ? "<green>enabled" : "<red>disabled") + " for you"));

        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Toggle whether you personally receive competition start/end messages");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("competitionmessages")
                .executes(CompetitionMessagesCommand::run);
    }
}
