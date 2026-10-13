package threeadd.glimpsy.feature.explosive;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import threeadd.glimpsy.feature.simple.TNTTrailListener;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.enchantments.CustomEnchantmentRegistry;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ExplosiveListener implements Listener {

    // Session-only, per-shooter - last time their Explosive enchant actually detonated
    private static final Map<UUID, Long> lastDetonationMillis = new ConcurrentHashMap<>();

    private static long cooldownMillis() {
        return ConfigManager.getLong("explosive-enchant.cooldown-seconds", 0) * 1000L;
    }

    private static int blocksAffectedForLevel(int level) {
        // The old flat "blocks-per-level" multiplier is still the fallback for levels that
        // aren't explicitly listed in explosive-enchant.blocks-per-level
        return ConfigManager.getMappedInt("explosive-enchant.blocks-per-level", level,
                level * ConfigManager.getInt("explosive-enchant.blocks-per-level-fallback", 3));
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow))
            return;

        ItemStack bow = arrow.getWeapon();
        if (bow == null) return;

        if (!bow.containsEnchantment(CustomEnchantmentRegistry.INSTANCE.EXPLOSIVE.toBukkitEnchantment()))
            return;

        if (arrow.getShooter() instanceof Player shooter) {
            long cooldown = cooldownMillis();
            if (cooldown > 0) {
                long now = System.currentTimeMillis();
                Long last = lastDetonationMillis.get(shooter.getUniqueId());
                if (last != null && now - last < cooldown)
                    return; // on cooldown - arrow just lands normally, no explosion

                lastDetonationMillis.put(shooter.getUniqueId(), now);
            }
        }

        int level = bow.getEnchantmentLevel(CustomEnchantmentRegistry.INSTANCE.EXPLOSIVE.toBukkitEnchantment());
        int blocksAffected = blocksAffectedForLevel(level);

        TNTTrailListener.explode(arrow.getLocation(), blocksAffected);
        arrow.remove();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastDetonationMillis.remove(event.getPlayer().getUniqueId());
    }
}
