package threeadd.glimpsy.feature.antifilter;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.text.ComponentParser;

public class AntiDropCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        if (!AntiFilterManager.isEnabled()) {
            player.sendMessage(Messages.msg("antifilter.disabled", "<error>That feature is turned off"));
            return SUCCESS;
        }

        AntiFilterManager.open(player, AntiFilterType.DROP);
        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString(
                "Open your anti-drop chest - items whose type you place here can't be accidentally dropped");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("antidrop").executes(AntiDropCommand::run);
    }
}
