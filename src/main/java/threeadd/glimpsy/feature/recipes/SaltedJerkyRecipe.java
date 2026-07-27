package threeadd.glimpsy.feature.recipes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.inventory.ItemBuilder;
import threeadd.glimpsy.util.recipes.CustomRecipe;
import threeadd.glimpsy.util.text.ComponentParser;

public class SaltedJerkyRecipe extends CustomRecipe {

    public static final NamespacedKey KEY = new NamespacedKey(Glimpsy.getInstance(), "salted_jerky");

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Combine Salt and Rotten Flesh to make Salted Jerky");
    }

    @Override
    public Recipe createRecipe() {
        ItemStack jerky = new ItemBuilder(Material.COOKED_RABBIT)
                .withName(Component.text("Salted Jerky").color(NamedTextColor.WHITE))
                .build();

        // ExactChoice so the ingredient is the actual named Salt item (which is also what
        // /recipes displays), not any white concrete powder.
        return new ShapelessRecipe(KEY, jerky)
                .addIngredient(new RecipeChoice.ExactChoice(SaltRecipe.createSaltItem(1)))
                .addIngredient(Material.ROTTEN_FLESH);
    }
}
