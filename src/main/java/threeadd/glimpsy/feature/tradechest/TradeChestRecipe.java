package threeadd.glimpsy.feature.tradechest;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.ShapedRecipe;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.recipes.CustomRecipe;
import threeadd.glimpsy.util.text.ComponentParser;

public class TradeChestRecipe extends CustomRecipe {

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString(
                "Surround a chest with 8 ender pearls to craft a Trade Chest - place it to trade items with other players remotely");
    }

    // The whole feature can be turned off - recipe registration happens once at startup, so
    // flipping trade-chest.enabled needs a restart to add/remove the recipe (the listeners
    // respect a /2tentyreload immediately)
    @Override
    public boolean isRegistered() {
        return ConfigManager.getBoolean("trade-chest.enabled", true);
    }

    @Override
    public boolean isListed() {
        return ConfigManager.getBoolean("trade-chest.enabled", true);
    }

    @Override
    public CraftingRecipe createRecipe() {
        return new ShapedRecipe(new NamespacedKey(Glimpsy.getInstance(), "trade_chest"), TradeChestItem.create())
                .shape("PPP", "PCP", "PPP")
                .setIngredient('P', Material.ENDER_PEARL)
                .setIngredient('C', Material.CHEST);
    }
}
