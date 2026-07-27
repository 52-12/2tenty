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

public class GoldHelmetRecipe extends CustomRecipe {

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString(
                "Smelt gold armor into gold nuggets:\n" +
                        "helmet: 3\n" +
                        "chestplate: 7\n" +
                        "leggings: 5\n" +
                        "boots: 2\n" +
                        "Blast furnaces work too and are twice as fast.");
    }

    @Override
    public Recipe createRecipe() {
        return new FurnaceRecipe(new NamespacedKey(Glimpsy.getInstance(), "furnace_gold_helmet"),
                ItemStack.of(Material.GOLD_NUGGET, 3),
                Material.GOLDEN_HELMET,
                50,
                300);
    }
}
