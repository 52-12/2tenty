package threeadd.glimpsy.feature.recipes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.inventory.ItemBuilder;
import threeadd.glimpsy.util.recipes.CustomRecipe;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.ArrayList;
import java.util.List;

// /recipes display only - the real crafting (requiring a full 64-stack in each of the 8
// surrounding slots, consumed whole, weighted-by-quantity material choice) is handled entirely
// by BridgeEggListener's PrepareItemCraftEvent/click-interception logic, since a real Recipe
// can't express any of that. isRegistered() is false so Bukkit never actually adds this one.
public class BridgeEggRecipe extends CustomRecipe {

    private static final List<Material> BLOCK_POOL = List.of(
            Material.COBBLESTONE, Material.DIRT, Material.OAK_PLANKS,
            Material.STONE_BRICKS, Material.SAND);

    // Each of the 8 surrounding slots gets its own rotation of the same pool, so at any given
    // moment they show different blocks from each other instead of all changing in lockstep -
    // demonstrating that the 8 slots don't all have to match, not just cycling one shared choice.
    private static RecipeChoice rotatedBlockChoice(int rotation) {
        int size = BLOCK_POOL.size();
        List<ItemStack> items = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
            items.add(new ItemStack(BLOCK_POOL.get((i + rotation) % size), 64));

        return new RecipeChoice.ExactChoice(items);
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Throw to bridge across gaps with the blocks used to craft it");
    }

    @Override
    public CraftingRecipe createRecipe() {
        ItemStack result = new ItemBuilder(Material.EGG)
                .withName(Component.text("Bridge Egg").color(NamedTextColor.WHITE))
                .build();

        return new ShapedRecipe(new NamespacedKey(Glimpsy.getInstance(), "bridge_egg_display"), result)
                .shape("ABC", "DEF", "GHI")
                .setIngredient('A', rotatedBlockChoice(0))
                .setIngredient('B', rotatedBlockChoice(1))
                .setIngredient('C', rotatedBlockChoice(2))
                .setIngredient('D', rotatedBlockChoice(3))
                .setIngredient('E', Material.EGG)
                .setIngredient('F', rotatedBlockChoice(4))
                .setIngredient('G', rotatedBlockChoice(1))
                .setIngredient('H', rotatedBlockChoice(3))
                .setIngredient('I', rotatedBlockChoice(0));
    }

    @Override
    public boolean isRegistered() {
        return false;
    }
}
