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
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import threeadd.glimpsy.util.ScheduleUtil;
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
                .append(Component.text("GLIMPSY")
                        .decorate(TextDecoration.BOLD)
                        .color(TextColor.color(0x2881C6)))
                .append(PADDING).appendNewline()
                .append(Component.text("by 3add")
                        .color(TextColor.color(0xC4C4C4)))
                .appendNewline();
    }

    private static Component createFooter(double tps, int playerCount) {
        String formattedTps = formatTps(tps);
        String players = String.valueOf(playerCount);

        TextColor labelColor = TextColor.color(0xBCBCBC);
        TextColor valueColor = TextColor.color(0x79ADD6);
        TextColor urlColor = TextColor.color(0x9F9F9F);

        Component statusLine = Component.empty()
                .append(Component.text("TPS ").color(labelColor))
                .append(Component.text(formattedTps).color(valueColor))
                .append(Component.text("   Players ").color(labelColor))
                .append(Component.text(players).color(valueColor));

        Component url = Component.text("Glimpsy.MineHut.gg")
                .color(urlColor);

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
