package threeadd.glimpsy.feature.wind;

import net.kyori.adventure.sound.Sound;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threeadd.glimpsy.feature.achievement.Achievement;
import threeadd.glimpsy.feature.achievement.AchievementManager;

import java.util.Objects;

public class WindFishingListener implements Listener {

    private static final NamespacedKey WIND_LURE_KEY = Objects.requireNonNull(NamespacedKey.fromString("2tenty:wind_lure"));
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

            AchievementManager.award(player, Achievement.WIND_MASTER);
        }

        player.sendActionBar(WindManager.getWindArrow(player));
    }

    private static void handleFailed(PlayerFishEvent event) {
        Player player = event.getPlayer();

        decrementIfPresent(player, event);
    }

    private static void decrementIfPresent(Player player, PlayerFishEvent event) {
        stripWindBoost(getRod(player, event));
    }

    /**
     * Removes the free wind Lure (the {@code wind_lure} tag plus the one bonus Lure level it added).
     * The boost is only ever meant to last while the rod is held, so every hand-off path calls this -
     * otherwise a boosted rod could be dropped, stashed, traded, or carried across a restart with the
     * extra Lure intact (see issue #4).
     */
    private static void stripWindBoost(ItemStack rod) {
        if (rod == null || !rod.getPersistentDataContainer().has(WIND_LURE_KEY, PersistentDataType.BOOLEAN)) return;

        rod.editPersistentDataContainer(container -> container.remove(WIND_LURE_KEY));

        int lureLevel = rod.getEnchantmentLevel(Enchantment.LURE);
        if (lureLevel <= 1) {
            rod.removeEnchantment(Enchantment.LURE);
            return;
        }

        rod.addUnsafeEnchantment(Enchantment.LURE, lureLevel - 1);
    }

    private static void stripInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents())
            stripWindBoost(item);

        stripWindBoost(player.getInventory().getItemInOffHand());
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

    // --- Hand-off guards: the boost dies the moment the rod leaves the player's hand ---

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        stripWindBoost(event.getItemDrop().getItemStack());
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        stripWindBoost(event.getItem().getItemStack());
    }

    @EventHandler
    public void onHeldSwitch(PlayerItemHeldEvent event) {
        stripWindBoost(event.getPlayer().getInventory().getItem(event.getPreviousSlot()));
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        stripWindBoost(event.getMainHandItem()); // the main-hand rod is the one moving to the off hand
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        stripWindBoost(event.getCurrentItem());
        stripWindBoost(event.getCursor());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        stripWindBoost(event.getOldCursor());
        event.getNewItems().values().forEach(WindFishingListener::stripWindBoost);
    }

    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent event) {
        stripWindBoost(event.getItem()); // hoppers and the like
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stripInventory(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        stripInventory(event.getEntity());
        event.getDrops().forEach(WindFishingListener::stripWindBoost);
    }
}
