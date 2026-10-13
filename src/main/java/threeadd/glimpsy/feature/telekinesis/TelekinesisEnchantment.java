package threeadd.glimpsy.feature.telekinesis;

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

/**
 * Telekinesis ("anti-steal") - mined-block and mob drops go straight into the holder's inventory
 * instead of landing on the floor where anyone could grab them. Level I only; obtained from
 * fished guardians (see GuardianFishingListener).
 */
@SuppressWarnings("UnstableApiUsage")
public final class TelekinesisEnchantment extends CustomEnchantment {

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Mined blocks and mob drops go straight into your inventory.");
    }

    @Override
    public Key createKey() {
        return TypedKey.create(RegistryKey.ENCHANTMENT, Key.key("2tenty:telekinesis"));
    }

    @Override
    public Component createDescription() {
        return Component.text("Telekinesis");
    }

    // Not config-driven - createInfo() runs during Paper's bootstrap phase, before config.yml is
    // loaded (same reason as ExplosiveEnchantment).
    @Override
    public CustomEnchantmentInfo createInfo() {
        return new CustomEnchantmentInfo(
                2,
                1,
                5,
                EnchantmentRegistryEntry.EnchantmentCost.of(1, 1),
                EnchantmentRegistryEntry.EnchantmentCost.of(50, 1)
        );
    }

    @Override
    public List<Enchantment> createIncompatibleEnchantments() {
        return List.of();
    }

    @Override
    public List<TypedKey<ItemType>> createSupportedItems() {
        return List.of(
                ItemTypeKeys.WOODEN_PICKAXE, ItemTypeKeys.STONE_PICKAXE, ItemTypeKeys.IRON_PICKAXE,
                ItemTypeKeys.GOLDEN_PICKAXE, ItemTypeKeys.DIAMOND_PICKAXE, ItemTypeKeys.NETHERITE_PICKAXE,
                ItemTypeKeys.WOODEN_AXE, ItemTypeKeys.STONE_AXE, ItemTypeKeys.IRON_AXE,
                ItemTypeKeys.GOLDEN_AXE, ItemTypeKeys.DIAMOND_AXE, ItemTypeKeys.NETHERITE_AXE,
                ItemTypeKeys.WOODEN_SHOVEL, ItemTypeKeys.STONE_SHOVEL, ItemTypeKeys.IRON_SHOVEL,
                ItemTypeKeys.GOLDEN_SHOVEL, ItemTypeKeys.DIAMOND_SHOVEL, ItemTypeKeys.NETHERITE_SHOVEL,
                ItemTypeKeys.WOODEN_HOE, ItemTypeKeys.STONE_HOE, ItemTypeKeys.IRON_HOE,
                ItemTypeKeys.GOLDEN_HOE, ItemTypeKeys.DIAMOND_HOE, ItemTypeKeys.NETHERITE_HOE,
                ItemTypeKeys.WOODEN_SWORD, ItemTypeKeys.STONE_SWORD, ItemTypeKeys.IRON_SWORD,
                ItemTypeKeys.GOLDEN_SWORD, ItemTypeKeys.DIAMOND_SWORD, ItemTypeKeys.NETHERITE_SWORD
        );
    }

    @Override
    public EquipmentSlotGroup createActiveSlots() {
        return EquipmentSlotGroup.HAND;
    }

    @Override
    public List<TagKey<Enchantment>> createTags() {
        // Deliberately not in the enchanting table / random loot tags - the only source is the
        // fished-guardian book drop, so it stays a fishing goal.
        return List.of();
    }
}
