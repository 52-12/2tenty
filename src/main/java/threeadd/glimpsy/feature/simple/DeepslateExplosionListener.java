package threeadd.glimpsy.feature.simple;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Set;

/**
 * Deepslate is craftable now (see DeepslateRecipe), so it's worth making it survive explosions -
 * TNT, creepers, beds/anchors in the wrong dimension, etc. - instead of vanilla's fairly average
 * blast resistance. TNT itself doesn't go through these events at all (TNTTrailListener cancels
 * them and does its own block breaking), so its resistance is handled separately there.
 */
public class DeepslateExplosionListener implements Listener {

    private static final Set<Material> PROTECTED = Set.of(
            Material.DEEPSLATE,
            Material.COBBLED_DEEPSLATE_SLAB,
            Material.POLISHED_DEEPSLATE_SLAB,
            Material.DEEPSLATE_BRICK_SLAB,
            Material.DEEPSLATE_TILE_SLAB
    );

    public static boolean isProtected(Material material) {
        return PROTECTED.contains(material);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> isProtected(block.getType()));
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> isProtected(block.getType()));
    }
}
