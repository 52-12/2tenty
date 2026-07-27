package threeadd.glimpsy.feature.tradechest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.inventory.ItemBuilder;
import threeadd.glimpsy.util.text.ComponentParser;

public class TradeChestItem {

    public static final NamespacedKey KEY = new NamespacedKey(Glimpsy.getInstance(), "trade_chest");

    public static ItemStack create() {
        return new ItemBuilder(Material.ENDER_CHEST)
                .withName(Component.text("Trade Chest").color(NamedTextColor.WHITE))
                .withLore(ComponentParser.parseRichString(
                        "Place it down, load it with up to 25 item stacks, and send trade offers "
                                + "to other placed Trade Chests. Only one can be placed per player."))
                .withItemMeta(meta -> meta.getPersistentDataContainer().set(KEY, PersistentDataType.BOOLEAN, true))
                .build();
    }

    public static boolean isTradeChest(ItemStack item) {
        if (item == null || item.getType() != Material.ENDER_CHEST) return false;

        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(KEY, PersistentDataType.BOOLEAN);
    }
}
