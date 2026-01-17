package threeadd.glimpsy.feature.recipes;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.recipes.CustomRecipe;
import threeadd.glimpsy.util.text.ComponentParser;

public class StringRecipe extends CustomRecipe {

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("String recipe was added to allow players to craft wool and other way around");
    }

    @Override
    public CraftingRecipe createRecipe() {
        return new ShapelessRecipe(new NamespacedKey(Glimpsy.getInstance(), "string"), ItemStack.of(Material.STRING, 4))
                .addIngredient(new RecipeChoice.MaterialChoice(
                        Material.WHITE_WOOL,
                        Material.ORANGE_WOOL,
                        Material.MAGENTA_WOOL,
                        Material.LIGHT_BLUE_WOOL,
                        Material.YELLOW_WOOL,
                        Material.LIME_WOOL,
                        Material.PINK_WOOL,
                        Material.GRAY_WOOL,
                        Material.LIGHT_GRAY_WOOL,
                        Material.CYAN_WOOL,
                        Material.PURPLE_WOOL,
                        Material.BLUE_WOOL,
                        Material.BROWN_WOOL,
                        Material.GREEN_WOOL,
                        Material.RED_WOOL,
                        Material.BLACK_WOOL
                ));
    }

}

