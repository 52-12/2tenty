package threeadd.glimpsy.feature.help;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.text.ComponentParser;

public class HelpCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        player.openInventory(new HelpListInventory(player).getInventory());

        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Get a list of all server features and general information");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("help")
                .executes(HelpCommand::run);
    }
}
