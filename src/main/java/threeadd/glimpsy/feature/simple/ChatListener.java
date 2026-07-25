package threeadd.glimpsy.feature.simple;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import threeadd.glimpsy.feature.nickname.NicknameManager;
import threeadd.glimpsy.feature.teams.TeamManager;
import threeadd.glimpsy.util.text.ComponentParser;

public class ChatListener implements Listener {

    @EventHandler
    public void onChat(AsyncChatEvent event) {

        // Using viewerUnaware renderer boosts performance, see https://jd.papermc.io/paper/1.21.11-pre3/io/papermc/paper/chat/ChatRenderer.ViewerUnaware.html
        // Unfortunately we do need a viewer (to measure distance) and thus I just wasted your time :)

        String teamName = TeamManager.getTeamName(event.getPlayer());
        Component team = teamName == null ? Component.empty() : Component.textOfChildren(
                Component.text("[", NamedTextColor.GRAY),
                MiniMessage.miniMessage().deserialize(teamName),
                Component.text("] ", NamedTextColor.GRAY)
        );

        String nickname = NicknameManager.getNickname(event.getPlayer());

        event.renderer(((source, sourceDisplayName, message, receiver) -> {

            Component name;
            if (nickname != null) {
                // A custom nickname colors the name itself, so it takes priority over the
                // distance fade below.
                name = NicknameManager.getDisplayName(source.getName(), source.getUniqueId());
            } else {
                name = sourceDisplayName;
                if (receiver instanceof Player playerReceiver)
                    name = ComponentParser.getDistancedString(source.getLocation(), playerReceiver.getLocation(), source.getName());
            }

            return Component.textOfChildren(
                    team,
                    name,
                    ComponentParser.parseRichString(": "),
                    message
            );
        }));
    }
}
