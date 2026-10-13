package threeadd.glimpsy.feature.simple;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.text.ComponentParser;

public class ProximityChatCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        boolean nowEnabled = !ProximityChat.isEnabledFor(player);
        ProximityChat.setEnabled(player, nowEnabled);

        player.sendMessage(ComponentParser.parseRichString(nowEnabled
                ? "<green>Proximity chat on - distant chat fades toward gray, and @mentions stay highlighted"
                : "<red>Proximity chat off - chat is shown at full color again"));

        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Toggle proximity chat for yourself: distant messages fade, your @mentions stay bright");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("proximitychat")
                .executes(ProximityChatCommand::run);
    }
}
