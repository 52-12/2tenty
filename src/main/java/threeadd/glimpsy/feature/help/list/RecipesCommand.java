package threeadd.glimpsy.feature.help.list;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.config.Messages;

public class RecipesCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        player.openInventory(new RecipeListInventory(player).getInventory());

        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return Messages.msg("help.category-recipes", "View a list of {name}'s custom recipes", "name", Messages.brandName());
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("recipes")
                .executes(RecipesCommand::run);
    }
}
