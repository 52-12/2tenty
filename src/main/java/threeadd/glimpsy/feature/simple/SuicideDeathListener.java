package threeadd.glimpsy.feature.simple;

import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class SuicideDeathListener implements Listener {

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!SuicideCommand.SUPPRESS_DEATH_MESSAGE.remove(event.getPlayer().getUniqueId())) return;

        event.deathMessage(Component.empty());
    }
}
