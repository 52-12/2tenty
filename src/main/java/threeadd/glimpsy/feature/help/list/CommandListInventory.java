package threeadd.glimpsy.feature.help.list;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import threeadd.glimpsy.feature.help.HelpListInventory;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.command.CustomCommandRegistry;
import threeadd.glimpsy.util.inventory.ItemBuilder;
import threeadd.glimpsy.util.text.ComponentParser;

public class CommandListInventory extends SubHelpListInventory<CustomCommand> {

    public CommandListInventory(Player viewer) {
        super(Component.text("Custom Commands"), CustomCommandRegistry.INSTANCE.getRegisteredItems(), viewer);

        setTransferSlot(Rows.SIX.getTotalSlots() - 9,
                new ItemBuilder(Material.RED_DYE)
                        .withName(Component.text("Return")
                                .color(TextColor.color(0xDB5858)))
                        .build(),
                () -> new HelpListInventory(getViewer()));
    }

    @Override
    protected @NotNull ItemStack mapToItem(@NotNull CustomCommand command) {
        LiteralCommandNode<CommandSourceStack> node = command.create().build();

        return new ItemBuilder(Material.COMMAND_BLOCK)
                .withName(ComponentParser.parseRichString("/" + node.getLiteral()))
                .withLore(command.createUsage())
                .build();
    }
}
