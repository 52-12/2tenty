package threeadd.glimpsy.feature.simple;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.List;

/**
 * Gives up your own respawn point without touching the bed it's set to. A respawn point is stored
 * on the player, not on the block, so clearing it leaves the bed standing and still usable -
 * anyone else can sleep in it and claim it for themselves, and the owner isn't forced to break
 * their own bed just to stop respawning there.
 */
public class ClearBedSpawnCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        Location respawn = player.getRespawnLocation();
        if (respawn == null) {
            player.sendMessage(Messages.msg("bed-spawn.none",
                    "<error>You don't have a bed spawn set"));
            return SUCCESS;
        }

        player.setRespawnLocation(null);
        player.sendMessage(Messages.msg("bed-spawn.cleared",
                "<success>Bed spawn cleared - you'll respawn at world spawn now. The bed at {x}, {y}, {z} is untouched",
                "x", String.valueOf(respawn.getBlockX()),
                "y", String.valueOf(respawn.getBlockY()),
                "z", String.valueOf(respawn.getBlockZ())));
        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString(
                "Clear your bed spawn without breaking the bed - it stays where it is for anyone else to claim");
    }

    @Override
    public List<String> getAliases() {
        return List.of("clearbed", "unsetbedspawn");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("clearbedspawn").executes(ClearBedSpawnCommand::run);
    }
}
