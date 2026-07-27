package threeadd.glimpsy.feature.simple;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.text.ComponentParser;

public class HatCommand extends CustomCommand {

    @SuppressWarnings("SameReturnValue")
    private static int run(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null) return SUCCESS;

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null) helmet = ItemStack.of(Material.AIR);

        if (helmet.isEmpty() && heldItem.isEmpty()) {
            player.sendMessage(ComponentParser.parseRichString("You have nothing to swap hats with"));
            return SUCCESS;
        }

        if (player.getCooldown(helmet.getType()) > 0 || player.getCooldown(heldItem.getType()) > 0) {
            player.sendMessage(ComponentParser.parseRichString("Slow down with the hats"));
            return SUCCESS;
        }

        ItemStack newHelmet = heldItem.clone();
        ItemStack newHeldItem = helmet.clone();

        int cooldownTicks = ConfigManager.getInt("hat.cooldown-ticks", 20);
        player.setCooldown(heldItem.getType(), cooldownTicks);
        player.setCooldown(helmet.getType(), cooldownTicks);

        player.getInventory().setHelmet(newHelmet);
        player.getInventory().setItemInMainHand(newHeldItem);

        player.playSound(player, Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0F, 1.0F);
        return SUCCESS;
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Swap the item in your hand with the helmet you're wearing");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("hat")
                .executes(HatCommand::run);
    }
}
