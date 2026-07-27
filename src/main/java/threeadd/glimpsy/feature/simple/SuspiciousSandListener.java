package threeadd.glimpsy.feature.simple;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BrushableBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;
import threeadd.glimpsy.util.config.ConfigManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SuspiciousSandListener implements Listener {

    // These are the built-in defaults; the actual pools are read from config (see rareLoot() /
    // commonLoot()) so they can be customised in config.yml. Duplicates raise a material's odds.
    private static final Material[] RARE_LOOT_DEFAULT = {
            Material.FLINT,
            Material.QUARTZ
    };

    private static Material[] rareLoot() {
        return ConfigManager.getMaterialList("suspicious-sand.rare-loot", RARE_LOOT_DEFAULT);
    }

    private static Material[] commonLoot() {
        return ConfigManager.getMaterialList("suspicious-sand.common-loot", COMMON_LOOT_DEFAULT);
    }

    private static final Material[] COMMON_LOOT_DEFAULT = {
            Material.BOW,
            Material.NAUTILUS_SHELL,
            Material.NAUTILUS_SHELL,
            Material.FISHING_ROD,
            Material.BUNDLE,
            Material.CARROT,
            Material.POTATO,
            Material.BEETROOT_SEEDS,
            Material.WHEAT_SEEDS,
            Material.DEAD_BUSH,
            Material.DEAD_BUSH,
            Material.GOLD_INGOT,
            Material.COAL,
            Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.WILD_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE
    };

    @EventHandler
    public void onBrush(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.SUSPICIOUS_SAND) return;
        if (!(block.getState() instanceof BrushableBlock brushableBlock)) return;
        if (brushableBlock.getItem().getType() != Material.AIR) return;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        ItemStack result;

        double rareLootChancePercent = ConfigManager.getDouble("suspicious-sand.rare-loot-chance-percent", 3.0);
        int commonLootChancePercent = ConfigManager.getInt("suspicious-sand.common-loot-chance-percent", 50);

        if (random.nextDouble(100) < rareLootChancePercent) {
            Material[] rare = rareLoot();
            result = new ItemStack(rare[random.nextInt(rare.length)]);
        } else if (random.nextInt(100) <= commonLootChancePercent) {
            Material[] common = commonLoot();
            result = new ItemStack(common[random.nextInt(common.length)]);
        } else {
            LootTable lootTable = Bukkit.getLootTable(LootTables.DESERT_PYRAMID.getKey());
            if (lootTable == null) return;

            LootContext lootContext = new LootContext.Builder(block.getLocation()).build();
            Collection<ItemStack> lootItems = lootTable.populateLoot(random, lootContext);
            // Diamond horse armor is kept - only the raw diamond item is excluded.
            lootItems.removeIf(itemStack -> switch (itemStack.getType()) {
                case ENCHANTED_GOLDEN_APPLE, GOLDEN_APPLE, DIAMOND -> true;
                default -> false;
            });

            if (lootItems.isEmpty()) return;
            List<ItemStack> loot = new ArrayList<>(lootItems);
            result = loot.get(random.nextInt(loot.size()));
        }

        brushableBlock.setItem(result);
        brushableBlock.update();
    }
}
