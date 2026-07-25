package threeadd.glimpsy.feature.simple;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.text.ComponentParser;

public class RightClickCraftingCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        boolean newState = !InteractCraftingListener.isEnabled();
        InteractCraftingListener.setEnabled(newState);

        ctx.getSource().getSender().sendMessage(ComponentParser.parseRichString(
                "Right click crafting is now " + (newState ? "<green>enabled" : "<red>disabled")));

        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Toggle whether right-clicking with a crafting table in hand opens the crafting GUI");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("rightclickcrafting")
                .requires(source -> source.getSender().hasPermission("2tenty.command.rightclickcrafting"))
                .executes(RightClickCraftingCommand::run);
    }

    @Override
    public boolean isVisibleTo(Player viewer) {
        return viewer.hasPermission("2tenty.command.rightclickcrafting");
    }
}
