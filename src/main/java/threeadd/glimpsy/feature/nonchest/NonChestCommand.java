package threeadd.glimpsy.feature.nonchest;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.List;

public class NonChestCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        player.openInventory(NonChestManager.getNonChest().getInventory());

        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("View the server's nonchest (publicly accessible chest)");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("nonchest")
                .executes(NonChestCommand::run);
    }

    @Override
    public List<String> getAliases() {
        return List.of("publicchest", "communitychest");
    }
}
