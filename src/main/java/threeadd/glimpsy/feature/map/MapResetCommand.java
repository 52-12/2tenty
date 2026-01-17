package threeadd.glimpsy.feature.map;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.text.ComponentParser;

public class MapResetCommand extends CustomCommand {

    private static int run(CommandContext<CommandSourceStack> ctx) {
        MapSetter.reset(Integer.MAX_VALUE);
        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Reset the map");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("mapreset")
                .requires(source -> source.getSender().hasPermission("glimpsy.command.mapreset"))
                .executes(MapResetCommand::run);
    }
}
