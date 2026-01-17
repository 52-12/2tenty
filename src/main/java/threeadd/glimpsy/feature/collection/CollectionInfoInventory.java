package threeadd.glimpsy.feature.collection;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.util.inventory.ItemBuilder;
import threeadd.glimpsy.util.inventory.ListInventory;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.Map;
import java.util.function.Consumer;

public class CollectionInfoInventory extends ListInventory<Map.Entry<Material, Boolean>> {

    public CollectionInfoInventory(Player player) {
        super(Rows.FIVE, Component.text("Collections"),
                CollectionManager.getCollectionInfo(player).getStates().entrySet(),
                Rows.FIVE.getInnerSlots(),
                player);
    }

    @Override
    protected @NotNull ItemStack mapToItem(@NotNull Map.Entry<Material, Boolean> entry) {
        return new ItemBuilder(entry.getKey())
                .withLore(ComponentParser.parseRichString("Collected: ")
                        .append(ComponentParser.parseBoolean(entry.getValue())))
                .build();
    }

    @Override
    protected @Nullable Consumer<InventoryClickEvent> mapClick(@NotNull Map.Entry<Material, Boolean> entry) {
        return event -> {

            Player player = (Player) event.getWhoClicked();
            Material material = entry.getKey();
            boolean hasCollected = entry.getValue();

            if (hasCollected) {
                player.sendMessage(ComponentParser.parseRichString("You've already collected " + material));
                return;
            }

            if (!player.getInventory().contains(material)) {
                player.sendMessage(ComponentParser.parseRichString("You don't have " + material));
                return;
            }

            CollectionManager.getCollectionInfo(player).collect(material);
            player.sendMessage(ComponentParser.parseRichString("You have collected " + material));

            updateInventory();
        };
    }
}
