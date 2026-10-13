package threeadd.glimpsy.feature.simple;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import threeadd.glimpsy.feature.achievement.Achievement;
import threeadd.glimpsy.feature.achievement.AchievementManager;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.preferences.PlayerPreferences;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerFishingListener implements Listener {

    private static double fishPlayerChance() {
        return ConfigManager.getDouble("player-fishing.catch-chance", 0.001); // per successful catch - very rare
    }

    private static final String IMMUNE_KEY = "fish-immune";

    public static boolean isImmune(Player player) {
        return PlayerPreferences.get(player, IMMUNE_KEY, false);
    }

    public static void setImmune(Player player, boolean immune) {
        PlayerPreferences.set(player, IMMUNE_KEY, immune);
    }

    // NORMAL: runs after the guardian roll (LOW) but before the creeper roll (HIGH). ignoreCancelled
    // means it's skipped entirely once an earlier special catch has already cancelled the event.
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
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

        hookLocation.getWorld().spawnParticle(Particle.SPLASH, hookLocation, 30, 0.3, 0.3, 0.3);
        FishingSounds.playCatch(hookLocation, Sound.ENTITY_GENERIC_SPLASH, 1.0F, 1.0F, fisher);
        FishingSounds.playCatch(hookLocation, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0F, 1.0F, fisher); // pull like a normal catch

        AchievementManager.award(fisher, Achievement.OH_HELLO_THERE);

        fisher.sendMessage(ComponentParser.parseRichString("You reeled in <yellow>" + caught.getName() + "</yellow>!"));
        caught.sendMessage(ComponentParser.parseRichString("<yellow>" + fisher.getName() + "</yellow> caught you with their fishing rod!"));

        event.getHook().remove(); // end the cast so it can't also land a normal fish
    }
}
