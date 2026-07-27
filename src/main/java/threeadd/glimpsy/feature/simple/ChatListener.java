package threeadd.glimpsy.feature.simple;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import threeadd.glimpsy.feature.nickname.NicknameManager;
import threeadd.glimpsy.feature.stats.PlayerStats;
import threeadd.glimpsy.feature.stats.StatCategory;
import threeadd.glimpsy.feature.stats.StatsManager;
import threeadd.glimpsy.feature.teams.TeamManager;
import threeadd.glimpsy.util.text.ComponentParser;

public class ChatListener implements Listener {

    private static final TextColor PLACEHOLDER_COLOR = TextColor.color(0xFFE08F);

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

        // Resolve the sender's own [kills]/[deaths]/[fishes]/[playtime] once (StatsManager is
        // concurrent-safe, so reading here on the async chat thread is fine).
        PlayerStats stats = StatsManager.getStats(event.getPlayer().getUniqueId());
        String kills = String.valueOf(stats != null ? stats.getKills() : 0);
        String deaths = String.valueOf(stats != null ? stats.getDeaths() : 0);
        String fishes = String.valueOf(stats != null ? stats.getFishCaught() : 0);
        String playtime = StatCategory.PLAYTIME.formatValue(stats != null ? stats.getPlaytimeMillis() : 0);

        event.renderer(((source, sourceDisplayName, message, receiver) -> {

            Component name = nickname != null
                    ? NicknameManager.getDisplayName(source.getName(), source.getUniqueId())
                    : sourceDisplayName;

            Component body = applyPlaceholders(message, kills, deaths, fishes, playtime);

            // Proximity chat applies to the message body too, not just the sender's name -
            // each existing color (including a colored nickname's own hue) fades toward gray
            // with distance instead of only the name doing so.
            if (receiver instanceof Player playerReceiver) {
                name = ComponentParser.getDistancedComponent(source.getLocation(), playerReceiver.getLocation(), name);
                body = ComponentParser.getDistancedComponent(source.getLocation(), playerReceiver.getLocation(), body);
            }

            return Component.textOfChildren(
                    team,
                    name,
                    ComponentParser.parseRichString(": "),
                    body
            );
        }));
    }

    private static Component applyPlaceholders(Component message, String kills, String deaths, String fishes, String playtime) {
        message = replace(message, "[kills]", kills);
        message = replace(message, "[deaths]", deaths);
        message = replace(message, "[fishes]", fishes);
        message = replace(message, "[playtime]", playtime);
        return message;
    }

    private static Component replace(Component message, String token, String value) {
        return message.replaceText(builder -> builder.matchLiteral(token)
                .replacement(Component.text(value, PLACEHOLDER_COLOR)));
    }
}
