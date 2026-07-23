package threeadd.glimpsy.feature.simple;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Boat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;

public class BedrockBoatListener implements Listener {

    private void removeIfOnBedrock(Boat boat) {
        // A boat resting exactly on a block's top surface can floor to either that block or
        // the one above it depending on sub-pixel placement, so check both possible
        // boundaries instead of just "the block below".
        Block at = boat.getLocation().getBlock();
        Block below = at.getRelative(BlockFace.DOWN);

        if (at.getType() != Material.BEDROCK && below.getType() != Material.BEDROCK) return;

        boat.remove();
    }

    @EventHandler
    public void onCreate(VehicleCreateEvent event) {
        if (event.getVehicle() instanceof Boat boat)
            removeIfOnBedrock(boat);
    }

    // Catches boats that get paddled or pushed onto bedrock after being placed elsewhere,
    // which VehicleCreateEvent alone (placement only) doesn't cover.
    @EventHandler
    public void onMove(VehicleMoveEvent event) {
        if (event.getVehicle() instanceof Boat boat)
            removeIfOnBedrock(boat);
    }
}
