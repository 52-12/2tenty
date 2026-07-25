package threeadd.glimpsy.feature.recipes;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.inventory.ItemBuilder;
import threeadd.glimpsy.util.recipes.CustomRecipe;
import threeadd.glimpsy.util.text.ComponentParser;

public class SaltRecipe extends CustomRecipe {

    // Quartz reused as Salt's item/texture - tagged so it can be told apart from real Quartz
    // and blocked from vanilla quartz recipes (see SaltListener).
    public static final NamespacedKey SALT_KEY = new NamespacedKey(Glimpsy.getInstance(), "salt");

    public static ItemStack createSaltItem(int amount) {
        return new ItemBuilder(Material.QUARTZ)
                .withAmount(amount)
                .withName(Component.text("Salt"))
                .withItemMeta(meta -> meta.getPersistentDataContainer().set(SALT_KEY, PersistentDataType.BOOLEAN, true))
                .build();
    }

    public static boolean isSalt(ItemStack item) {
        if (item.getType() != Material.QUARTZ) return false;

        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(SALT_KEY, PersistentDataType.BOOLEAN);
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Smelt a water bucket to get Salt - your bucket comes back empty");
    }

    @Override
    public Recipe createRecipe() {
        return new FurnaceRecipe(new NamespacedKey(Glimpsy.getInstance(), "furnace_salt"),
                createSaltItem(2),
                Material.WATER_BUCKET,
                50,
                200);
    }
}
