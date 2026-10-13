package threeadd.glimpsy.feature.simple;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntitySoundEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSoundEffect;

import java.util.UUID;

/**
 * Silences vanilla's fishing-bobber sounds (throw / splash / retrieve) for players who opted out
 * with /togglefishingsounds. Those sounds are played by the server itself, not by us, so the only
 * way to drop them for one player is at the packet level - exactly what issue #16 suggested.
 */
public class FishingSoundFilter extends PacketListenerAbstract {

    private static final String FISHING_BOBBER_PREFIX = "minecraft:entity.fishing_bobber";

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.SOUND_EFFECT
                && event.getPacketType() != PacketType.Play.Server.ENTITY_SOUND_EFFECT) return;

        UUID uuid = event.getUser().getUUID();
        if (uuid == null || !FishingSounds.isMuted(uuid)) return;

        String key = resolveSoundKey(event);
        if (key != null && key.startsWith(FISHING_BOBBER_PREFIX)) event.setCancelled(true);
    }

    private static String resolveSoundKey(PacketSendEvent event) {
        try {
            if (event.getPacketType() == PacketType.Play.Server.SOUND_EFFECT)
                return new WrapperPlayServerSoundEffect(event).getSound().getSoundId().toString();

            return new WrapperPlayServerEntitySoundEffect(event).getSound().getSoundId().toString();
        } catch (Throwable throwable) {
            return null; // unknown/unreadable - leave the packet alone
        }
    }
}
