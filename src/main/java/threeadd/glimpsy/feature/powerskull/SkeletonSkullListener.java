package threeadd.glimpsy.feature.powerskull;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import threeadd.glimpsy.util.config.ConfigManager;

public class SkeletonSkullListener implements Listener {

    @EventHandler
    public void onEquip(PlayerArmorChangeEvent event) {
        if (event.getSlotType() != PlayerArmorChangeEvent.SlotType.HEAD) return;
        if (event.getNewItem().getType() != Material.SKELETON_SKULL) return;

        Player player = event.getPlayer();
        player.playSound(player, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, 1.0F, 1.0F);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getDamager().getType() != EntityType.ARROW) return;

        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null || helmet.getType() != Material.SKELETON_SKULL) return;

        if (player.getCooldown(Material.SKELETON_SKULL) > 0) return;
        player.setCooldown(Material.SKELETON_SKULL, ConfigManager.getInt("skeleton-skull.reflect-cooldown-seconds", 3) * 20);

        player.playSound(player, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, 1.0F, 1.0F);
        event.setDamage(ConfigManager.getDouble("skeleton-skull.reduced-arrow-damage", 1));
    }
}
