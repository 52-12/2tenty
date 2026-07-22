package threeadd.glimpsy.feature.simple;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CorianRootListener implements Listener {

    private static final double MAX_HEALTH_CAP = 40.0;

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() != Material.BEETROOT) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomName()) return;
        if (!meta.customName().toString().contains("Corian Root")) return;

        Player player = event.getPlayer();
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null || maxHealth.getBaseValue() >= MAX_HEALTH_CAP) return;

        double newMaxHealth = maxHealth.getBaseValue() + 1;
        maxHealth.setBaseValue(newMaxHealth);
        player.setHealthScale(newMaxHealth);
        player.heal(1.0);
    }
}
