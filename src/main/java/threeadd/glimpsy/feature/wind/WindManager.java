package threeadd.glimpsy.feature.wind;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import threeadd.glimpsy.util.ScheduleUtil;
import threeadd.glimpsy.util.manager.Manager;
import threeadd.glimpsy.util.text.ComponentParser;

public class WindManager extends Manager {

    private static Wind currentWind = Wind.getRandom();

    public static Component getWindArrow(Player player) {
        Component directionComponent = Component.text(currentWind.getRelativeArrow(player))
                .color(TextColor.color(0xC4C4C4));

        if (currentWind.isInWind(player))
            directionComponent = directionComponent.color(TextColor.color(0x3DC9FF));

        return directionComponent;
    }

    public static Wind getCurrentWind() {
        return currentWind;
    }

    @Override
    public void enable() {

        ScheduleUtil.scheduleSync(() -> {
            WindManager.currentWind = Wind.getRandom();

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(ComponentParser.parseRichString(
                                "The wind has changed to " +
                                        WindManager.currentWind + " (")
                        .append(WindManager.getWindArrow(player))
                        .append(ComponentParser.parseRichString(")"))
                );
            }
        }, 0, 15, ScheduleUtil.Unit.MINUTE);

    }
}
