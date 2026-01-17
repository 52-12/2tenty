package threeadd.glimpsy.feature.explosive;

import org.bukkit.entity.Arrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import threeadd.glimpsy.feature.simple.TNTTrailListener;
import threeadd.glimpsy.util.enchantments.CustomEnchantmentRegistry;

public class ExplosiveListener implements Listener {

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow))
            return;

        ItemStack bow = arrow.getWeapon();
        if (bow == null) return;

        if (!bow.containsEnchantment(CustomEnchantmentRegistry.INSTANCE.EXPLOSIVE.toBukkitEnchantment()))
            return;

        int level = bow.getEnchantmentLevel(CustomEnchantmentRegistry.INSTANCE.EXPLOSIVE.toBukkitEnchantment());
        int blocksAffected = level * 3;

        TNTTrailListener.explode(arrow.getLocation(), blocksAffected);
        arrow.remove();
    }
}
