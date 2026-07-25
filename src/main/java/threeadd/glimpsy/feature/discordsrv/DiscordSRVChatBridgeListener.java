package threeadd.glimpsy.feature.discordsrv;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreBroadcastEvent;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filters DiscordSRV's Discord-to-Minecraft chat relay per player. Deliberately not a Bukkit
 * Listener - DiscordSRV dispatches its own API events through its own event bus
 * (DiscordSRV.api.subscribe(...) + @Subscribe), not Bukkit's PluginManager.
 *
 * @apiNote Only ever call {@link #register()}, and only when DiscordSRV is confirmed present
 * (see Glimpsy#onEnable) - referencing this class at all resolves DiscordSRV's classes, which
 * throws NoClassDefFoundError if DiscordSRV isn't installed.
 */
public class DiscordSRVChatBridgeListener {

    private static final Logger log = LoggerFactory.getLogger(DiscordSRVChatBridgeListener.class);

    public static void register() {
        DiscordSRV.api.subscribe(new DiscordSRVChatBridgeListener());
        log.info("DiscordSRV chat mute bridge subscribed");
    }

    @Subscribe
    public void onPreBroadcast(DiscordGuildMessagePreBroadcastEvent event) {
        int before = event.getRecipients().size();

        event.getRecipients().removeIf(recipient ->
                recipient instanceof Player player && DiscordChatMuteManager.isMuted(player));

        int after = event.getRecipients().size();
        log.info("DiscordSRV pre-broadcast: {} recipients before mute filter, {} after", before, after);
    }
}
