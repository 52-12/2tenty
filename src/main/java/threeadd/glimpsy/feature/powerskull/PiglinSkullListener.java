package threeadd.glimpsy.feature.powerskull;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public class PiglinSkullListener implements Listener {

    @EventHandler
    public void onEquip(PlayerArmorChangeEvent event) {
        if (event.getSlotType() != PlayerArmorChangeEvent.SlotType.HEAD) return;
        if (event.getNewItem().getType() != Material.PIGLIN_HEAD) return;

        Player player = event.getPlayer();
        player.playSound(player, Sound.ENTITY_PIGLIN_AMBIENT, 1.0F, 1.0F);
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        ItemStack helmet = attacker.getInventory().getHelmet();
        if (helmet == null || helmet.getType() != Material.PIGLIN_HEAD) return;

        event.setDamage(event.getDamage() * 2.5);
    }

    @EventHandler
    public void onHurt(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null || helmet.getType() != Material.PIGLIN_HEAD) return;

        player.playSound(player.getLocation(), Sound.ENTITY_PIGLIN_HURT, 1.0F, 1.0F);
    }
}
