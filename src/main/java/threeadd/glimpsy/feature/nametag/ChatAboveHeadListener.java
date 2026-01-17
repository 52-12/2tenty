package threeadd.glimpsy.feature.nametag;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.joml.Vector3f;
import threeadd.glimpsy.util.ScheduleUtil;

public class ChatAboveHeadListener implements Listener {

    // Shouldn't be run async (accessing the main thread)
    private static void handle(AsyncChatEvent event) {
        Player player = event.getPlayer();

        Component chatIcon = Component.text('✉')
                .color(TextColor.color(0x70D7FF));

        Nametag nametag = NametagManager.getNametag(player);

        nametag.addFadingLine(Component.textOfChildren(
                                chatIcon, Component.space(), event.originalMessage()),
                        TextColor.color(0xE0E0E0),
                        TextColor.color(0x5A5A5A),
                        5,
                        ScheduleUtil.Unit.SECOND)
                .setLocalOffset(0.35f)
                .setScale(new Vector3f(1.5f, 1.5f, 1.5f));

        nametag.rebuild();
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (event.isAsynchronous()) {
            ScheduleUtil.scheduleSync(() -> handle(event)); // Next tick on main thread
            return;
        }

        handle(event);
    }
}
