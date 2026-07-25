package threeadd.glimpsy.feature.recipes;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.recipes.CustomRecipe;
import threeadd.glimpsy.util.text.ComponentParser;

public class DirtFromMyceliumRecipe extends CustomRecipe {

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Craft 1 mycelium back into 9 dirt");
    }

    @Override
    public CraftingRecipe createRecipe() {
        return new ShapelessRecipe(new NamespacedKey(Glimpsy.getInstance(), "dirt_from_mycelium"), ItemStack.of(Material.DIRT, 9))
                .addIngredient(Material.MYCELIUM);
    }
}
