package threeadd.glimpsy.feature.explosive;

import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.keys.ItemTypeKeys;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemType;
import threeadd.glimpsy.util.enchantments.CustomEnchantment;
import threeadd.glimpsy.util.enchantments.CustomEnchantmentInfo;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public final class ExplosiveEnchantment extends CustomEnchantment {

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Allow arrows to explode when they hit a block.");
    }

    @Override
    public Key createKey() {
        return TypedKey.create(RegistryKey.ENCHANTMENT, Key.key("2tenty:explosive"));
    }

    @Override
    public Component createDescription() {
        return Component.text("Explosive");
    }

    // Not config-driven like most other balance knobs - createInfo() is called synchronously
    // during Paper's plugin bootstrap phase (see GlimpsyBootstrap), which runs before Glimpsy's
    // own onEnable() sets its instance or loads config.yml, so ConfigManager isn't safely usable
    // this early.
    @Override
    public CustomEnchantmentInfo createInfo() {
        return new CustomEnchantmentInfo(
                1,
                5,
                10,
                EnchantmentRegistryEntry.EnchantmentCost.of(1, 1),
                EnchantmentRegistryEntry.EnchantmentCost.of(30, 1)
        );
    }

    @Override
    public List<Enchantment> createIncompatibleEnchantments() {
        return List.of(Enchantment.INFINITY);
    }

    @Override
    public List<TypedKey<ItemType>> createSupportedItems() {
        return List.of(
                ItemTypeKeys.BOW,
                ItemTypeKeys.CROSSBOW
        );
    }

    @Override
    public EquipmentSlotGroup createActiveSlots() {
        return EquipmentSlotGroup.HAND;
    }

    @Override
    public List<TagKey<Enchantment>> createTags() {
        // ON_RANDOM_LOOT deliberately omitted - Explosive is no longer obtainable from vanilla
        // random loot (fishing treasure, dungeon chests, etc). It's now only available via
        // the enchanting table, or the low-chance charged creeper fishing catch.
        return List.of(
                EnchantmentTagKeys.IN_ENCHANTING_TABLE
        );
    }
}
