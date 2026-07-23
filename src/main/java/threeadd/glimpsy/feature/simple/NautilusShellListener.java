package threeadd.glimpsy.feature.simple;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class NautilusShellListener implements Listener {

    private static final double BONUS_SHELL_CHANCE = 0.05; // 5% bonus chance per successful catch

    // MONITOR + ignoreCancelled so this only applies to catches that weren't hijacked by
    // ChargedCreeperFishingListener/GuardianFishingListener (both run at NORMAL priority).
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (ThreadLocalRandom.current().nextDouble() >= BONUS_SHELL_CHANCE) return;

        Player player = event.getPlayer();
        Map<Integer, ItemStack> leftOver = player.getInventory().addItem(new ItemStack(Material.NAUTILUS_SHELL));
        for (ItemStack leftOverItem : leftOver.values()) {
            player.getWorld().dropItem(event.getHook().getLocation(), leftOverItem);
        }
    }

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
        player.setCooldown(Material.NAUTILUS_SHELL, 20);
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
