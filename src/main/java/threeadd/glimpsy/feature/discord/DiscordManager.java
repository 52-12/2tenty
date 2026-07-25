package threeadd.glimpsy.feature.discord;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import threeadd.glimpsy.util.ScheduleUtil;
import threeadd.glimpsy.util.manager.ListeningManager;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordManager extends ListeningManager {

    public static final String DISCORD_INVITE_URL = "https://discord.gg/3FmEbdKqAK";

    private static final Set<UUID> messagesDisabled = ConcurrentHashMap.newKeySet();

    public static Component createInviteMessage() {
        return ComponentParser.parseRichString(
                "<click:open_url:'" + DISCORD_INVITE_URL + "'><#5865F2>Join our Discord: " + DISCORD_INVITE_URL + "</click>\n" +
                        "<gray>Disable this with /togglediscord</gray>");
    }

    public static boolean isMessagesEnabled(Player player) {
        return !messagesDisabled.contains(player.getUniqueId());
    }

    public static void setMessagesEnabled(Player player, boolean enabled) {
        UUID id = player.getUniqueId();

        if (enabled)
            messagesDisabled.remove(id);
        else
            messagesDisabled.add(id);

        DiscordMessagesRepository.getInstance().setDisabledAsync(id, !enabled);
    }

    private void announce() {
        Component message = createInviteMessage();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isMessagesEnabled(player))
                player.sendMessage(message);
        }
    }

    @Override
    public void onEnable() {
        ScheduleUtil.scheduleSync(this::announce, 6, 6, ScheduleUtil.Unit.MINUTE);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();

        DiscordMessagesRepository.getInstance().isDisabledAsync(id)
                .thenAccept(disabled -> {
                    if (disabled) messagesDisabled.add(id);
                    else messagesDisabled.remove(id);
                });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        messagesDisabled.remove(event.getPlayer().getUniqueId());
    }
}
