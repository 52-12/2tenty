package threeadd.glimpsy.feature.telekinesis;

import org.bukkit.GameMode;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import threeadd.glimpsy.util.enchantments.CustomEnchantmentRegistry;

import java.util.ArrayList;
import java.util.Map;

/**
 * Moves the drops of blocks broken / mobs killed with a Telekinesis tool straight into the player's
 * inventory (overflow still drops normally). Block drops are taken from {@link BlockDropItemEvent}
 * so anything other listeners have added to the drop set is included too.
 */
public class TelekinesisListener implements Listener {

    private static Enchantment telekinesis() {
        return CustomEnchantmentRegistry.INSTANCE.TELEKINESIS.toBukkitEnchantment();
    }

    private static boolean hasTelekinesis(Player player) {
        ItemStack tool = player.getInventory().getItemInMainHand();
        return tool != null && tool.containsEnchantment(telekinesis());
    }

    // HIGH so it runs after the other block-drop listeners, collecting everything they added.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        if (!hasTelekinesis(player)) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;

        for (Item drop : new ArrayList<>(event.getItems())) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(drop.getItemStack());
            for (ItemStack overflow : leftover.values())
                player.getWorld().dropItemNaturally(drop.getLocation(), overflow);
        }

        event.getItems().clear();
    }

    // HIGH so it runs after listeners that add to getDrops() (heads, guardian loot, ...).
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (!hasTelekinesis(killer)) return;
        if (killer.getGameMode() == GameMode.CREATIVE) return;

        for (ItemStack drop : new ArrayList<>(event.getDrops())) {
            if (drop == null) continue;

            Map<Integer, ItemStack> leftover = killer.getInventory().addItem(drop.clone());
            event.getDrops().remove(drop);

            for (ItemStack overflow : leftover.values())
                killer.getWorld().dropItemNaturally(event.getEntity().getLocation(), overflow);
        }
    }
}
