package threeadd.glimpsy.feature.nametag;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threeadd.glimpsy.util.ScheduleUtil;
import threeadd.glimpsy.util.manager.ListeningManager;

import java.util.HashMap;
import java.util.Map;

public class NametagManager extends ListeningManager {

    private static final String TEAM_NAME = "glimpsy";

    private static final Map<Player, Nametag> nametags = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(NametagManager.class);

    private static Team TEAM;

    private static void createTeam() {
        NametagManager.TEAM = Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam(TEAM_NAME);
        NametagManager.TEAM.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        NametagManager.TEAM.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
    }

    private static void createNametag(Player player) {
        nametags.put(player, new Nametag(player));
    }

    private static void removeNametag(Player player) {
        nametags.get(player).remove();
        nametags.remove(player);

        player.playerListName();
    }

    public static Nametag getNametag(Player player) {
        return nametags.get(player);
    }

    @Override
    public void onEnable() {
        NametagManager.TEAM = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(TEAM_NAME);
        if (NametagManager.TEAM == null) createTeam();

        log.info("Created nametag Team");

        ScheduleUtil.scheduleSync(() ->
                        Bukkit.getOnlinePlayers().forEach(player ->
                                getNametag(player).update()),
                0,
                1,
                ScheduleUtil.Unit.SECOND);
    }

    @Override
    public void disable() {
        new HashMap<>(nametags).forEach((player, nametag) ->
                removeNametag(player));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        NametagManager.TEAM.addPlayer(player);
        createNametag(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeNametag(event.getPlayer());
    }
}
