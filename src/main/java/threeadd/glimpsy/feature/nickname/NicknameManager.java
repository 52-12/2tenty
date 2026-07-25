package threeadd.glimpsy.feature.nickname;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threeadd.glimpsy.util.manager.ListeningManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NicknameManager extends ListeningManager {

    private static final Logger log = LoggerFactory.getLogger(NicknameManager.class);
    private static final Map<UUID, String> nicknames = new ConcurrentHashMap<>();

    public static @Nullable String getNickname(Player player) {
        return getNickname(player.getUniqueId());
    }

    // UUID-keyed since callers may be running off the main thread (e.g. TabManager's async
    // packet building, or the async chat renderer), where touching a live Player isn't safe.
    public static @Nullable String getNickname(UUID uuid) {
        return nicknames.get(uuid);
    }

    public static void setNickname(Player player, String rawName) {
        nicknames.put(player.getUniqueId(), rawName);
        NicknameRepository.getInstance().runAsyncSave(player.getUniqueId(), rawName);
    }

    public static void clearNickname(Player player) {
        nicknames.remove(player.getUniqueId());
        NicknameRepository.getInstance().runAsyncClear(player.getUniqueId());
    }

    /**
     * The colored nickname component if the player has one set, otherwise their plain name.
     */
    public static Component getDisplayName(String playerName, UUID uuid) {
        String raw = nicknames.get(uuid);
        return raw == null ? Component.text(playerName) : MiniMessage.miniMessage().deserialize(raw);
    }

    @Override
    public void onEnable() {
        nicknames.putAll(NicknameRepository.getInstance().loadAllSync());
        log.info("Loaded {} nickname(s)", nicknames.size());
    }
}
