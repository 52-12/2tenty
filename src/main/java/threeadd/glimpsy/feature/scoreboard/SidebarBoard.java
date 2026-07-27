package threeadd.glimpsy.feature.scoreboard;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A single player's sidebar, built on the flicker-free "team prefix" pattern: each line is a
 * unique invisible score entry whose text never changes, with the actual content held in that
 * entry's team prefix (updated in place, so lines never flash).
 * <p>
 * Since this replaces the player's active scoreboard, it also mirrors {@code NametagManager}'s
 * "2tenty" team (collision + default-nametag hiding) so those rules keep working - otherwise a
 * player on this board would get vanilla nametags and collisions back.
 */
@SuppressWarnings("UnstableApiUsage")
class SidebarBoard {

    private static final String NAMETAG_TEAM = "2tenty"; // must match NametagManager#TEAM_NAME

    // Unique, effectively-invisible score-holder strings (a colour code + reset renders nothing).
    private static final String[] ENTRIES = buildEntries(ScoreboardLines.LINE_COUNT);

    private final UUID ownerId;
    private final Scoreboard scoreboard;
    private final Objective objective;
    private final Team[] lineTeams;
    private final Team nametagTeam;
    private final Set<String> nametagMembers = new HashSet<>();

    SidebarBoard(Player player) {
        this.ownerId = player.getUniqueId();
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        this.objective = scoreboard.registerNewObjective("2tenty_sb", Criteria.DUMMY, ScoreboardLines.title());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank()); // hide the red score numbers

        this.lineTeams = new Team[ENTRIES.length];
        for (int i = 0; i < ENTRIES.length; i++) {
            Team team = scoreboard.registerNewTeam("line" + i);
            team.addEntry(ENTRIES[i]);
            objective.getScore(ENTRIES[i]).setScore(ENTRIES.length - i); // descending -> line 0 on top
            lineTeams[i] = team;
        }

        this.nametagTeam = scoreboard.registerNewTeam(NAMETAG_TEAM);
        nametagTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        nametagTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        syncNametagMembers();

        player.setScoreboard(scoreboard);
    }

    void update(Player player) {
        objective.displayName(ScoreboardLines.title());

        List<Component> lines = ScoreboardLines.build(player);
        for (int i = 0; i < lineTeams.length; i++)
            lineTeams[i].prefix(i < lines.size() ? lines.get(i) : Component.empty());

        syncNametagMembers();
    }

    // Keeps this board's mirrored nametag team in step with who's online, adding/removing only on
    // real changes (so we don't churn collision/nametag packets every tick).
    private void syncNametagMembers() {
        Set<String> online = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) online.add(p.getName());

        for (String name : online)
            if (nametagMembers.add(name)) nametagTeam.addEntry(name);

        nametagMembers.removeIf(name -> {
            if (online.contains(name)) return false;
            nametagTeam.removeEntry(name);
            return true;
        });
    }

    // Puts the player back on the main scoreboard (where NametagManager's own team lives)
    void clear() {
        Player player = Bukkit.getPlayer(ownerId);
        if (player != null)
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    private static String[] buildEntries(int count) {
        String[] entries = new String[count];
        for (int i = 0; i < count; i++)
            entries[i] = "§" + Integer.toHexString(i % 16) + "§r";
        return entries;
    }
}
