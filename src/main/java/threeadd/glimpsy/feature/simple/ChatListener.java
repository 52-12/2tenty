package threeadd.glimpsy.feature.simple;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import threeadd.glimpsy.feature.nickname.NicknameManager;
import threeadd.glimpsy.feature.stats.PlayerStats;
import threeadd.glimpsy.feature.stats.StatCategory;
import threeadd.glimpsy.feature.stats.StatsManager;
import threeadd.glimpsy.feature.teams.TeamManager;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener implements Listener {

    private static final TextColor MENTION_COLOR = TextColor.color(0xFFD54F);

    // Cached across messages - only rebuilt when the online roster changes, instead of compiling a
    // regex per receiver per message.
    private static volatile String mentionCacheKey;
    private static volatile Pattern mentionCachePattern;

    @EventHandler
    public void onChat(AsyncChatEvent event) {

        // Using viewerUnaware renderer boosts performance, see https://jd.papermc.io/paper/1.21.11-pre3/io/papermc/paper/chat/ChatRenderer.ViewerUnaware.html
        // Unfortunately we do need a viewer (to measure distance) and thus I just wasted your time :)

        // Drop the message entirely for anyone whose /chatdropoff range excludes the sender.
        Player sender = event.getPlayer();
        event.viewers().removeIf(viewer -> {
            if (!(viewer instanceof Player recipient) || recipient.getUniqueId().equals(sender.getUniqueId()))
                return false; // keep the console, and the sender always sees their own message

            double cutoff = ChatDropoffManager.getCutoff(recipient);
            if (cutoff <= 0) return false;
            return recipient.getWorld() != sender.getWorld()
                    || recipient.getLocation().distance(sender.getLocation()) > cutoff;
        });

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

        // Built once (they don't depend on the viewer) - each is a labelled value like "Kills: 12".
        Component killsComp = Messages.msg("chat.placeholder-kills", "<#FFE08F>[Kills : {value}]", "value", kills);
        Component deathsComp = Messages.msg("chat.placeholder-deaths", "<#FFE08F>[Deaths : {value}]", "value", deaths);
        Component fishesComp = Messages.msg("chat.placeholder-fishes", "<#FFE08F>[Fishes : {value}]", "value", fishes);
        Component playtimeComp = Messages.msg("chat.placeholder-playtime", "<#FFE08F>[Playtime : {value}]", "value", playtime);

        // Built once per message (not per receiver), and cached while the roster is unchanged.
        Pattern mentions = mentionPattern();

        event.renderer(((source, sourceDisplayName, message, receiver) -> {

            Component name = nickname != null
                    ? NicknameManager.getDisplayName(source.getName(), source.getUniqueId())
                    : sourceDisplayName;

            Component body = applyPlaceholders(message, killsComp, deathsComp, fishesComp, playtimeComp);

            // Proximity chat applies to the message body too, not just the sender's name - each
            // existing color (including a colored nickname's own hue) fades toward gray with
            // distance. It's opt-in per player (and off entirely unless enabled in config), so
            // most receivers just see normal full-color chat. @mentions are re-highlighted after
            // the fade so they never gray out.
            if (receiver instanceof Player playerReceiver && ProximityChat.isEnabledFor(playerReceiver)) {
                name = ComponentParser.getDistancedComponent(source.getLocation(), playerReceiver.getLocation(), name);
                body = ComponentParser.getDistancedComponent(source.getLocation(), playerReceiver.getLocation(), body);

                if (mentions != null) body = highlightMentions(body, mentions);
            }

            return Component.textOfChildren(
                    team,
                    name,
                    ComponentParser.parseRichString(": "),
                    body
            );
        }));
    }

    private static Component applyPlaceholders(Component message, Component kills, Component deaths, Component fishes, Component playtime) {
        message = replace(message, "[kills]", kills);
        message = replace(message, "[deaths]", deaths);
        message = replace(message, "[fishes]", fishes);
        message = replace(message, "[playtime]", playtime);
        return message;
    }

    private static Component replace(Component message, String token, Component replacement) {
        return message.replaceText(builder -> builder.matchLiteral(token).replacement(replacement));
    }

    // "@" followed by any online player's name (nicknames are just styled real usernames, so the
    // plain name is what's typed). Cached while the roster is unchanged so we don't recompile a
    // regex on every message.
    private static Pattern mentionPattern() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers())
            names.add(player.getName());

        if (names.isEmpty()) return null;

        names.sort(String.CASE_INSENSITIVE_ORDER);
        String key = String.join(",", names);

        Pattern cached = mentionCachePattern;
        if (cached != null && key.equals(mentionCacheKey)) return cached;

        Pattern built = Pattern.compile("@(?:" + String.join("|", names.stream().map(Pattern::quote).toList()) + ")\\b",
                Pattern.CASE_INSENSITIVE);
        mentionCacheKey = key;
        mentionCachePattern = built;
        return built;
    }

    // Re-colours any @mention in the tree, overriding the distance fade applied just before it.
    // The already-processed original children are re-attached after the split parts - the mention
    // pieces we create must NOT be fed back through this method, or "@name" matches itself and the
    // recursion never ends (StackOverflowError).
    private static Component highlightMentions(Component component, Pattern pattern) {
        List<Component> processedChildren = new ArrayList<>(component.children().size());
        for (Component child : component.children())
            processedChildren.add(highlightMentions(child, pattern));

        if (!(component instanceof TextComponent text) || text.content().isEmpty()
                || !pattern.matcher(text.content()).find()) {
            return processedChildren.isEmpty() ? component : component.children(processedChildren);
        }

        TextComponent.Builder builder = Component.text().style(text.style());

        String content = text.content();
        Matcher matcher = pattern.matcher(content);
        int last = 0;
        while (matcher.find()) {
            if (matcher.start() > last)
                builder.append(Component.text(content.substring(last, matcher.start())));

            builder.append(Component.text(matcher.group()).color(MENTION_COLOR));
            last = matcher.end();
        }
        if (last < content.length())
            builder.append(Component.text(content.substring(last)));

        for (Component child : processedChildren)
            builder.append(child);

        return builder.build();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ChatDropoffManager.clear(event.getPlayer());
    }
}
