package threeadd.glimpsy.util.recipes;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;
import threeadd.glimpsy.util.Documentable;

import java.util.List;

public abstract class CustomRecipe implements Documentable {
    public abstract Recipe createRecipe();

    public NamespacedKey getKey() {
        Recipe recipe = createRecipe();

        if (recipe instanceof Keyed keyed)
            return keyed.getKey();

        throw new IllegalStateException("Unknown CraftingRecipe instance" + recipe);
    }

    public List<RecipeChoice> getRequirements() {
        Recipe recipe = createRecipe();

        if (recipe instanceof ShapelessRecipe shapelessRecipe) {
            return shapelessRecipe.getChoiceList();
        } else if (recipe instanceof ShapedRecipe shapedRecipe) {
            return shapedRecipe.getChoiceMap().values().stream().toList();
        } else if (recipe instanceof FurnaceRecipe furnaceRecipe) {
            return List.of(furnaceRecipe.getInputChoice());
        }

        throw new IllegalStateException("Unknown CraftingRecipe instance" + recipe);
    }

    /**
     * Whether this recipe gets its own entry in /recipes. Doesn't affect whether the recipe
     * actually works in-game - only its own registration in {@link threeadd.glimpsy.util.recipes.CustomRecipeRegistry}
     * controls that. Override this to {@code false} on recipes that are just a variant of
     * another one already listed (e.g. the other armor pieces, or the blast furnace version of
     * one already shown) so /recipes doesn't fill up with near-duplicates.
     */
    public boolean isListed() {
        return true;
    }

    /**
     * Whether this recipe should actually be registered with Bukkit as a working recipe.
     * Override to {@code false} for recipes that only need a /recipes entry because the real
     * crafting is handled by hand elsewhere (e.g. requires consuming whole stacks, or picking a
     * weighted result) - a registered {@link Recipe} can't express that, and would let players
     * bypass the real logic by matching it with a single item per slot instead.
     */
    public boolean isRegistered() {
        return true;
    }
}
