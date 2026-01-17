package threeadd.glimpsy.feature.competition.view;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.feature.competition.Competition;
import threeadd.glimpsy.feature.competition.CompetitionManager;
import threeadd.glimpsy.util.inventory.CustomInventory;
import threeadd.glimpsy.util.inventory.ItemBuilder;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CompetitionInfoInventory extends CustomInventory {

    public CompetitionInfoInventory(Player viewer) {
        super(Rows.THREE, Component.text("Competitions"), viewer);

        setUpdatingSlot(13,
                this::getItem,
                () -> null,
                1, TimeUnit.SECONDS);
    }

    public ItemStack getItem() {
        @Nullable Competition competition = CompetitionManager.getCompetition();
        boolean isActive = competition != null;

        if (isActive) {
            int limit = 10; // show top 10 players

            List<Component> lines = new ArrayList<>();

            int count = 0;
            for (Competition.PlayerEntry playerEntry : competition.getSortedEntries()) {
                if (count >= limit) break;

                Component line = ComponentParser.parseRichString("#" + playerEntry.position() + " " + playerEntry.player().getName() + " with " + playerEntry.score())
                        .appendSpace()
                        .append(competition.getIdentifier());
                lines.add(line);

                count++;
            }

            lines.add(Component.empty());

            Player viewer = getViewer();

            UUID viewerId = viewer.getUniqueId();
            Competition.PlayerEntry viewingEntry = competition.getEntry(viewerId);
            if (viewingEntry != null)
                lines.add(Component.text("#" + viewingEntry.position() + " You -> " + " with " + viewingEntry.score())
                        .appendSpace()
                        .append(competition.getIdentifier()));

            lines.addFirst(Component.text("Ends in: ").append(ComponentParser.parseLocalDateTime(competition.getEndTime())));
            lines.add(1, Component.empty());

            return new ItemBuilder(Material.PAPER)
                    .withName(competition.getIdentifier().appendSpace().append(ComponentParser.parseRichString("Competition")))
                    .withLore(lines)
                    .build();
        } else {
            return new ItemBuilder(Material.RED_DYE)
                    .withName(Component.text("No Running competition"))
                    .withLore(Component.text("Next Competition ").append(ComponentParser.parseLocalDateTime(CompetitionManager.getNextCompetitionTime())))
                    .build();
        }
    }
}
