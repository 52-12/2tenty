package threeadd.glimpsy.template;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.text.ComponentParser;

/**
 * TEMPLATE: a player command. Copy this into a feature package, rename it, then register it.
 * <p>
 * Commands extend {@link CustomCommand} and are built with Paper's Brigadier API. The two methods
 * you must fill in are {@link #createUsage()} (the /help description) and {@link #create()} (the
 * command tree - its name, arguments and what running it does).
 * <p>
 * Permissions are automatic: every command gets a {@code 2tenty.command.<name>} permission node
 * that's granted to everyone by default, so a permissions plugin (LuckPerms) can revoke it. For a
 * staff-only command, override {@link #getPermissionDefault()} to return {@code PermissionDefault.OP}
 * and add a {@code .requires(...)} check in {@link #create()} - see MapResetCommand for that.
 */
@SuppressWarnings("UnstableApiUsage")
public class ExampleCommand extends CustomCommand {

    // This runs when a player types just "/example".
    @SuppressWarnings("SameReturnValue")
    private static int greet(CommandContext<CommandSourceStack> ctx) {
        // getPlayer returns null (and tells the sender off) if the console ran the command.
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        player.sendMessage(Messages.msg("example.greeting", "<primary>Hello, {name}!", "name", player.getName()));
        return SUCCESS;
    }

    // This runs when a player types "/example say <text>". The <text> value is read below.
    @SuppressWarnings("SameReturnValue")
    private static int say(CommandContext<CommandSourceStack> ctx) {
        String text = StringArgumentType.getString(ctx, "text");
        ctx.getSource().getSender().sendMessage(ComponentParser.parseRichString("<yellow>You said: " + text));
        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("An example command - /example or /example say <text>");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("example")            // the command name: /example
                .executes(ExampleCommand::greet)       // what plain "/example" does
                .then(Commands.literal("say")          // a sub-command: /example say ...
                        .then(Commands.argument("text", StringArgumentType.greedyString()) // the <text> argument
                                .executes(ExampleCommand::say)));
    }

    // ---------------------------------------------------------------------------------------
    // HOW TO USE THIS:
    // 1. Copy this file into a feature package and rename the class + the "example" literal.
    // 2. Change greet/say (and the tree in create()) to do what you want.
    // 3. Open util/command/CustomCommandRegistry.java and add `new YourCommand()` to register(...).
    // 4. Rebuild and deploy. The command works in-game and shows in /help automatically.
    // ---------------------------------------------------------------------------------------
}
