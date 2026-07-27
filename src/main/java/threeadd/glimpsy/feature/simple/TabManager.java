package threeadd.glimpsy.feature.simple;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.Action;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.PlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerListHeaderAndFooter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import threeadd.glimpsy.feature.nickname.NicknameManager;
import threeadd.glimpsy.feature.teams.TeamManager;
import threeadd.glimpsy.util.ScheduleUtil;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.manager.ListeningManager;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// Because tab updates happen so often for every player, I tried optimizing this class as much as I could.
// Almost all async safe data is handled async.

public class TabManager extends ListeningManager {

    private static final Component PADDING = Component.text("                  ");
    private static final EnumSet<Action> UPDATE_ACTIONS = EnumSet.of(
            Action.UPDATE_GAME_MODE,
            Action.UPDATE_HAT,
            Action.UPDATE_LATENCY,
            Action.UPDATE_DISPLAY_NAME,
            Action.UPDATE_LIST_ORDER,
            Action.UPDATE_LISTED
    );

    private final Map<UUID, User> userMap = new ConcurrentHashMap<>();

    private static Component createHeader() {
        return Component.newline()
                .append(PADDING)
                .append(Messages.msg("tab.header-title", "<b><#2881C6>2TENTY</b>"))
                .append(PADDING).appendNewline()
                .append(Messages.msg("tab.header-subtitle", "<#C4C4C4>by 3upman"))
                .appendNewline();
    }

    private static Component createFooter(double tps, int playerCount) {
        Component statusLine = Messages.msg("tab.footer-status",
                "<#BCBCBC>TPS <#79ADD6>{tps}   <#BCBCBC>Players <#79ADD6>{players}",
                "tps", formatTps(tps), "players", String.valueOf(playerCount));

        Component url = Messages.msg("tab.footer-url", "<#9F9F9F>2tenty.MineHut.gg");

        return Component.newline()
                .append(statusLine)
                .appendNewline()
                .append(url)
                .appendNewline();
    }

    private static String formatTps(double tpsValue) {
        String formattedTps = String.format(Locale.US, "%.2f", tpsValue);

        if (formattedTps.endsWith(".00")) {
            return formattedTps.substring(0, formattedTps.length() - 3);
        }

        return formattedTps;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        userMap.put(player.getUniqueId(), PacketEvents.getAPI().getPlayerManager().getUser(player));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        userMap.remove(player.getUniqueId());
    }

    @Override
    public void onEnable() {
        ScheduleUtil.scheduleSync(this::tickTabList, 0, 1, ScheduleUtil.Unit.SECOND);
    }

    // Header and Footer

    private void tickTabList() {
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        if (onlinePlayers.isEmpty()) return;

        List<PlayerSnapshot> snapshots = new ArrayList<>(onlinePlayers.size());

        for (Player player : onlinePlayers) {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(player);

            snapshots.add(new PlayerSnapshot(
                    player.getUniqueId(),
                    user.getProfile(),
                    player.getLocation(),
                    player.getPing(),
                    GameMode.valueOf(player.getGameMode().name()),
                    player.getName()
            ));
        }

        double currentTps = Bukkit.getTPS()[0];
        int playerCount = onlinePlayers.size();

        ScheduleUtil.scheduleAsync(() -> processUpdates(snapshots, currentTps, playerCount));
    }

    private void processUpdates(List<PlayerSnapshot> snapshots, double tps, int playerCount) {

        WrapperPlayServerPlayerListHeaderAndFooter headerFooterPacket = new WrapperPlayServerPlayerListHeaderAndFooter(
                createHeader(),
                createFooter(tps, playerCount)
        );

        for (PlayerSnapshot viewer : snapshots) {
            List<PlayerInfo> packetEntries = new ArrayList<>(snapshots.size()); // list for this viewer

            for (PlayerSnapshot target : snapshots) {
                Component displayName;
                int order;

                if (viewer.uuid().equals(target.uuid())) {
                    displayName = Component.text(target.name())
                            .color(TextColor.color(0xFF9393))
                            .decorate(TextDecoration.UNDERLINED);
                    order = 1;
                } else {
                    double distance;

                    if (viewer.location().getWorld() == target.location().getWorld()) {
                        distance = viewer.location().distance(target.location());
                        displayName = ComponentParser.getDistancedString(target.location(), viewer.location(), target.name());
                    } else {
                        // if different worlds treat as very far away
                        distance = Integer.MAX_VALUE;
                        displayName = Component.text(target.name()).color(TextColor.color(0x4B4B4B));
                    }

                    order = Integer.MAX_VALUE - (int) distance; // reverse order
                }

                String nickname = NicknameManager.getNickname(target.uuid());
                if (nickname != null)
                    displayName = NicknameManager.getDisplayName(target.name(), target.uuid());

                String teamName = TeamManager.getTeamName(target.uuid());
                if (teamName != null) {
                    displayName = Component.textOfChildren(
                            Component.text("[", NamedTextColor.GRAY),
                            MiniMessage.miniMessage().deserialize(teamName),
                            Component.text("] ", NamedTextColor.GRAY),
                            displayName
                    );
                }

                packetEntries.add(new PlayerInfo(
                        target.profile(),
                        true,
                        target.ping(),
                        target.gameMode(),
                        displayName,
                        null,
                        order
                ));
            }

            WrapperPlayServerPlayerInfoUpdate infoPacket = new WrapperPlayServerPlayerInfoUpdate(UPDATE_ACTIONS, packetEntries);

            User viewingUser = userMap.get(viewer.uuid());
            if (viewingUser == null) continue; // disconnected between the snapshot and this async send

            viewingUser.sendPacket(infoPacket);
            viewingUser.sendPacket(headerFooterPacket);
        }
    }

    // async safe way of representing a player on tab
    private record PlayerSnapshot(
            UUID uuid,
            UserProfile profile,
            Location location,
            int ping,
            GameMode gameMode,
            String name
    ) {
    }
}
