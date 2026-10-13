package threeadd.glimpsy.feature.simple;

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
 * Tracks each player's active fishing bobber and republishes its location every tick, so
 * {@link FishingSoundFilter} can tell a player's own bobber sounds from everyone else's without
 * touching the world from a Netty thread.
 */
public class FishingBobberTracker implements Listener {

    private static final Map<UUID, FishHook> hooks = new ConcurrentHashMap<>();

    public FishingBobberTracker() {
        ScheduleUtil.scheduleSync(FishingBobberTracker::refresh, 1, 1, Unit.TICK);
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        UUID id = event.getPlayer().getUniqueId();

        switch (event.getState()) {
            case FISHING -> hooks.put(id, event.getHook());
            case CAUGHT_ENTITY -> {
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
        FishingSounds.setOwnBobber(id, null);
    }

    private static void refresh() {
        for (Iterator<Map.Entry<UUID, FishHook>> iterator = hooks.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<UUID, FishHook> entry = iterator.next();
            FishHook hook = entry.getValue();

            if (!hook.isValid()) {
                iterator.remove();
                FishingSounds.setOwnBobber(entry.getKey(), null);
                continue;
            }

            FishingSounds.setOwnBobber(entry.getKey(), hook.getLocation());
        }
    }
}
