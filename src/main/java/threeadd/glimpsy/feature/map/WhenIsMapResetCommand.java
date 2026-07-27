package threeadd.glimpsy.feature.map;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.text.ComponentParser;

public class WhenIsMapResetCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        Component message;
        if (!MapResetManager.isAutoEnabled()) {
            message = Messages.msg("map-reset.when-disabled", "<muted>Automatic map resets are turned off");
        } else if (MapResetManager.isPaused()) {
            message = Messages.msg("map-reset.when-paused",
                    "<primary>The map reset countdown is paused at <white>{time}",
                    "time", MapResetManager.formatLong());
        } else {
            message = Messages.msg("map-reset.when-active",
                    "<primary>The map resets in <white>{time}",
                    "time", MapResetManager.formatLong());
        }

        ctx.getSource().getSender().sendMessage(message);
        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Shows how long until the next automatic map reset");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("whenismapreset")
                .executes(WhenIsMapResetCommand::run);
    }
}
