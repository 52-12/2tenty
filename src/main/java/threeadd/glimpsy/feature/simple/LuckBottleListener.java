package threeadd.glimpsy.feature.simple;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.PotionContents;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import threeadd.glimpsy.util.inventory.ItemBuilder;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class LuckBottleListener implements Listener {

    private static final Map<Material, Double> luckMap = Map.of(
            Material.COD, 0.2,
            Material.PUFFERFISH, 0.5,
            Material.TROPICAL_FISH, 0.8
    );

    private static boolean isLuckApplicable(ItemStack item) {
        return luckMap.containsKey(item.getType());
    }

    @SuppressWarnings("UnstableApiUsage")
    private static boolean isWaterBottle(ItemStack item) {
        if (!item.getType().equals(Material.POTION)
                || !item.hasData(DataComponentTypes.POTION_CONTENTS))
            return false;

        PotionContents contents = item.getData(DataComponentTypes.POTION_CONTENTS);
        if (contents == null || contents.potion() == null)
            throw new IllegalStateException("Found data but can't extract " + item);

        return contents.potion().equals(PotionType.WATER);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack hoverItem = event.getCursor();

        if (clickedItem == null)
            return;

        if (!isLuckApplicable(hoverItem)) return;
        if (!isWaterBottle(clickedItem)) return;

        Random random = ThreadLocalRandom.current();
        if (!(random.nextDouble() < luckMap.get(hoverItem.getType()))) {
            Sound sound = Sound.sound(Key.key("minecraft:ui.button.click"), Sound.Source.AMBIENT, 1, 0.1F);
            event.getView().getPlayer().playSound(sound, Sound.Emitter.self());

            event.setCurrentItem(ItemStack.of(Material.GLASS_BOTTLE));
            return;
        }

        event.getCursor().subtract(1);
        event.setCurrentItem(new ItemBuilder(Material.POTION)
                .withItemMeta(PotionMeta.class, meta ->
                        meta.setBasePotionType(PotionType.LUCK))
                .build());
        Sound sound = Sound.sound(Key.key("minecraft:block.brewing_stand.brew"), Sound.Source.AMBIENT, 1, 1);
        event.getView().getPlayer().playSound(sound, Sound.Emitter.self());
    }
}
