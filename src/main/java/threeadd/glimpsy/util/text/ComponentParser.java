package threeadd.glimpsy.util.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ComponentParser {

    public static Component parseRichString(String rawString) {
        return Component.textOfChildren(
                ComponentWrapper.wrapComponent(
                        MiniMessage.miniMessage().deserialize(rawString)
                                .applyFallbackStyle(Style.style()
                                        .colorIfAbsent(TextColor.color(0xC4C4C4)) // Lighter color than default 0xFFFFFF, nicer on the eyes
                                        .build()),

                        75).toArray(new ComponentLike[]{}));
    }

    public static Component parseBoolean(boolean state) {
        if (state) {
            return Component.text("Yes")
                    .color(TextColor.color(0x63C14F));
        } else {
            return Component.text("No")
                    .color(TextColor.color(0xC14F4F));
        }
    }

    public static Component parseLocalDateTime(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        List<String> parts = extractPartsFromDateTime(dateTime, now);

        String result;
        if (parts.isEmpty()) {
            result = "0s";
        } else if (parts.size() == 1) {
            result = parts.getFirst();
        } else if (parts.size() == 2) {
            result = parts.get(0) + " and " + parts.get(1);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.size(); i++) {
                if (i == parts.size() - 1) {
                    sb.append("and ").append(parts.get(i));
                } else {
                    sb.append(parts.get(i)).append(i == parts.size() - 2 ? " " : ", ");
                }
            }
            result = sb.toString();
        }

        return Component.text(result);
    }


    private static @NotNull List<String> extractPartsFromDateTime(LocalDateTime dateTime, LocalDateTime now) {
        LocalDateTime from = dateTime.isBefore(now) ? dateTime : now;
        LocalDateTime to = dateTime.isBefore(now) ? now : dateTime;

        long years = from.until(to, ChronoUnit.YEARS);
        from = from.plusYears(years);

        long months = from.until(to, ChronoUnit.MONTHS);
        from = from.plusMonths(months);

        long days = from.until(to, ChronoUnit.DAYS);
        from = from.plusDays(days);

        long hours = from.until(to, ChronoUnit.HOURS);
        from = from.plusHours(hours);

        long minutes = from.until(to, ChronoUnit.MINUTES);
        from = from.plusMinutes(minutes);

        long seconds = from.until(to, ChronoUnit.SECONDS);

        Map<String, Long> units = new LinkedHashMap<>();
        units.put("y", years);
        units.put("mo", months);
        units.put("d", days);
        units.put("h", hours);
        units.put("m", minutes);
        units.put("s", seconds);

        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Long> e : units.entrySet()) {
            if (e.getValue() > 0) {
                parts.add(e.getValue() + e.getKey());
            }
        }

        return parts;
    }

    public static Component getDistancedString(Location locationA, Location locationB, String string) {
        double distanceBetweenPlayers = locationA.distance(locationB);

        TextColor nearColor = TextColor.color(0xFFF8EB);
        TextColor farColor = TextColor.color(0x4B4B4B);

        double maxFadeDistance = 300D;
        double fadeAmount = Math.min(distanceBetweenPlayers / maxFadeDistance, 1.0);

        TextColor finalColor = interpolateColor(nearColor, farColor, fadeAmount);

        return Component.text(string, finalColor);
    }

    public static TextColor interpolateColor(TextColor start, TextColor end, double fraction) {
        double t = Math.max(0, Math.min(1, fraction));

        int red = (int) (start.red() + (end.red() - start.red()) * t);
        int green = (int) (start.green() + (end.green() - start.green()) * t);
        int blue = (int) (start.blue() + (end.blue() - start.blue()) * t);

        return TextColor.color(red, green, blue);
    }
}
