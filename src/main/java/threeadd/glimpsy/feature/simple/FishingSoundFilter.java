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

import java.util.UUID;

/**
 * Silences other players' fishing sounds for anyone who opted out with /togglefishingsounds. Those
 * sounds are played by the server itself, not by us, so the only way to drop them for one player is
 * at the packet level - what issue #16 suggested.
 * <p>
 * A muted player still hears:
 * <ul>
 *     <li>their own rod (cast / water splash / reel), by proximity to them or their own bobber;</li>
 *     <li>the reel-in "pull-up" sound ({@code entity.fishing_bobber.retrieve}) from anyone - that's
 *         the sound of a catch actually being hauled in, which is worth keeping;</li>
 *     <li>their own XP-pickup chime.</li>
 * </ul>
 * <p>
 * Runs on a Netty thread, so it must never touch the world/chunks - everything it needs is cached.
 */
public class FishingSoundFilter extends PacketListenerAbstract {

    private static final String FISHING_BOBBER_PREFIX = "minecraft:entity.fishing_bobber";
    private static final String RETRIEVE_KEY = "minecraft:entity.fishing_bobber.retrieve";
    private static final String EXPERIENCE_PICKUP_KEY = "minecraft:entity.experience_orb.pickup";
    private static final String ENTITY_PREFIX = "minecraft:entity.";
    private static final String SPLASH_SUFFIX = ".splash";
    private static final double OWN_SOUND_RADIUS_SQUARED = 4.0; // 2 blocks

    @Override
    public void onPacketSend(PacketSendEvent event) {
        boolean positional = event.getPacketType() == PacketType.Play.Server.SOUND_EFFECT;
        boolean entitySound = event.getPacketType() == PacketType.Play.Server.ENTITY_SOUND_EFFECT;
        if (!positional && !entitySound) return;

        UUID uuid = event.getUser().getUUID();
        if (uuid == null || !FishingSounds.isMuted(uuid)) return;

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) { // mid-disconnect - just drop it
            event.setCancelled(true);
            return;
        }

        if (positional) {
            WrapperPlayServerSoundEffect wrapper = new WrapperPlayServerSoundEffect(event);
            String key = wrapper.getSound().getSoundId().toString();

            if (key.equals(RETRIEVE_KEY)) return; // the actual pull-up of a catch stays audible

            Vector3d position = wrapper.getPosition();

            if (key.equals(EXPERIENCE_PICKUP_KEY)) {
                // Vanilla broadcasts this to nearby players whenever someone gains XP - mute other
                // players' XP, keep the muted player's own.
                if (!isNear(player.getLocation(), position)) event.setCancelled(true);
                return;
            }

            boolean bobberSound = key.startsWith(FISHING_BOBBER_PREFIX);
            boolean splashSound = key.startsWith(ENTITY_PREFIX) && key.endsWith(SPLASH_SUFFIX);
            if (!bobberSound && !splashSound) return;

            if (isNearOwnSound(player, uuid, position)) return;

            // Others' bobber sounds always go; a generic splash variant only if it's actually at a
            // bobber, so unrelated water splashes (boats, players) still play.
            if (bobberSound || isNearAnyBobber(position)) event.setCancelled(true);
            return;
        }

        WrapperPlayServerEntitySoundEffect wrapper = new WrapperPlayServerEntitySoundEffect(event);
        String key = wrapper.getSound().getSoundId().toString();

        if (key.equals(RETRIEVE_KEY)) return;

        boolean bobberSound = key.startsWith(FISHING_BOBBER_PREFIX);
        boolean splashSound = key.startsWith(ENTITY_PREFIX) && key.endsWith(SPLASH_SUFFIX);
        if (!bobberSound && !splashSound) return;

        // Entity sounds aren't positional, so the only thing we can trust is "is it this player".
        if (wrapper.getEntityId() != player.getEntityId() && bobberSound) event.setCancelled(true);
    }

    // A positional sound belongs to this player if it's at their own bobber or right on top of them
    // (vanilla plays the cast sound at the player).
    private static boolean isNearOwnSound(Player player, UUID uuid, Vector3d position) {
        if (isNear(player.getLocation(), position)) return true;

        Location ownBobber = FishingSounds.getOwnBobber(uuid);
        return ownBobber != null && isNear(ownBobber, position);
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
