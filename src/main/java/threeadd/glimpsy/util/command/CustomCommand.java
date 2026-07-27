package threeadd.glimpsy.util.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import threeadd.glimpsy.util.Documentable;
import threeadd.glimpsy.util.config.Messages;

import java.util.List;

public abstract class CustomCommand implements Documentable {

    protected static final int SUCCESS = Command.SINGLE_SUCCESS;

    protected static Player getPlayer(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendRichMessage(
                    Messages.get("misc.player-only", "<error>You need to be a player for this command"));
            return null;
        }

        return player;
    }

    public abstract LiteralArgumentBuilder<CommandSourceStack> create();

    public List<String> getAliases() {
        return List.of();
    }

    private String permissionNode;

    /**
     * Auto-derived "2tenty.command.[name]" node gating this command (and its aliases).
     * Registered with {@code PermissionDefault.TRUE} at startup (see Glimpsy's command
     * registration), so everyone has it until a permissions plugin explicitly revokes it -
     * staff-only commands additionally keep their own stricter {@code .requires(...)} on top.
     */
    public final String getPermissionNode() {
        if (permissionNode == null)
            permissionNode = "2tenty.command." + create().getLiteral();
        return permissionNode;
    }

    /**
     * Bukkit default for {@link #getPermissionNode()}: TRUE for player commands (everyone has
     * them until a permissions plugin revokes them), OP for staff commands. Staff commands whose
     * own .requires() node is the same string as the auto node MUST override this to OP, or the
     * blanket TRUE registration would silently open them to everyone.
     */
    public PermissionDefault getPermissionDefault() {
        return PermissionDefault.TRUE;
    }

    /**
     * Whether this command should show up in the /help command list for the given viewer.
     * Follows the same auto node that gates execution, so a revoked command also disappears
     * from /help. Staff-only commands override this with their own stricter node.
     */
    public boolean isVisibleTo(Player viewer) {
        return viewer.hasPermission(getPermissionNode());
    }
}
