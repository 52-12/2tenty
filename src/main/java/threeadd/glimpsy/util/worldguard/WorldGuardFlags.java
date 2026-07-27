package threeadd.glimpsy.util.worldguard;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Custom WorldGuard region flags. Both default to ALLOW everywhere - a region only needs to set
// one of these to DENY to opt itself into that specific protection (e.g. `/rg flag spawn
// bridge-egg deny` stops Bridge Egg trails from placing blocks in the spawn region specifically,
// without touching Bridge Egg anywhere else on the map).
//
// Must be registered from Glimpsy#onLoad() - WorldGuard's flag registry is only open for
// registration before WorldGuard's own onEnable() runs, and Bukkit guarantees every plugin's
// onLoad() finishes before any plugin's onEnable() starts, regardless of load order.
public class WorldGuardFlags {

    private static final Logger log = LoggerFactory.getLogger(WorldGuardFlags.class);

    public static StateFlag BRIDGE_EGG;
    public static StateFlag BOAT_FIX;

    public static void register() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

        BRIDGE_EGG = registerFlag(registry, "bridge-egg");
        BOAT_FIX = registerFlag(registry, "boat-fix");
    }

    private static StateFlag registerFlag(FlagRegistry registry, String name) {
        StateFlag flag = new StateFlag(name, true); // default ALLOW

        try {
            registry.register(flag);
            return flag;
        } catch (FlagConflictException e) {
            // Already registered (e.g. a /reload) - reuse WorldGuard's existing instance instead
            // of ours, since only one Flag instance can actually back this name at a time.
            log.warn("WorldGuard flag '{}' was already registered - reusing the existing one", name);
            return (StateFlag) registry.get(name);
        }
    }
}
