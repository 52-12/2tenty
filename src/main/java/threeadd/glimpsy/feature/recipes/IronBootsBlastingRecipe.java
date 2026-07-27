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

public class IronBootsBlastingRecipe extends CustomRecipe {

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Added for iron gathering - blast furnaces are twice as fast");
    }

    @Override
    public Recipe createRecipe() {
        return new BlastingRecipe(new NamespacedKey(Glimpsy.getInstance(), "blasting_iron_boots"),
                ItemStack.of(Material.IRON_NUGGET, 2),
                Material.IRON_BOOTS,
                50,
                150);
    }

    @Override
    public boolean isListed() {
        return false; // shown as part of IronHelmetRecipe's /recipes entry instead
    }
}
