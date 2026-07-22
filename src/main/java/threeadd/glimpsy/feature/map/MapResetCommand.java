package threeadd.glimpsy.feature.map;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.text.ComponentParser;

public class MapResetCommand extends CustomCommand {

    private static int run(CommandContext<CommandSourceStack> ctx) {
        if (!Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit")) {
            ctx.getSource().getSender().sendMessage(ComponentParser.parseRichString(
                    "Map reset requires the FastAsyncWorldEdit plugin, which isn't installed on this server"));
            return SUCCESS;
        }

        if (!MapSetter.reset(Integer.MAX_VALUE)) {
            ctx.getSource().getSender().sendMessage(ComponentParser.parseRichString(
                    "A map reset is already in progress, or no world border is set"));
        }
        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Reset the map");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("mapreset")
                .requires(source -> source.getSender().hasPermission("2tenty.command.mapreset"))
                .executes(MapResetCommand::run);
    }
}
