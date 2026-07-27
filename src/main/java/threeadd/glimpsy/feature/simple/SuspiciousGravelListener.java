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

public class SuspiciousGravelListener implements Listener {

    private static final Material[] COMMON_LOOT = {
            Material.BOW,
            Material.NAUTILUS_SHELL,
            Material.FISHING_ROD,
            Material.BUNDLE,
            Material.CARROT,
            Material.CARROT,
            Material.POTATO,
            Material.POTATO,
            Material.DEAD_BUSH,
            Material.DEAD_BUSH,
            Material.GOLD_INGOT,
            Material.COAL,
            Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE,
            Material.NETHERITE_SCRAP,
            Material.QUARTZ,
            Material.CHORUS_PLANT,
            Material.LAPIS_LAZULI,
            Material.LAPIS_LAZULI,
            Material.LAPIS_LAZULI
    };

    @EventHandler
    public void onBrush(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.SUSPICIOUS_GRAVEL) return;
        if (!(block.getState() instanceof BrushableBlock brushableBlock)) return;
        if (brushableBlock.getItem().getType() != Material.AIR) return;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        ItemStack result;

        int commonLootChancePercent = ConfigManager.getInt("suspicious-gravel.common-loot-chance-percent", 98);
        if (random.nextInt(100) <= commonLootChancePercent) {
            result = new ItemStack(COMMON_LOOT[random.nextInt(COMMON_LOOT.length)]);
        } else {
            LootTable lootTable = Bukkit.getLootTable(LootTables.ANCIENT_CITY.getKey());
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
