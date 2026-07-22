package threeadd.glimpsy.feature.help;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import threeadd.glimpsy.util.text.ComponentParser;

public enum HelpCategory {
    COMMANDS(ComponentParser.parseRichString("Commands"),
            ComponentParser.parseRichString("View a list of 2tenty's custom commands"),
            ItemStack.of(Material.COMMAND_BLOCK)),
    ENCHANTMENTS(ComponentParser.parseRichString("Enchantments"),
            ComponentParser.parseRichString("View a list of 2tenty's custom enchantments"),
            ItemStack.of(Material.ENCHANTED_BOOK)),
    RECIPES(ComponentParser.parseRichString("Recipes"),
            ComponentParser.parseRichString("View a list of 2tenty's custom recipes"),
            ItemStack.of(Material.WRITABLE_BOOK)),
    ARTICLES(ComponentParser.parseRichString("Articles"),
            ComponentParser.parseRichString("View a list of 2tenty's community written articles"),
            ItemStack.of(Material.BOOK));

    private final Component title;
    private final Component description;
    private final ItemStack item;

    HelpCategory(Component title, Component description, ItemStack item) {
        this.title = title;
        this.description = description;
        this.item = item;
    }

    public Component getTitle() {
        return title;
    }

    public Component getDescription() {
        return description;
    }

    public ItemStack getItem() {
        return item;
    }
}
