package threeadd.glimpsy.feature.discordsrv;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreBroadcastEvent;
import github.scarsz.discordsrv.api.events.GameChatMessagePreProcessEvent;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filters DiscordSRV's chat relay per player, both directions:
 * <ul>
 *     <li>Discord -> Minecraft: a muted player is dropped from the recipient list.</li>
 *     <li>Minecraft -> Discord: a muted player's own chat is cancelled before it's sent to Discord.</li>
 * </ul>
 * Deliberately not a Bukkit Listener - DiscordSRV dispatches its own API events through its own
 * event bus (DiscordSRV.api.subscribe(...) + @Subscribe), not Bukkit's PluginManager.
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

    // Minecraft -> Discord. Cancelling the pre-process event stops the message being relayed to
    // Discord at all, which is the half that was missing before (#13).
    @Subscribe
    public void onGameChatPreProcess(GameChatMessagePreProcessEvent event) {
        Player player = event.getPlayer();
        if (player == null || !DiscordChatMuteManager.isMuted(player)) return;

        event.setCancelled(true);
        log.info("DiscordSRV relay suppressed a muted player's chat ({})", player.getName());
    }
}
