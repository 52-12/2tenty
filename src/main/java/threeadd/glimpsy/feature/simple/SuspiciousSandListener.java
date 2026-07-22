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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SuspiciousSandListener implements Listener {

    private static final Material[] COMMON_LOOT = {
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

        if (random.nextInt(100) <= 50) {
            result = new ItemStack(COMMON_LOOT[random.nextInt(COMMON_LOOT.length)]);
        } else {
            LootTable lootTable = Bukkit.getLootTable(LootTables.DESERT_PYRAMID.getKey());
            if (lootTable == null) return;

            LootContext lootContext = new LootContext.Builder(block.getLocation()).build();
            Collection<ItemStack> lootItems = lootTable.populateLoot(random, lootContext);
            lootItems.removeIf(itemStack -> switch (itemStack.getType()) {
                case ENCHANTED_GOLDEN_APPLE, GOLDEN_APPLE -> true;
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
