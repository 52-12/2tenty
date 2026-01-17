package threeadd.glimpsy.util.inventory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import threeadd.glimpsy.util.text.ComponentSplitter;
import threeadd.glimpsy.util.text.ComponentWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class ItemBuilder {

    private ItemStack item;

    /**
     * Create a builder from a Material.
     *
     * @param material The Material
     */
    public ItemBuilder(Material material) {
        this.item = ItemStack.of(material);
    }

    /**
     * Create a builder from a previous ItemStack (clones the ItemStack)
     *
     * @param item The ItemStack
     */
    public ItemBuilder(@NotNull ItemStack item) {
        this.item = item.clone();
    }

    public ItemStack getItem() {
        return item;
    }

    /**
     * Change the display name of the ItemStack in the ItemBuilder
     *
     * @param name The new display name
     * @return The ItemBuilder with this new ItemStack
     * @see ItemMeta#displayName(Component)
     */
    public ItemBuilder withName(Component name) {
        withItemMeta(meta -> meta.displayName(name
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE) // default not italic
                .colorIfAbsent(NamedTextColor.WHITE))); // default white
        return this;
    }

    /**
     * Change the lore of the ItemStack in the ItemBuilder
     *
     * @param lore The lines of lore you want this item to have
     * @return The ItemBuilder with this new ItemStack
     * @see ItemBuilder#withLore(List)
     */
    public ItemBuilder withLore(Component... lore) {
        return withLore(List.of(lore));
    }

    /**
     * Change the lore of the ItemStack in the ItemBuilder
     *
     * @param lore The lines of lore you want this item to have
     * @return The ItemBuilder with this new ItemStack
     * @see ItemMeta#lore()
     */
    public ItemBuilder withLore(List<Component> lore) {
        withItemMeta(meta -> {

            List<Component> out = new ArrayList<>();
            for (Component line : lore) {
                List<Component> splitLines = ComponentSplitter.split(line, Pattern.compile("\n"));

                for (Component split : splitLines) {
                    for (Component wrapped : ComponentWrapper.wrapComponent(split, 75)) {

                        Component finalComp = wrapped
                                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE) // default not italic
                                .colorIfAbsent(NamedTextColor.WHITE); // default white

                        out.add(finalComp);
                    }
                }
            }

            meta.lore(out);
        });
        return this;
    }

    /**
     * Change the item amount of the ItemStack in the ItemBuilder
     *
     * @param itemAmount the new item amount
     * @return The ItemBuilder with this new ItemStack
     * @see ItemStack#setAmount(int)
     */
    public ItemBuilder withAmount(int itemAmount) {
        item.setAmount(itemAmount);
        return this;
    }

    /**
     * Change or apply an enchantment to the ItemStack in the ItemBuilder
     *
     * @param enchant The enchantment to chance/apply
     * @return The ItemBuilder with this new ItemStack
     * @see ItemMeta#addEnchant(Enchantment, int, boolean)
     */
    public ItemBuilder withEnchant(Enchantment enchant, int level) {
        withItemMeta(meta -> meta.addEnchant(enchant, level, true));
        return this;
    }

    /**
     * Change the all enchantments of the ItemStack in the ItemBuilder
     *
     * @param enchants A map from Enchantment to level
     * @return The ItemBuilder with this new ItemStack
     * @see ItemBuilder#withEnchant(Enchantment, int)
     */
    public ItemBuilder withEnchants(@NotNull Map<Enchantment, Integer> enchants) {
        enchants.forEach(this::withEnchant);
        return this;
    }

    /**
     * Change the CustomModelDataComponent of the item contained within this ItemBuilder.
     *
     * @see ItemBuilder#withItemMeta(Consumer)
     */
    public ItemBuilder withItemModel(String modelId) {
        withItemMeta(meta ->
                meta.setItemModel(new NamespacedKey("glimpsy", modelId)));
        return this;
    }

    /**
     * Change the {@link ItemMeta} of an item, internal methods also use this to change lore, name, etc.
     *
     * @param itemMeta The consumer which u alter the ItemMeta with.
     * @return The ItemBuilder with the changed item meta.
     * @see ItemBuilder#withItemMeta(Class, Consumer)
     */
    public ItemBuilder withItemMeta(Consumer<ItemMeta> itemMeta) {
        withItemMeta(ItemMeta.class, itemMeta);
        return this;
    }

    public ItemBuilder withItemMeta(ItemMeta newItemMeta) {
        item.setItemMeta(newItemMeta);
        return this;
    }

    /**
     * @param clazz        The {@link ItemMeta} class to use (ease of use for example {@link org.bukkit.inventory.meta.SkullMeta})
     * @param metaConsumer The consumer which u use to alter the {@link T}.
     * @param <T>          The type of {@link ItemMeta} to use
     * @return The ItemBuilder with the changed item inside.
     */
    public <T extends ItemMeta> ItemBuilder withItemMeta(Class<T> clazz, Consumer<T> metaConsumer) {
        ItemMeta itemMeta = item.getItemMeta();

        T meta = clazz.cast(itemMeta);
        metaConsumer.accept(meta);

        item.setItemMeta(meta);
        return this;
    }

    /**
     * Sets the unbreakable state if the item inside the builder
     *
     * @param state The new unbreakable state
     * @return The ItemBuilder with the updated state
     * @see ItemMeta#setUnbreakable(boolean)
     */
    public ItemBuilder withUnbreakableState(boolean state) {
        return withItemMeta(meta ->
                meta.setUnbreakable(state));
    }

    /**
     * Sets the enchanted glint state if the item inside the builder
     *
     * @param state The new enchanted glint state
     * @return The ItemBuilder with the updated state
     * @see ItemMeta#setEnchantmentGlintOverride(Boolean)
     */
    public ItemBuilder withEnchantedStated(boolean state) {
        return withItemMeta(meta ->
                meta.setEnchantmentGlintOverride(state));
    }

    /**
     * Apply {@link ItemFlag}s to the item inside the builder
     *
     * @param flags The flags to apply
     * @return The ItemBuilder with the updated flags
     * @see ItemFlag
     */
    public ItemBuilder withItemFlags(ItemFlag... flags) {
        return withItemMeta(meta ->
                meta.addItemFlags(flags));
    }

    /**
     * Change the material of the item inside this builder
     *
     * @param material The new material for the ite,
     * @return The ItemBuilder with the updated material
     * @apiNote This will change the ItemStack instance as they material is immutable
     * @see ItemStack#withType(Material)
     */
    public ItemBuilder withMaterial(Material material) {
        this.item = this.item.withType(material);
        return this;
    }

    /**
     * Build the ItemStack in this ItemBuilder
     *
     * @return The ItemStack
     */
    public ItemStack build() {
        return item;
    }
}
