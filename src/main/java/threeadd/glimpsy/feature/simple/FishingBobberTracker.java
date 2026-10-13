package threeadd.glimpsy.feature.simple;

import org.bukkit.Location;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import threeadd.glimpsy.util.ScheduleUtil;
import threeadd.glimpsy.util.ScheduleUtil.Unit;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks each player's active fishing bobber and republishes its location, so
 * {@link FishingSoundFilter} can tell a player's own bobber sounds from everyone else's without
 * touching the world from a Netty thread. Runs every other tick (block accuracy is plenty) and only
 * republishes when the bobber actually moved a block.
 */
public class FishingBobberTracker implements Listener {

    private static final Map<UUID, FishHook> hooks = new ConcurrentHashMap<>();
    private static final Map<UUID, Location> lastPublished = new ConcurrentHashMap<>();

    public FishingBobberTracker() {
        ScheduleUtil.scheduleSync(FishingBobberTracker::refresh, 2, 2, Unit.TICK);
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        UUID id = event.getPlayer().getUniqueId();

        switch (event.getState()) {
            case FISHING -> hooks.put(id, event.getHook());
            case CAUGHT_ENTITY -> {
                // Marked at the exact hook event; the filter lets this one reel through for a few
                // ticks (packet latency).
                if (event.getCaught() instanceof Player caught) FishingSounds.markHooked(caught);
            }
            case CAUGHT_FISH, REEL_IN, FAILED_ATTEMPT -> hooks.remove(id);
            default -> {
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        hooks.remove(id);
        lastPublished.remove(id);
        FishingSounds.clear(id);
    }

    private static void refresh() {
        for (Iterator<Map.Entry<UUID, FishHook>> iterator = hooks.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<UUID, FishHook> entry = iterator.next();
            UUID id = entry.getKey();
            FishHook hook = entry.getValue();

            if (!hook.isValid()) {
                iterator.remove();
                lastPublished.remove(id);
                FishingSounds.setOwnBobber(id, null);
                continue;
            }

            Location location = hook.getLocation();
            Location previous = lastPublished.get(id);

            // Block accuracy is all the sound filter needs - skip the clone when it hasn't moved a block.
            if (previous != null
                    && previous.getBlockX() == location.getBlockX()
                    && previous.getBlockY() == location.getBlockY()
                    && previous.getBlockZ() == location.getBlockZ()) continue;

            lastPublished.put(id, location);
            FishingSounds.setOwnBobber(id, location);
        }
    }
}
