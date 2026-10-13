package threeadd.glimpsy.feature.teams;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import threeadd.glimpsy.util.text.ComponentParser;

public class TeamChatListener implements Listener {

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!TeamChatManager.isEnabled(player)) return;

        event.setCancelled(true);

        if (TeamManager.getTeam(player) == null) {
            // Turn the mode back off rather than leave the player silently unable to chat.
            TeamChatManager.toggle(player);
            player.sendMessage(ComponentParser.parseRichString("<red>You aren't on a team - team chat was turned back off"));
            return;
        }

        String plainMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
        TeamChatManager.send(player, plainMessage);
    }
}
