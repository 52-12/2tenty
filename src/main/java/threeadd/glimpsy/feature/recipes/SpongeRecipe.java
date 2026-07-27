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

public class SpongeRecipe extends CustomRecipe {

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Wet Sponge smelts faster than vanilla");
    }

    @Override
    public Recipe createRecipe() {
        return new FurnaceRecipe(new NamespacedKey(Glimpsy.getInstance(), "furnace_sponge"),
                ItemStack.of(Material.SPONGE),
                Material.WET_SPONGE,
                15,
                100);
    }

    @Override
    public boolean isListed() {
        return false;
    }
}
