package threeadd.glimpsy.feature.simple;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.inventory.ItemBuilder;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A very rare chance for a killed player to drop their own head - a trophy, not a resource, so
 * the odds are deliberately tiny. Pairs with /headcollections, where the heads end up.
 */
public class HeadDropListener implements Listener {

    private static double dropChance() {
        return ConfigManager.getDouble("head-drop.chance", 0.002); // 1 in 500 kills
    }

    // Off by default for mob kills / fall damage / lava etc. - a head is meant to be something
    // another player took off you, not something the world hands out
    private static boolean requirePlayerKiller() {
        return ConfigManager.getBoolean("head-drop.require-player-killer", true);
    }

    private static boolean announce() {
        return ConfigManager.getBoolean("head-drop.announce", true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        double chance = dropChance();
        if (chance <= 0) return;

        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null && requirePlayerKiller()) return;

        if (ThreadLocalRandom.current().nextDouble() >= chance) return;

        event.getDrops().add(createHead(victim));

        if (!announce()) return;

        Component message = killer == null
                ? Messages.msg("head-drop.dropped", "<primary>{victim} dropped their head!",
                        "victim", victim.getName())
                : Messages.msg("head-drop.dropped-by", "<primary>{killer} knocked {victim}'s head clean off!",
                        "killer", killer.getName(), "victim", victim.getName());
        Bukkit.broadcast(message);
    }

    private static ItemStack createHead(Player victim) {
        return new ItemBuilder(Material.PLAYER_HEAD)
                .withItemMeta(SkullMeta.class, meta -> meta.setOwningPlayer(victim))
                .withName(Messages.msg("head-drop.item-name", "{player}'s Head", "player", victim.getName()))
                .build();
    }
}
