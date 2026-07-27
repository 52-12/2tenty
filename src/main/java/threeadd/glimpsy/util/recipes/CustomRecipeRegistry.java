package threeadd.glimpsy.util.recipes;

import org.bukkit.NamespacedKey;
import threeadd.glimpsy.feature.recipes.*;
import threeadd.glimpsy.feature.tradechest.TradeChestRecipe;
import threeadd.glimpsy.util.registry.Registry;

import java.util.List;

public class CustomRecipeRegistry extends Registry<CustomRecipe> {

    public final static CustomRecipeRegistry INSTANCE = new CustomRecipeRegistry();

    private CustomRecipeRegistry() {
        register(new BundleRecipe(),
                new StringRecipe(),
                new GoldHelmetRecipe(),
                new GoldChestPlateRecipe(),
                new GoldLeggingsRecipe(),
                new GoldBootsRecipe(),
                new IronHelmetRecipe(),
                new IronChestPlateRecipe(),
                new IronLeggingsRecipe(),
                new IronBootsRecipe(),
                new GoldHelmetBlastingRecipe(),
                new GoldChestPlateBlastingRecipe(),
                new GoldLeggingsBlastingRecipe(),
                new GoldBootsBlastingRecipe(),
                new IronHelmetBlastingRecipe(),
                new IronChestPlateBlastingRecipe(),
                new IronLeggingsBlastingRecipe(),
                new IronBootsBlastingRecipe(),
                new HopperRecipe(),
                new SaltRecipe(),
                new SaltedJerkyRecipe(),
                new SpongeRecipe(),
                new MyceliumRecipe(),
                new DirtFromMyceliumRecipe(),
                new DeepslateRecipe(),
                new CobblestoneFromDeepslateRecipe(),
                new BridgeEggRecipe(),
                new TradeChestRecipe());
    }

    public List<NamespacedKey> getRemovedRecipes() {
        return List.of(
                NamespacedKey.minecraft("bundle"),
                NamespacedKey.minecraft("hopper"),
                NamespacedKey.minecraft("sponge")
        );
    }
}
