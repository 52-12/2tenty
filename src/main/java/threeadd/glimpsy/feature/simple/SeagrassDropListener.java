package threeadd.glimpsy.feature.simple;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import threeadd.glimpsy.util.config.ConfigManager;

import java.util.Iterator;

/**
 * Makes seagrass unobtainable. Shearing (or silk-touching) seagrass/tall seagrass is the only
 * vanilla source of the seagrass item - breaking it any other way already drops nothing - so
 * stripping the item from that one drop path covers every acquisition route.
 */
public class SeagrassDropListener implements Listener {

    private static boolean obtainable() {
        return ConfigManager.getBoolean("seagrass.obtainable", false);
    }

    @EventHandler
    public void onBlockDrop(BlockDropItemEvent event) {
        if (obtainable()) return;

        Material type = event.getBlockState().getType();
        if (type != Material.SEAGRASS && type != Material.TALL_SEAGRASS) return;

        Iterator<org.bukkit.entity.Item> iterator = event.getItems().iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next().getItemStack();
            if (item.getType() == Material.SEAGRASS) iterator.remove();
        }
    }
}
