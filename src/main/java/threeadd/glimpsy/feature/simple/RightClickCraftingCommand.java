package threeadd.glimpsy.feature.simple;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.text.ComponentParser;

public class RightClickCraftingCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        boolean newState = InteractCraftingListener.toggle(player);
        player.sendMessage(newState
                ? Messages.msg("misc.rightclick-crafting-enabled", "Right click crafting is now <success>enabled")
                : Messages.msg("misc.rightclick-crafting-disabled", "Right click crafting is now <error>disabled"));

        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Toggle whether sneak-right-clicking with a crafting table in hand opens the crafting GUI for you");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("rightclickcrafting")
                .executes(RightClickCraftingCommand::run);
    }
}
