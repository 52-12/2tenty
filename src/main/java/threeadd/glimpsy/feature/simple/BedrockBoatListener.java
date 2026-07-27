package threeadd.glimpsy.feature.simple;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Boat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import threeadd.glimpsy.util.worldguard.WorldGuardFlags;
import threeadd.glimpsy.util.worldguard.WorldGuardHook;

public class BedrockBoatListener implements Listener {

    private void removeIfOnBedrock(Boat boat) {
        // A boat resting exactly on a block's top surface can floor to either that block or
        // the one above it depending on sub-pixel placement, so check both possible
        // boundaries instead of just "the block below".
        Block at = boat.getLocation().getBlock();
        Block below = at.getRelative(BlockFace.DOWN);

        if (at.getType() != Material.BEDROCK && below.getType() != Material.BEDROCK) return;

        // Same opt-in-per-region model as Bridge Egg's flag - only actually removes the boat
        // where a region has explicitly denied boat-fix (e.g. spawn), not everywhere by default.
        if (WorldGuardHook.isAllowed(boat.getLocation(), WorldGuardFlags.BOAT_FIX)) return;

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
