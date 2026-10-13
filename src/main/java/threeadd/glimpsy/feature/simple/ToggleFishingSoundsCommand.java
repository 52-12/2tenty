package threeadd.glimpsy.feature.simple;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.text.ComponentParser;

public class ToggleFishingSoundsCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        boolean nowMuted = !FishingSounds.isMuted(player);
        FishingSounds.setMuted(player, nowMuted);

        player.sendMessage(ComponentParser.parseRichString(nowMuted
                ? "<green>Fishing sounds disabled - fishing bobber sounds will be hidden from you"
                : "<red>Fishing sounds enabled again"));

        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Toggle hearing other players' fishing catch sounds");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("togglefishingsounds")
                .executes(ToggleFishingSoundsCommand::run);
    }
}
