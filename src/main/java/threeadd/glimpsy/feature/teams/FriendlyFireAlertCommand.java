package threeadd.glimpsy.feature.teams;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.text.ComponentParser;

public class FriendlyFireAlertCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        boolean nowEnabled = FriendlyFireAlertManager.toggle(player);
        player.sendMessage(nowEnabled
                ? Messages.msg("teams.ff-alerts-enabled", "Friendly fire alerts are now <success>enabled")
                : Messages.msg("teams.ff-alerts-disabled", "Friendly fire alerts are now <error>disabled"));

        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Toggle the alert your whole team gets when a teammate or ally hits each other");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("togglefriendlyfirealerts")
                .executes(FriendlyFireAlertCommand::run);
    }
}
