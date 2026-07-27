package threeadd.glimpsy.feature.simple;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;
import threeadd.glimpsy.util.config.ConfigManager;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class NautilusShellListener implements Listener {

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() != Material.NAUTILUS_SHELL) return;
        if (player.getCooldown(Material.NAUTILUS_SHELL) > 0) return;

        LootTable lootTable = Bukkit.getLootTable(LootTables.SHIPWRECK_SUPPLY.getKey());
        if (lootTable == null) return;

        heldItem.subtract(1);
        player.setCooldown(Material.NAUTILUS_SHELL, ConfigManager.getInt("nautilus-shell.cooldown-ticks", 20));
        player.swingMainHand();
        player.playSound(player, Sound.ITEM_BRUSH_BRUSHING_SAND, 0.5F, 1.0F);

        LootContext lootContext = new LootContext.Builder(player.getLocation()).build();
        Collection<ItemStack> lootItems = lootTable.populateLoot(ThreadLocalRandom.current(), lootContext);

        Map<Integer, ItemStack> leftOver = player.getInventory().addItem(lootItems.toArray(new ItemStack[0]));
        for (ItemStack leftOverItem : leftOver.values()) {
            player.getWorld().dropItem(player.getLocation(), leftOverItem);
        }
    }
}
