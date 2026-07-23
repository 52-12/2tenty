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

public class HopperRecipe extends CustomRecipe {

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Hopper recipe was cheapened to use Iron Nuggets instead of Iron Ingots");
    }

    @Override
    public CraftingRecipe createRecipe() {
        return new ShapedRecipe(new NamespacedKey(Glimpsy.getInstance(), "hopper"), ItemStack.of(Material.HOPPER))
                .shape("N N", "NCN", " N ")
                .setIngredient('N', Material.IRON_NUGGET)
                .setIngredient('C', Material.CHEST);
    }
}
