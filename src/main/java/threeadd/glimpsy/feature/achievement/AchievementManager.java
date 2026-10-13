package threeadd.glimpsy.feature.achievement;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.manager.Manager;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which of the server's own {@link Achievement}s each player has earned. Everything is
 * kept in memory for every player (earned or not, they're a handful of enum values each) so the
 * award check on a hot event path never touches the database.
 */
public class AchievementManager extends Manager {

    private static final Map<UUID, Set<Achievement>> unlocked = new ConcurrentHashMap<>();

    private static boolean enabled() {
        return ConfigManager.getBoolean("achievements.enabled", true);
    }

    private static boolean broadcast() {
        return ConfigManager.getBoolean("achievements.broadcast", true);
    }

    @Override
    public void enable() {
        unlocked.putAll(AchievementRepository.getInstance().loadAllSync());
    }

    public static boolean has(UUID playerId, Achievement achievement) {
        Set<Achievement> earned = unlocked.get(playerId);
        return earned != null && earned.contains(achievement);
    }

    public static Set<Achievement> unlockedBy(UUID playerId) {
        Set<Achievement> earned = unlocked.get(playerId);
        return earned == null ? Set.of() : Collections.unmodifiableSet(earned);
    }

    /**
     * Grants an achievement if the player doesn't already have it. Safe to call from anywhere on
     * the main thread, as often as the triggering event fires - a repeat is a cheap set lookup
     * and does nothing.
     *
     * @return true only when this call is what earned it
     */
    public static boolean award(Player player, Achievement achievement) {
        return award(player.getUniqueId(), achievement);
    }

    /**
     * Same as {@link #award(Player, Achievement)}, for cases where the earning player might not
     * be online right now (e.g. an admin approving a community article while its author is
     * offline) - the achievement still unlocks and saves, it's just announced silently until they
     * next check /achievements instead of getting the toast immediately.
     *
     * @return true only when this call is what earned it
     */
    public static boolean award(UUID playerId, Achievement achievement) {
        if (!enabled()) return false;

        // Merged in one shot so two events in the same tick can't both decide they were first
        boolean[] granted = new boolean[1];
        unlocked.compute(playerId, (id, earned) -> {
            Set<Achievement> set = earned != null ? earned : EnumSet.noneOf(Achievement.class);
            granted[0] = set.add(achievement);
            return set;
        });
        if (!granted[0]) return false;

        AchievementRepository.getInstance().runAsyncSave(playerId, achievement);

        Player player = Bukkit.getPlayer(playerId);
        if (player != null) announce(player, achievement);
        return true;
    }

    private static void announce(Player player, Achievement achievement) {
        String title = PlainTextComponentSerializer.plainText().serialize(achievement.title());

        player.sendMessage(withDescription(Messages.msg("achievements.unlocked",
                "<primary>Achievement unlocked: <white>{title}", "title", title), achievement));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

        if (!broadcast()) return;

        Component message = withDescription(Messages.msg("achievements.broadcast",
                "<muted>{player} earned the achievement <white>{title}",
                "player", player.getName(), "title", title), achievement);

        for (Player other : Bukkit.getOnlinePlayers())
            if (!other.getUniqueId().equals(player.getUniqueId())) other.sendMessage(message);
    }

    /**
     * Puts the achievement's description behind a hover instead of a second chat line - the
     * announcement stays one line, and anyone curious what it took can point at it.
     */
    private static Component withDescription(Component message, Achievement achievement) {
        return message.hoverEvent(HoverEvent.showText(Component.textOfChildren(
                achievement.title(),
                Component.newline(),
                achievement.description())));
    }
}
