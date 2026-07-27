package threeadd.glimpsy.feature.simple;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerFishingListener implements Listener {

    private static double fishPlayerChance() {
        return ConfigManager.getDouble("player-fishing.catch-chance", 0.001); // per successful catch - very rare
    }

    private static final Set<UUID> immunePlayers = ConcurrentHashMap.newKeySet();

    public static boolean isImmune(Player player) {
        return immunePlayers.contains(player.getUniqueId());
    }

    public static void setImmune(Player player, boolean immune) {
        if (immune)
            immunePlayers.add(player.getUniqueId());
        else
            immunePlayers.remove(player.getUniqueId());
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (ThreadLocalRandom.current().nextDouble() >= fishPlayerChance()) return;

        Player fisher = event.getPlayer();

        List<Player> candidates = new ArrayList<>(Bukkit.getOnlinePlayers());
        candidates.remove(fisher);
        candidates.removeIf(PlayerFishingListener::isImmune);
        if (candidates.isEmpty()) return;

        Player caught = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));

        event.setCancelled(true);

        Location hookLocation = event.getHook().getLocation();
        caught.teleport(hookLocation.clone().add(0, 1, 0));

        hookLocation.getWorld().playSound(hookLocation, Sound.ENTITY_GENERIC_SPLASH, 1.0F, 1.0F);
        hookLocation.getWorld().spawnParticle(Particle.SPLASH, hookLocation, 30, 0.3, 0.3, 0.3);

        fisher.sendMessage(ComponentParser.parseRichString("You reeled in <yellow>" + caught.getName() + "</yellow>!"));
        caught.sendMessage(ComponentParser.parseRichString("<yellow>" + fisher.getName() + "</yellow> caught you with their fishing rod!"));
    }
}
