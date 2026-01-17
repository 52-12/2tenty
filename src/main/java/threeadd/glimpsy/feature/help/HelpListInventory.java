package threeadd.glimpsy.feature.help;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import threeadd.glimpsy.feature.help.list.ArticleListInventory;
import threeadd.glimpsy.feature.help.list.CommandListInventory;
import threeadd.glimpsy.feature.help.list.EnchantmentListInventory;
import threeadd.glimpsy.feature.help.list.RecipeListInventory;
import threeadd.glimpsy.util.inventory.CustomInventory;
import threeadd.glimpsy.util.inventory.ItemBuilder;

public class HelpListInventory extends CustomInventory {

    public HelpListInventory(Player viewer) {
        super(Rows.THREE, Component.text("Help Categories"), viewer);

        int index = 11;
        for (HelpCategory category : HelpCategory.values()) {
            setTransferSlot(index,
                    new ItemBuilder(category.getItem())
                            .withName(category.getTitle())
                            .withLore(category.getDescription())
                            .build(),
                    () -> getInventoryList(category));
            index++;
        }
    }

    private CustomInventory getInventoryList(HelpCategory category) {
        return switch (category) {
            case COMMANDS -> new CommandListInventory(getViewer());
            case ARTICLES -> new ArticleListInventory(getViewer());
            case ENCHANTMENTS -> new EnchantmentListInventory(getViewer());
            case RECIPES -> new RecipeListInventory(getViewer());
        };
    }
}
