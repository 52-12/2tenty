package threeadd.glimpsy.feature.spawn;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.FinePositionResolver;
import io.papermc.paper.math.FinePosition;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.List;
import java.util.Locale;

@SuppressWarnings("UnstableApiUsage")
public class WorldSpawnCommand extends CustomCommand {

    private static final float NORTH_YAW = 180f;

    @SuppressWarnings("SameReturnValue")
    private static int runHere(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        apply(ctx, player.getLocation());
        return SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    private static int runAtPosition(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        FinePositionResolver resolver = ctx.getArgument("position", FinePositionResolver.class);
        FinePosition position = resolver.resolve(ctx.getSource());

        World world = ctx.getSource().getLocation().getWorld();
        apply(ctx, new Location(world, position.x(), position.y(), position.z()));

        return SUCCESS;
    }

    private static void apply(CommandContext<CommandSourceStack> ctx, Location location) {
        location.setYaw(NORTH_YAW); // spawn always faces north, regardless of which way it was set facing

        WorldSpawnManager.setCustomSpawn(location);
        ctx.getSource().getSender().sendMessage(successMessage(location));
    }

    private static Component successMessage(Location location) {
        return ComponentParser.parseRichString(
                "World spawn set to <#63C14F>" + formatCoords(location) +
                        "<reset> - new players and bed/anchor-less respawns will land there precisely, " +
                        "without snapping to the highest block");
    }

    private static String formatCoords(Location location) {
        return String.format(Locale.US, "%.2f %.2f %.2f", location.getX(), location.getY(), location.getZ());
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Set the world spawn (defaults to your position) to an exact location, no highest-block snapping");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("setworldspawn")
                .requires(source -> source.getSender().hasPermission("2tenty.command.setworldspawn"))
                .executes(WorldSpawnCommand::runHere)
                .then(Commands.argument("position", ArgumentTypes.finePosition())
                        .executes(WorldSpawnCommand::runAtPosition));
    }

    @Override
    public List<String> getAliases() {
        return List.of("setspawn");
    }

    @Override
    public boolean isVisibleTo(Player viewer) {
        return viewer.hasPermission("2tenty.command.setworldspawn");
    }

    // Same node string as the auto-derived one - must stay OP or the blanket TRUE
    // registration would open this staff command to everyone
    @Override
    public PermissionDefault getPermissionDefault() {
        return PermissionDefault.OP;
    }
}
