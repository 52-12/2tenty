package threeadd.glimpsy.feature.simple;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.text.ComponentParser;

public class FishImmuneCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        boolean newState = !PlayerFishingListener.isImmune(player);
        PlayerFishingListener.setImmune(player, newState);

        player.sendMessage(ComponentParser.parseRichString(
                "You are now " + (newState ? "<green>immune to" : "<red>vulnerable to") + " being fished up"));

        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Toggle whether you can be randomly fished up and teleported by other players");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("fishimmune")
                .requires(source -> source.getSender().hasPermission("2tenty.command.fishimmune"))
                .executes(FishImmuneCommand::run);
    }

    @Override
    public boolean isVisibleTo(Player viewer) {
        return viewer.hasPermission("2tenty.command.fishimmune");
    }
}
