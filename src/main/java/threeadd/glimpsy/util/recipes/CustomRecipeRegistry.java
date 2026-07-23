package threeadd.glimpsy.util.recipes;

import org.bukkit.NamespacedKey;
import threeadd.glimpsy.feature.recipes.*;
import threeadd.glimpsy.util.registry.Registry;

import java.util.List;

public class CustomRecipeRegistry extends Registry<CustomRecipe> {

    public final static CustomRecipeRegistry INSTANCE = new CustomRecipeRegistry();

    private CustomRecipeRegistry() {
        register(new BundleRecipe(),
                new LeatherRecipe(),
                new StringRecipe(),
                new GoldHelmetRecipe(),
                new GoldChestPlateRecipe(),
                new GoldLeggingsRecipe(),
                new GoldBootsRecipe(),
                new HopperRecipe());
    }

    public List<NamespacedKey> getRemovedRecipes() {
        return List.of(
                NamespacedKey.minecraft("bundle"),
                NamespacedKey.minecraft("hopper")
        );
    }
}
