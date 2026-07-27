package threeadd.glimpsy.feature.teams;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player "team chat mode" toggle - while enabled, a player's normal chat messages are
 * redirected to their team instead of being sent publicly (see TeamChatListener). Session-only,
 * not persisted - resets to off on rejoin.
 */
public class TeamChatManager {

    private static final Set<UUID> enabled = ConcurrentHashMap.newKeySet();

    public static boolean isEnabled(Player player) {
        return enabled.contains(player.getUniqueId());
    }

    /**
     * @return the new state (true if team chat is now enabled)
     */
    public static boolean toggle(Player player) {
        UUID id = player.getUniqueId();
        if (enabled.remove(id)) return false;

        enabled.add(id);
        return true;
    }

    public static void onQuit(Player player) {
        enabled.remove(player.getUniqueId());
    }

    /**
     * @return true if the message was sent (the sender is on a team), false otherwise
     */
    public static boolean send(Player sender, String plainMessage) {
        TeamData team = TeamManager.getTeam(sender);
        if (team == null) return false;

        Component formatted = Messages.msg("teams.chat-prefix", "<ally>[Team] </ally>{player}: ", "player", sender.getName())
                .append(ComponentParser.parseRichString(plainMessage));

        for (UUID member : team.getMembers()) {
            Player player = Bukkit.getPlayer(member);
            if (player != null) player.sendMessage(formatted);
        }

        return true;
    }
}
