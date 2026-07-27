package threeadd.glimpsy.template;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.recipes.CustomRecipe;
import threeadd.glimpsy.util.text.ComponentParser;

/**
 * TEMPLATE: a custom crafting recipe. Copy this into {@code feature/recipes}, rename it, change
 * the recipe, then register it (see the bottom of this file).
 * <p>
 * A recipe class extends {@link CustomRecipe} and only really has to answer two questions:
 * <ol>
 *     <li>{@link #createRecipe()} - what the recipe actually is (shape + ingredients + result).</li>
 *     <li>{@link #createUsage()} - the one-line description shown in the in-game {@code /recipes} menu.</li>
 * </ol>
 * This example is a "shaped" recipe (the ingredients must be in a specific pattern). There are
 * also {@code ShapelessRecipe} (ingredients anywhere) and {@code FurnaceRecipe} (smelting) - the
 * real classes in {@code feature/recipes} have examples of all three.
 */
public class ExampleRecipe extends CustomRecipe {

    @Override
    public Recipe createRecipe() {
        // The result players get when they craft this. ItemStack.of(material, amount).
        ItemStack result = ItemStack.of(Material.DIAMOND, 1);

        // Every custom recipe needs a unique NamespacedKey. Keep the string lowercase with
        // underscores and make sure no other recipe uses the same one.
        NamespacedKey key = new NamespacedKey(Glimpsy.getInstance(), "example_diamond");

        // A shaped recipe is a 3x3 grid. Each string is one row; each character is a slot.
        // A space means "empty slot". setIngredient maps a character to a material.
        return new ShapedRecipe(key, result)
                .shape(
                        "GGG",
                        "G G",
                        "GGG")
                .setIngredient('G', Material.GOLD_INGOT);
    }

    @Override
    public Component createUsage() {
        // ComponentParser.parseRichString lets you use MiniMessage colour tags like <red>text</red>.
        return ComponentParser.parseRichString("Surround an empty slot with gold to make a diamond");
    }

    // --- Optional overrides (delete them to keep the defaults) ---

    @Override
    public boolean isListed() {
        // true (default) = gets its own entry in the /recipes menu.
        // Set to false for a variant of a recipe already listed (e.g. a blast-furnace version),
        // so /recipes doesn't fill up with near-duplicates.
        return true;
    }

    @Override
    public boolean isRegistered() {
        // true (default) = Bukkit actually registers it as a working recipe.
        // Set to false if the crafting is handled by hand in a listener instead (e.g. it needs
        // to consume whole stacks, or pick a random result) - then this class is only here to
        // give the recipe a /recipes entry.
        return true;
    }

    // ---------------------------------------------------------------------------------------
    // HOW TO USE THIS:
    // 1. Copy this file into src/main/java/threeadd/glimpsy/feature/recipes and rename it.
    // 2. Change the result, key, shape and ingredients above.
    // 3. Open util/recipes/CustomRecipeRegistry.java and add `new YourRecipe()` to the
    //    register(...) list in its constructor.
    // 4. Rebuild and deploy. That's it - it now works in-game and shows in /recipes.
    // ---------------------------------------------------------------------------------------
}
