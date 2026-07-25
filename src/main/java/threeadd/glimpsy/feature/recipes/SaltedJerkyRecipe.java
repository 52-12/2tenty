package threeadd.glimpsy.feature.recipes;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
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
        ItemStack jerky = new ItemBuilder(Material.COOKED_MUTTON)
                .withName(Component.text("Salted Jerky"))
                .build();

        return new ShapelessRecipe(KEY, jerky)
                .addIngredient(Material.QUARTZ)
                .addIngredient(Material.ROTTEN_FLESH);
    }
}
