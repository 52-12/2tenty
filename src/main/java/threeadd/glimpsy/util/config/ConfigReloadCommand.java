package threeadd.glimpsy.util.config;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.text.ComponentParser;

@SuppressWarnings("UnstableApiUsage")
public class ConfigReloadCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        ConfigManager.reload();
        Messages.reload();
        ctx.getSource().getSender().sendMessage(ComponentParser.parseRichString("<green>config.yml and messages.yml reloaded"));
        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Reload config.yml's gameplay-balance values without restarting the server");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("2tentyreload")
                .requires(source -> source.getSender().hasPermission("2tenty.command.reload"))
                .executes(ConfigReloadCommand::run);
    }

    @Override
    public boolean isVisibleTo(Player viewer) {
        return viewer.hasPermission("2tenty.command.reload");
    }

    @Override
    public PermissionDefault getPermissionDefault() {
        return PermissionDefault.OP;
    }
}
