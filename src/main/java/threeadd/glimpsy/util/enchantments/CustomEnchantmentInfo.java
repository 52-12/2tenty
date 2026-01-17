package threeadd.glimpsy.util.enchantments;

import io.papermc.paper.registry.data.EnchantmentRegistryEntry;

@SuppressWarnings("UnstableApiUsage")
public record CustomEnchantmentInfo(int anvilCost, int maxLevel, int weight,
                                    EnchantmentRegistryEntry.EnchantmentCost minCost,
                                    EnchantmentRegistryEntry.EnchantmentCost maxCost) {
}
