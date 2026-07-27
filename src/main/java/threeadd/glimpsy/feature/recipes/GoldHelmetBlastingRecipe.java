package threeadd.glimpsy.feature.recipes;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.recipes.CustomRecipe;
import threeadd.glimpsy.util.text.ComponentParser;

public class GoldHelmetBlastingRecipe extends CustomRecipe {

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Added for gold gathering - blast furnaces are twice as fast");
    }

    @Override
    public Recipe createRecipe() {
        return new BlastingRecipe(new NamespacedKey(Glimpsy.getInstance(), "blasting_gold_helmet"),
                ItemStack.of(Material.GOLD_NUGGET, 3),
                Material.GOLDEN_HELMET,
                50,
                150);
    }

    @Override
    public boolean isListed() {
        return false; // shown as part of GoldHelmetRecipe's /recipes entry instead
    }
}
