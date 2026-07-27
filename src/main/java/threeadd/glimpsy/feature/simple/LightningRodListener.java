package threeadd.glimpsy.feature.simple;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import threeadd.glimpsy.util.config.ConfigManager;

public class LightningRodListener implements Listener {

    @EventHandler
    public void onStrike(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.isInRain()) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.LIGHTNING_ROD) return;

        Location location = block.getLocation();
        if (location.getWorld().getHighestBlockAt(location).getY() != block.getY()) return;

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() != Material.COPPER_INGOT) return;
        if (player.getCooldown(Material.COPPER_INGOT) > 0) return;

        heldItem.subtract(1);
        player.setCooldown(Material.COPPER_INGOT, ConfigManager.getInt("lightning-rod.cooldown-ticks", 100));

        block.getWorld().spawnEntity(location, EntityType.LIGHTNING_BOLT, CreatureSpawnEvent.SpawnReason.CUSTOM);

        player.playSound(player, Sound.BLOCK_GRINDSTONE_USE, 0.5F, 1.0F);
        player.playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 1.5F);
    }
}
