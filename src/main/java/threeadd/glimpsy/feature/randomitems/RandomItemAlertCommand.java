package threeadd.glimpsy.feature.randomitems;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.text.ComponentParser;

public class RandomItemAlertCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        RandomItemsInfo info = RandomItemManager.getInfo(player);
        if (info == null) {
            player.sendMessage(ComponentParser.parseRichString("<red>Your item settings haven't loaded yet - try again in a moment"));
            return SUCCESS;
        }

        boolean newState = !info.isFullInventoryAlertEnabled();
        info.setFullInventoryAlertEnabled(newState);

        player.sendMessage(ComponentParser.parseRichString(
                "Full inventory alerts for gens are now " + (newState ? "<green>enabled" : "<red>disabled")));

        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Toggle the action bar warning when a gen can't give you an item because your inventory is full");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("toggleitemsalert")
                .executes(RandomItemAlertCommand::run);
    }
}
