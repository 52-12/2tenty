package threeadd.glimpsy.feature.simple;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.PotionContents;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;

import java.util.concurrent.ThreadLocalRandom;

public class MudClayListener implements Listener {

    @SuppressWarnings("UnstableApiUsage")
    private static boolean isWaterBottle(ItemStack item) {
        if (item.getType() != Material.POTION || !item.hasData(DataComponentTypes.POTION_CONTENTS)) return false;

        PotionContents contents = item.getData(DataComponentTypes.POTION_CONTENTS);
        return contents != null && PotionType.WATER.equals(contents.potion());
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!isWaterBottle(event.getItem())) return;

        Block target = event.getPlayer().getTargetBlockExact(4);
        if (target == null) return;

        switch (target.getType()) {
            case CLAY, MUD -> event.setCancelled(true);
            default -> {}
        }
    }

    @EventHandler
    public void onMud(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.MUD) return;

        ItemStack used = event.getItem();
        if (used == null || !isWaterBottle(used)) return;

        Player player = event.getPlayer();
        if (player.getCooldown(Material.GLASS_BOTTLE) > 0) return;
        player.setCooldown(Material.GLASS_BOTTLE, 5);

        used.setType(Material.GLASS_BOTTLE);
        block.setType(Material.CLAY);

        player.swingMainHand();
        player.playSound(player, Sound.ITEM_BOTTLE_EMPTY, 1.0F, 1.0F);
        player.playSound(block.getLocation(), Sound.BLOCK_GRAVEL_BREAK, 0.5F, 0.5F);
        player.playSound(block.getLocation(), Sound.ITEM_DYE_USE, 0.5F, 0.5F);
    }

    @EventHandler
    public void onClay(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CLAY) return;

        ItemStack used = event.getItem();
        if (used == null || !isWaterBottle(used)) return;

        Player player = event.getPlayer();
        if (player.getCooldown(Material.GLASS_BOTTLE) > 0) return;
        player.setCooldown(Material.GLASS_BOTTLE, 5);

        used.setType(Material.GLASS_BOTTLE);
        block.setType(ThreadLocalRandom.current().nextInt(100) <= 40 ? Material.SUSPICIOUS_SAND : Material.SAND);

        player.swingMainHand();
        player.playSound(player, Sound.ITEM_BOTTLE_EMPTY, 1.0F, 1.0F);
        player.playSound(block.getLocation(), Sound.BLOCK_GRAVEL_BREAK, 0.5F, 0.5F);
        player.playSound(block.getLocation(), Sound.ITEM_DYE_USE, 0.5F, 0.5F);
    }
}
