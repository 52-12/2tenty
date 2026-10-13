package threeadd.glimpsy.feature.achievement;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.inventory.ItemBuilder;
import threeadd.glimpsy.util.inventory.ListInventory;

import java.util.List;
import java.util.function.Consumer;

/** Every achievement, earned ones showing their own icon and locked ones greyed out. */
public class AchievementListInventory extends ListInventory<Achievement> {

    public AchievementListInventory(Player viewer) {
        super(Rows.FOUR,
                Messages.blackTitle("achievements.title", "Achievements ({earned}/{total})",
                        "earned", String.valueOf(AchievementManager.unlockedBy(viewer.getUniqueId()).size()),
                        "total", String.valueOf(Achievement.values().length)),
                List.of(Achievement.values()), Rows.FOUR.getInnerSlots(), viewer);
    }

    @Override
    protected @NotNull ItemStack mapToItem(@NotNull Achievement achievement) {
        boolean unlocked = AchievementManager.unlockedBy(getViewer().getUniqueId()).contains(achievement);

        return new ItemBuilder(unlocked ? achievement.icon() : Material.GRAY_DYE)
                .withName(achievement.title())
                .withLore(achievement.description(), Component.empty(), unlocked
                        ? Messages.msg("achievements.lore-unlocked", "<success>Unlocked")
                        : Messages.msg("achievements.lore-locked", "<muted>Not earned yet"))
                .withEnchantedStated(unlocked)
                .build();
    }

    @Override
    protected @Nullable Consumer<InventoryClickEvent> mapClick(@NotNull Achievement achievement) {
        return null; // display only
    }
}
