package threeadd.glimpsy.feature.simple;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntitySoundEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSoundEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Silences other players' fishing sounds for anyone who opted out with /togglefishingsounds. Those
 * sounds are played by the server itself, not by us, so the only way to drop them for one player is
 * at the packet level - what issue #16 suggested.
 * <p>
 * Runs on a Netty thread, so it must never touch the world/chunks - everything it needs is cached.
 */
public class FishingSoundFilter extends PacketListenerAbstract {

    private static final Logger log = LoggerFactory.getLogger(FishingSoundFilter.class);

    private static final String FISHING_BOBBER_PREFIX = "minecraft:entity.fishing_bobber";
    private static final String THROW_KEY = "minecraft:entity.fishing_bobber.throw";
    private static final String RETRIEVE_KEY = "minecraft:entity.fishing_bobber.retrieve";
    private static final String ENTITY_PREFIX = "minecraft:entity.";
    private static final String SPLASH_SUFFIX = ".splash";

    private static final double OWN_SOUND_RADIUS_SQUARED = 1.0; // 1 block - bobber sounds are at the bobber

    // --- TEMPORARY XP DIAGNOSTIC (remove once confirmed) -------------------------------------
    // Logs every distinct sound key once, and cancels any XP/orb sound for everyone.
    private static final Set<String> loggedKeys = ConcurrentHashMap.newKeySet();

    private static boolean isXpTestSound(String key) {
        String lower = key.toLowerCase();
        return lower.contains("experience") || lower.contains("orb") || lower.contains("levelup")
                || lower.contains("level_up");
    }

    private static void diagnose(String key, PacketSendEvent event) {
        if (loggedKeys.size() < 2000 && loggedKeys.add(key))
            log.info("[fishing-sound] sound key seen: {}", key);

        if (isXpTestSound(key)) {
            log.info("[fishing-sound] CANCELLING xp sound: {}", key);
            event.setCancelled(true);
        }
    }
    // --- end temporary -----------------------------------------------------------------------

    @Override
    public void onPacketSend(PacketSendEvent event) {
        boolean positional = event.getPacketType() == PacketType.Play.Server.SOUND_EFFECT;
        boolean entitySound = event.getPacketType() == PacketType.Play.Server.ENTITY_SOUND_EFFECT;
        if (!positional && !entitySound) return;

        if (entitySound) {
            WrapperPlayServerEntitySoundEffect wrapper = new WrapperPlayServerEntitySoundEffect(event);
            String key = wrapper.getSound().getSoundId().toString();
            diagnose(key, event);
            if (event.isCancelled()) return;

            UUID id = event.getUser().getUUID();
            if (id == null || !FishingSounds.isMuted(id)) return;

            Player entityPlayer = Bukkit.getPlayer(id);
            if (entityPlayer == null) {
                event.setCancelled(true);
                return;
            }

            if (key.equals(RETRIEVE_KEY)) {
                if (!FishingSounds.wasRecentlyHooked(id)) event.setCancelled(true);
                return;
            }

            boolean bobber = key.startsWith(FISHING_BOBBER_PREFIX);
            boolean splash = key.startsWith(ENTITY_PREFIX) && key.endsWith(SPLASH_SUFFIX);
            if (!bobber && !splash) return;

            if (wrapper.getEntityId() != entityPlayer.getEntityId() && bobber) event.setCancelled(true);
            return;
        }

        WrapperPlayServerSoundEffect wrapper = new WrapperPlayServerSoundEffect(event);
        String key = wrapper.getSound().getSoundId().toString();
        diagnose(key, event);
        if (event.isCancelled()) return;

        UUID uuid = event.getUser().getUUID();
        if (uuid == null || !FishingSounds.isMuted(uuid)) return;

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) { // mid-disconnect - just drop it
            event.setCancelled(true);
            return;
        }

        Vector3d position = wrapper.getPosition();

        // Muted players don't hear reels, except the one that hooks THEM (see markHooked).
        if (key.equals(RETRIEVE_KEY)) {
            if (!FishingSounds.wasRecentlyHooked(uuid)) event.setCancelled(true);
            return;
        }

        boolean bobberSound = key.startsWith(FISHING_BOBBER_PREFIX);
        boolean splashSound = key.startsWith(ENTITY_PREFIX) && key.endsWith(SPLASH_SUFFIX);
        if (!bobberSound && !splashSound) return;

        // The cast sound is played at the caster, so match it at the player.
        if (key.equals(THROW_KEY) && isNear(player.getLocation(), position)) return;

        // Otherwise match by the player's OWN bobber (live or just reeled) - never by proximity to
        // the player, or a neighbour's rod would count as yours.
        Location ownBobber = FishingSounds.getOwnBobber(uuid);
        if (ownBobber != null && isNear(ownBobber, position)) return;

        // Others' bobber sounds always go; a generic splash variant only if it's actually at a
        // bobber, so unrelated water splashes (boats, players) still play.
        if (bobberSound || isNearAnyBobber(position)) event.setCancelled(true);
    }

    private static boolean isNearAnyBobber(Vector3d position) {
        for (Location bobber : FishingSounds.getAllBobbers())
            if (isNear(bobber, position)) return true;

        return false;
    }

    private static boolean isNear(Location location, Vector3d position) {
        return distanceSquared(location, position) <= OWN_SOUND_RADIUS_SQUARED;
    }

    private static double distanceSquared(Location location, Vector3d position) {
        double dx = location.getX() - position.getX();
        double dy = location.getY() - position.getY();
        double dz = location.getZ() - position.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
