package threeadd.glimpsy.feature.help.list;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import threeadd.glimpsy.util.inventory.CustomInventory;
import threeadd.glimpsy.util.inventory.ItemBuilder;
import threeadd.glimpsy.util.recipes.CustomRecipe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class RecipeInfoInventory extends CustomInventory {

    private final Map<Integer, List<ItemStack>> cyclingSlots = new HashMap<>();

    public RecipeInfoInventory(Player viewer, CustomRecipe customRecipe) {
        super(Rows.FIVE, buildTitle(customRecipe), viewer);

        setTransferSlot(Rows.FIVE.getTotalSlots() - 9,
                new ItemBuilder(Material.RED_DYE)
                        .withName(Component.text("Return")
                                .color(TextColor.color(0xDB5858)))
                        .build(),
                () -> new RecipeListInventory(getViewer()));

        Recipe recipe = customRecipe.createRecipe();
        addResultAndType(recipe);

        switch (recipe) {
            case ShapedRecipe shapedRecipe -> handleShaped(shapedRecipe);
            case ShapelessRecipe shapelessRecipe -> handleShapeless(shapelessRecipe);
            case FurnaceRecipe furnaceRecipe -> handleFurnace(furnaceRecipe);
            default -> throw new IllegalStateException("Unknown handling for recipe " + customRecipe.createRecipe());
        }

        startCyclingSlots();
    }

    // Item names can carry their own explicit color (e.g. from ItemBuilder#withName), which would
    // otherwise override this title's color for just that portion of the text, leaving a
    // mismatched chest name - strip it back to plain text first so the whole title is one
    // consistent color no matter what the result item itself looks like.
    private static Component buildTitle(CustomRecipe customRecipe) {
        String resultName = PlainTextComponentSerializer.plainText()
                .serialize(customRecipe.createRecipe().getResult().effectiveName());

        return Component.text(resultName + " Recipe").color(NamedTextColor.BLACK);
    }

    private static List<ItemStack> getItemStacksFromChoice(RecipeChoice choice) {
        if (choice instanceof RecipeChoice.ExactChoice exactChoice) {
            return exactChoice.getChoices();
        }

        if (choice instanceof RecipeChoice.MaterialChoice materialChoice) {
            return materialChoice.getChoices().stream()
                    .map(ItemStack::of)
                    .toList();
        }

        throw new IllegalStateException("Couldn't eval recipe choice instance " + choice);
    }

    private void startCyclingSlots() {
        for (Map.Entry<Integer, List<ItemStack>> entry : cyclingSlots.entrySet()) {
            int slot = entry.getKey();
            List<ItemStack> items = entry.getValue();

            Supplier<ItemStack> supplier = cyclingItemSupplier(items);

            setUpdatingSlot(slot, supplier, () -> null, 500, TimeUnit.MILLISECONDS);
        }
    }

    private Supplier<ItemStack> cyclingItemSupplier(List<ItemStack> items) {
        return new Supplier<>() {
            private int animationIndex = 0;

            @Override
            public ItemStack get() {
                if (items.isEmpty()) return null;
                ItemStack item = items.get(animationIndex % items.size());
                animationIndex++;
                return item;
            }
        };
    }

    private void addResultAndType(Recipe recipe) {
        int arrowSlot = 2 * 9 + 5;
        int resultSlot = arrowSlot + 2;

        Material type;

        if (recipe instanceof CraftingRecipe)
            type = Material.CRAFTING_TABLE;
        else if (recipe instanceof FurnaceRecipe)
            type = Material.FURNACE;
        else
            throw new IllegalStateException("Unknown Recipe " + recipe);

        ItemStack arrow = new ItemBuilder(type)
                .withName(Component.text("Result")
                        .color(TextColor.color(0xDBC258)))
                .build();

        setSlot(arrowSlot, arrow);

        // Set the actual recipe output
        ItemStack result = recipe.getResult().clone();
        setSlot(resultSlot, result);
    }

    private void handleShaped(ShapedRecipe recipe) {
        String[] shape = recipe.getShape();
        Map<Character, RecipeChoice> choiceMap = recipe.getChoiceMap();

        int startRow = 1;
        int startCol = 2;

        for (int row = 0; row < shape.length; row++) {
            String line = shape[row];
            for (int col = 0; col < line.length(); col++) {
                char c = line.charAt(col);
                RecipeChoice choice = choiceMap.get(c);
                int slotIndex = (startRow + row) * 9 + (startCol + col);
                if (choice != null) {
                    setChoiceSlot(slotIndex, choice);
                } else {
                    setSlot(slotIndex, (ItemStack) null);
                }
            }
        }
    }

    private void handleShapeless(ShapelessRecipe recipe) {
        int startRow = 1;
        int startCol = 2;
        int colCount = 3;

        int index = 0;
        for (RecipeChoice choice : recipe.getChoiceList()) {
            int rowOffset = index / colCount;
            int colOffset = index % colCount;
            int slotIndex = (startRow + rowOffset) * 9 + (startCol + colOffset);

            setChoiceSlot(slotIndex, choice);
            index++;
        }
    }

    private void handleFurnace(FurnaceRecipe recipe) {
        int startRow = 1;
        int startCol = 2;

        int slotIndex = startRow * 9 + startCol;
        setChoiceSlot(slotIndex, recipe.getInputChoice());
    }

    private void setChoiceSlot(int slotIndex, RecipeChoice choice) {
        List<ItemStack> options = getItemStacksFromChoice(choice);

        if (options.size() == 1) {
            setSlot(slotIndex, options.getFirst());
            return;
        }

        cyclingSlots.put(slotIndex, options);
        setSlot(slotIndex, options.getFirst());
    }
}
