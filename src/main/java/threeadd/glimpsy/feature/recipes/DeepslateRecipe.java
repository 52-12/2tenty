package threeadd.glimpsy.feature.recipes;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.recipes.CustomRecipe;
import threeadd.glimpsy.util.text.ComponentParser;

public class DeepslateRecipe extends CustomRecipe {

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Craft 9 cobblestone into 1 deepslate");
    }

    @Override
    public CraftingRecipe createRecipe() {
        return new ShapedRecipe(new NamespacedKey(Glimpsy.getInstance(), "deepslate"), ItemStack.of(Material.DEEPSLATE))
                .shape("CCC", "CCC", "CCC")
                .setIngredient('C', Material.COBBLESTONE);
    }
}
