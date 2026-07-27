package threeadd.glimpsy.util.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.Association;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.association.Associables;
import com.sk89q.worldguard.protection.flags.StateFlag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Soft dependency (see paper-plugin.yml) - WorldGuard's own classes are only ever touched from
// inside isAllowed, and every caller is expected to go through this class rather than reference
// WorldGuard directly, so the plugin keeps working fine when it isn't installed.
public class WorldGuardHook {

    private static final Logger log = LoggerFactory.getLogger(WorldGuardHook.class);
    private static boolean warnedNullFlag = false;

    // Checked live rather than cached once - a cached value taken too early (e.g. before
    // WorldGuard finishes enabling, depending on plugin load order) would silently stay wrong
    // for the plugin's entire lifetime. isPluginEnabled() is just a cheap map lookup.
    private static boolean isEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
    }

    // True everywhere by default - only false where a region overlapping this location has the
    // given flag explicitly set to DENY. If WorldGuard isn't installed, or the flag failed to
    // register, nothing is ever restricted.
    public static boolean isAllowed(Location location, StateFlag flag) {
        boolean enabled = isEnabled();

        // WorldGuard is present but this flag never registered (see WorldGuardFlags#register) -
        // every region check would silently no-op forever without this, so surface it once.
        if (enabled && flag == null && !warnedNullFlag) {
            warnedNullFlag = true;
            log.warn("A WorldGuard flag check ran with a null flag - custom flag registration "
                    + "likely failed (check for a 'Registered custom WorldGuard flags' line at startup). "
                    + "Region protection using this flag will not work until the server is restarted.");
        }

        if (!enabled || flag == null) return true;

        ApplicableRegionSet regions = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .createQuery()
                .getApplicableRegions(BukkitAdapter.adapt(location));

        return regions.queryState(Associables.constant(Association.NON_MEMBER), flag) != StateFlag.State.DENY;
    }
}
