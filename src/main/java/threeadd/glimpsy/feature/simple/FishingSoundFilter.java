package threeadd.glimpsy.feature.simple;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.sound.Sound;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntitySoundEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSoundEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Silences other players' vanilla fishing-bobber sounds (throw / splash / retrieve) for anyone who
 * opted out with /togglefishingsounds. Those sounds are played by the server itself, not by us, so
 * the only way to drop them for one player is at the packet level - what issue #16 suggested.
 * <p>
 * A muted player still hears their <b>own</b> rod: a fishing sound is kept if it's near the player
 * or near a bobber they cast, and dropped otherwise.
 */
public class FishingSoundFilter extends PacketListenerAbstract {

    private static final String FISHING_BOBBER_PREFIX = "minecraft:entity.fishing_bobber";
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

        FishHook ownHook = findOwnHook(player);

        if (positional) {
            WrapperPlayServerSoundEffect wrapper = new WrapperPlayServerSoundEffect(event);
            if (!isFishingSound(wrapper.getSound())) return;
            if (isOwnSound(player, ownHook, wrapper.getPosition())) return;
        } else {
            WrapperPlayServerEntitySoundEffect wrapper = new WrapperPlayServerEntitySoundEffect(event);
            if (!isFishingSound(wrapper.getSound())) return;
            if (ownHook != null && wrapper.getEntityId() == ownHook.getEntityId()) return;
            if (wrapper.getEntityId() == player.getEntityId()) return;
        }

        event.setCancelled(true);
    }

    private static boolean isFishingSound(Sound sound) {
        return sound.getSoundId().toString().startsWith(FISHING_BOBBER_PREFIX);
    }

    // A positional sound belongs to this player if it's at their own bobber or right on top of them
    // (vanilla plays the cast sound at the player).
    private static boolean isOwnSound(Player player, FishHook ownHook, Vector3d position) {
        if (distanceSquared(player.getLocation(), position) <= OWN_SOUND_RADIUS_SQUARED) return true;
        return ownHook != null && distanceSquared(ownHook.getLocation(), position) <= OWN_SOUND_RADIUS_SQUARED;
    }

    private static double distanceSquared(Location location, Vector3d position) {
        double dx = location.getX() - position.getX();
        double dy = location.getY() - position.getY();
        double dz = location.getZ() - position.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static FishHook findOwnHook(Player player) {
        for (FishHook hook : player.getWorld().getEntitiesByClass(FishHook.class)) {
            if (hook.getShooter() instanceof Player shooter && shooter.getUniqueId().equals(player.getUniqueId()))
                return hook;
        }

        return null;
    }
}
