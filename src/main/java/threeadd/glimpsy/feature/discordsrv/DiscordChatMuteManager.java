package threeadd.glimpsy.feature.discordsrv;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import threeadd.glimpsy.util.manager.ListeningManager;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players have muted DiscordSRV's Discord-to-Minecraft chat relay for
 * themselves. Deliberately holds no reference to any DiscordSRV API type, so this class is
 * always safe to load regardless of whether DiscordSRV is installed - only
 * DiscordSRVChatBridgeListener touches DiscordSRV's classes, and only when DiscordSRV is
 * actually present (see Glimpsy#onEnable).
 */
public class DiscordChatMuteManager extends ListeningManager {

    private static final Set<UUID> mutedPlayers = ConcurrentHashMap.newKeySet();

    public static boolean isMuted(Player player) {
        return mutedPlayers.contains(player.getUniqueId());
    }

    public static void setMuted(Player player, boolean muted) {
        UUID id = player.getUniqueId();

        if (muted)
            mutedPlayers.add(id);
        else
            mutedPlayers.remove(id);

        DiscordChatMuteRepository.getInstance().setMutedAsync(id, muted);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();

        DiscordChatMuteRepository.getInstance().isMutedAsync(id)
                .thenAccept(muted -> {
                    if (muted) mutedPlayers.add(id);
                    else mutedPlayers.remove(id);
                });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        mutedPlayers.remove(event.getPlayer().getUniqueId());
    }
}
