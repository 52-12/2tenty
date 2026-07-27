package threeadd.glimpsy.feature.map;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.text.ComponentParser;

/**
 * Like /mapreset, but also clears every player's inventory - online players immediately, and
 * offline players when they next rejoin (see MapResetManager#recordFullReset). Also restarts
 * the automatic reset countdown.
 */
public class FullResetCommand extends CustomCommand {

    private static final String PERMISSION = "2tenty.command.fullreset";

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        if (!Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit")) {
            ctx.getSource().getSender().sendMessage(ComponentParser.parseRichString(
                    "Full reset requires the FastAsyncWorldEdit plugin, which isn't installed on this server"));
            return SUCCESS;
        }

        if (!MapSetter.reset(Integer.MAX_VALUE)) {
            ctx.getSource().getSender().sendMessage(ComponentParser.parseRichString(
                    "A map reset is already in progress, or no world border is set"));
            return SUCCESS;
        }

        for (Player player : Bukkit.getOnlinePlayers())
            player.getInventory().clear();

        MapResetManager.recordFullReset(); // clears offline players' inventories on rejoin
        MapResetManager.resetCountdown();

        ctx.getSource().getSender().sendMessage(Messages.msg("full-reset.done",
                "<success>Full reset started - the world and every player's inventory were wiped"));
        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Reset the world AND clear every player's inventory");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("fullreset")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .executes(FullResetCommand::run);
    }

    @Override
    public boolean isVisibleTo(Player viewer) {
        return viewer.hasPermission(PERMISSION);
    }

    @Override
    public PermissionDefault getPermissionDefault() {
        return PermissionDefault.OP;
    }
}
