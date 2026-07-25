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

public class CobblestoneFromDeepslateRecipe extends CustomRecipe {

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Craft 1 deepslate back into 9 cobblestone");
    }

    @Override
    public CraftingRecipe createRecipe() {
        return new ShapelessRecipe(new NamespacedKey(Glimpsy.getInstance(), "cobblestone_from_deepslate"), ItemStack.of(Material.COBBLESTONE, 9))
                .addIngredient(Material.DEEPSLATE);
    }
}
