package threeadd.glimpsy.feature.simple;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import threeadd.glimpsy.util.inventory.ItemBuilder;
import threeadd.glimpsy.util.text.ComponentParser;

public class ChatShareListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncChatEvent event) {
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Player player = event.getPlayer();

        if (message.equalsIgnoreCase("[inv]")) {
            event.setCancelled(true);
            shareInventory(player);
            return;
        }

        if (message.equalsIgnoreCase("[item]") || message.equalsIgnoreCase("[i]")) {
            event.setCancelled(true);
            shareItem(player);
        }
    }

    private void shareItem(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.isEmpty()) {
            player.sendMessage(ComponentParser.parseRichString("You're not holding anything"));
            return;
        }

        int amount = player.getInventory().all(item.getType()).values().stream()
                .mapToInt(ItemStack::getAmount)
                .sum();

        Component message = Component.text(player.getName() + ": ")
                .append(Component.text("[", NamedTextColor.GRAY))
                .append(Component.text(amount + "x ", NamedTextColor.AQUA))
                .append(item.effectiveName())
                .append(Component.text("]", NamedTextColor.GRAY))
                .hoverEvent(item.asHoverEvent(op -> op));

        Bukkit.broadcast(message);
    }

    private void shareInventory(Player player) {
        ItemStack[] contents = player.getInventory().getContents().clone();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) contents[i] = contents[i].clone();
        }

        ItemStack[] armor = {
                player.getInventory().getHelmet(),
                player.getInventory().getChestplate(),
                player.getInventory().getLeggings(),
                player.getInventory().getBoots()
        };
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null) armor[i] = armor[i].clone();
        }

        ItemStack offhand = player.getInventory().getItemInOffHand();
        offhand = offhand.isEmpty() ? null : offhand.clone();

        ItemStack finalOffhand = offhand;
        ItemStack preview = new ItemBuilder(Material.BUNDLE)
                .withName(Component.text("Inventory Preview"))
                .withItemMeta(BundleMeta.class, meta -> {
                    for (ItemStack item : contents) {
                        if (item == null || item.isEmpty()) continue;
                        meta.addItem(item);
                    }
                    for (ItemStack item : armor) {
                        if (item == null || item.isEmpty()) continue;
                        meta.addItem(item);
                    }
                    if (finalOffhand != null) meta.addItem(finalOffhand);
                })
                .build();

        Component message = Component.text(player.getName() + ": ")
                .append(Component.text("[", NamedTextColor.GRAY))
                .append(Component.text("Inventory Snapshot", NamedTextColor.YELLOW))
                .append(Component.text("]", NamedTextColor.GRAY))
                .hoverEvent(preview.asHoverEvent(op -> op))
                .clickEvent(ClickEvent.callback(audience -> {
                    if (audience instanceof Player viewer)
                        viewer.openInventory(new InventorySnapshotInventory(player.getName(), contents, armor, finalOffhand).getInventory());
                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).build()));

        Bukkit.broadcast(message);
    }
}
