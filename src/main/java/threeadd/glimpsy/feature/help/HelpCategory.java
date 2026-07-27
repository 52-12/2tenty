package threeadd.glimpsy.feature.help;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.text.ComponentParser;

public enum HelpCategory {
    COMMANDS("Commands", "help.category-commands", "View a list of {name}'s custom commands",
            Material.COMMAND_BLOCK),
    ENCHANTMENTS("Enchantments", "help.category-enchantments", "View a list of {name}'s custom enchantments",
            Material.ENCHANTED_BOOK),
    RECIPES("Recipes", "help.category-recipes", "View a list of {name}'s custom recipes",
            Material.WRITABLE_BOOK),
    ARTICLES("Articles", "help.category-articles", "View a list of {name}'s community written articles",
            Material.BOOK);

    private final String title;
    private final String descriptionKey;
    private final String descriptionDefault;
    private final ItemStack item;

    HelpCategory(String title, String descriptionKey, String descriptionDefault, Material item) {
        this.title = title;
        this.descriptionKey = descriptionKey;
        this.descriptionDefault = descriptionDefault;
        this.item = ItemStack.of(item);
    }

    public Component getTitle() {
        return ComponentParser.parseRichString(title);
    }

    // Read live (not cached at enum init) so a /2tentyreload rename takes effect immediately
    public Component getDescription() {
        return Messages.msg(descriptionKey, descriptionDefault, "name", Messages.brandName());
    }

    public ItemStack getItem() {
        return item;
    }
}
