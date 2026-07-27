package threeadd.glimpsy.feature.simple;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.BlockType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class BloodListener implements Listener {

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        // Don't show blood for a hit that was actually absorbed by a raised shield.
        if (event.getEntity() instanceof HumanEntity human && human.isBlocking()) return;

        Location location = event.getEntity().getLocation();

        Particle.BLOCK_CRUMBLE.builder()
                .location(location)
                .count(30)
                .offset(0.25, 0.25, 0.25)
                .data(BlockType.RED_CONCRETE.createBlockData())
                .spawn();
    }
}
