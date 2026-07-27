package threeadd.glimpsy.feature.recipes;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.recipes.CustomRecipe;
import threeadd.glimpsy.util.text.ComponentParser;

public class IronLeggingsRecipe extends CustomRecipe {

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Added for iron gathering");
    }

    @Override
    public Recipe createRecipe() {
        return new FurnaceRecipe(new NamespacedKey(Glimpsy.getInstance(), "furnace_iron_leggings"),
                ItemStack.of(Material.IRON_NUGGET, 5),
                Material.IRON_LEGGINGS,
                50,
                300);
    }

    @Override
    public boolean isListed() {
        return false; // shown as part of IronHelmetRecipe's /recipes entry instead
    }
}
