package threeadd.glimpsy.template;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.config.Messages;

/**
 * TEMPLATE: a listener that reacts to something happening in the game. Copy this into a feature
 * package (e.g. {@code feature/simple}), rename it, change the event, then register it.
 * <p>
 * A listener implements {@link Listener} and has one or more methods annotated with
 * {@link EventHandler}. Each such method takes exactly one Bukkit event and runs whenever that
 * event fires. Browse the {@code org.bukkit.event} packages to find the event you need
 * (PlayerJoinEvent, EntityDeathEvent, PlayerInteractEvent, ...).
 */
public class ExampleListener implements Listener {

    // A tiny helper that reads a tunable number from config.yml, with a default if it's missing.
    // Reading it fresh each time (rather than caching) means /2tentyreload takes effect instantly.
    private static double rewardChance() {
        return ConfigManager.getDouble("example.diamond-chance", 0.05); // 5%
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Only do something 5% of the time (configurable). Math.random() returns 0.0 .. 1.0.
        if (Math.random() >= rewardChance()) return;

        // Send the player a message. Messages.msg(key, default) pulls the text from messages.yml
        // if present, otherwise uses the default here. <success>/<primary>/... are branding tags.
        player.sendMessage(Messages.msg("example.lucky-break", "<success>Lucky! You found something."));
    }

    // ---------------------------------------------------------------------------------------
    // HOW TO USE THIS:
    // 1. Copy this file into a feature package and rename it.
    // 2. Change the @EventHandler method(s) to react to the event you care about.
    // 3. Open util/listener/ListenerRegistry.java and add `new YourListener()` to register(...).
    //    (If your class instead needs to load/save data on startup, extend ListeningManager and
    //     register it in ManagerRegistry - see ExampleManager-style classes in feature/stats.)
    // 4. Rebuild and deploy.
    // ---------------------------------------------------------------------------------------
}
