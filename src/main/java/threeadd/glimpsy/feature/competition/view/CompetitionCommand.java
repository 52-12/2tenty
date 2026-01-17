package threeadd.glimpsy.feature.competition.view;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.List;

public class CompetitionCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        player.openInventory(new CompetitionInfoInventory(player).getInventory());
        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Used to view all information about competitions");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("competitions")
                .executes(CompetitionCommand::run);
    }

    @Override
    public List<String> getAliases() {
        return List.of("comp");
    }
}
