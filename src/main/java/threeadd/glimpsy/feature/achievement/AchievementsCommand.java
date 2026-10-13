package threeadd.glimpsy.feature.achievement;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.List;

public class AchievementsCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        player.openInventory(new AchievementListInventory(player).getInventory());
        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("See the server's achievements and which ones you've earned");
    }

    @Override
    public List<String> getAliases() {
        return List.of("achievement");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("achievements").executes(AchievementsCommand::run);
    }
}
