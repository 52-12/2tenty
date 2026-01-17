package threeadd.glimpsy.feature.wind;

import net.kyori.adventure.sound.Sound;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class WindFishingListener implements Listener {

    private static final NamespacedKey WIND_LURE_KEY = Objects.requireNonNull(NamespacedKey.fromString("glimpsy:wind_lure"));
    private static final Logger log = LoggerFactory.getLogger(WindFishingListener.class);

    private static void handleFishing(PlayerFishEvent event) {
        Player player = event.getPlayer();
        Wind currentWind = WindManager.getCurrentWind();

        decrementIfPresent(player, event); // remove prev if present

        if (currentWind.isInWind(player)) {
            Sound sound = Sound.sound(NamespacedKey.minecraft("block.note_block.bell"), Sound.Source.AMBIENT, 1f, 1f);
            player.playSound(sound);

            ItemStack rod = getRod(player, event);
            rod.editPersistentDataContainer(container ->
                    container.set(WIND_LURE_KEY, PersistentDataType.BOOLEAN, true));

            int lureLevel = rod.getEnchantmentLevel(Enchantment.LURE);
            rod.addUnsafeEnchantment(Enchantment.LURE, lureLevel + 1);
        }

        player.sendActionBar(WindManager.getWindArrow(player));
    }

    private static void handleFailed(PlayerFishEvent event) {
        Player player = event.getPlayer();

        decrementIfPresent(player, event);
    }

    private static void decrementIfPresent(Player player, PlayerFishEvent event) {
        ItemStack rod = getRod(player, event);

        if (!rod.getPersistentDataContainer().has(WIND_LURE_KEY)) return;

        rod.editPersistentDataContainer(container ->
                container.remove(WIND_LURE_KEY));

        int lureLevel = rod.getEnchantmentLevel(Enchantment.LURE);
        if (lureLevel == 1) {
            rod.removeEnchantment(Enchantment.LURE);
            return;
        }

        rod.addUnsafeEnchantment(Enchantment.LURE, lureLevel - 1);
    }

    private static ItemStack getRod(Player player, PlayerFishEvent event) {
        EquipmentSlot hand = event.getHand();
        if (hand == null)
            throw new IllegalStateException("No hand in this fish event " + event);

        return player.getEquipment().getItem(hand);
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {

        if (event.getState().equals(PlayerFishEvent.State.FISHING))
            handleFishing(event);
        else if (event.getState().equals(PlayerFishEvent.State.CAUGHT_FISH)
                || event.getState().equals(PlayerFishEvent.State.REEL_IN))

            handleFailed(event);
    }
}
