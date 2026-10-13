package threeadd.glimpsy.feature.headcollection;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.List;

public class HeadCollectionsCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        if (!HeadCollectionManager.enabled()) {
            player.sendMessage(Messages.msg("head-collection.disabled", "<error>Head collections are disabled"));
            return SUCCESS;
        }

        HeadCollectionManager.open(player);
        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString(
                "Open your head collection - a chest that only holds heads. Type [heads] in chat to show it off");
    }

    @Override
    public List<String> getAliases() {
        return List.of("headcollection", "heads");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("headcollections").executes(HeadCollectionsCommand::run);
    }
}
