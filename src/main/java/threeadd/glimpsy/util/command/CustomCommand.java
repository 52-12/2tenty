package threeadd.glimpsy.util.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.Documentable;

import java.util.List;

public abstract class CustomCommand implements Documentable {

    protected static final int SUCCESS = Command.SINGLE_SUCCESS;

    protected static Player getPlayer(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendRichMessage("<red>You need to be a player for this command");
            return null;
        }

        return player;
    }

    public abstract LiteralArgumentBuilder<CommandSourceStack> create();

    public List<String> getAliases() {
        return List.of();
    }
}
