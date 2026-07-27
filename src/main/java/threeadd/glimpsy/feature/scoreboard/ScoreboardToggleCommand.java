package threeadd.glimpsy.feature.scoreboard;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.text.ComponentParser;

public class ScoreboardToggleCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        boolean shown = ScoreboardManager.toggle(player);
        player.sendMessage(shown
                ? Messages.msg("scoreboard.shown", "<success>Scoreboard shown")
                : Messages.msg("scoreboard.hidden", "<error>Scoreboard hidden"));

        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Show or hide the sidebar scoreboard");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("togglescoreboard")
                .executes(ScoreboardToggleCommand::run);
    }
}
