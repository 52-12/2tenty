package threeadd.glimpsy.feature.collection;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threeadd.glimpsy.util.manager.ListeningManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CollectionManager extends ListeningManager {

    public static final List<Material> COLLECTABLES = List.of(
            Material.IRON_NUGGET,
            Material.DIRT,
            Material.COBBLESTONE,
            Material.OAK_PLANKS
    );
    private static final Map<UUID, CollectionInfo> collectionsInfoMap = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(CollectionManager.class);

    public static CollectionInfo getCollectionInfo(Player player) {
        return collectionsInfoMap.get(player.getUniqueId());
    }

    public static int getCollectionPoints(Player player) {
        return collectionsInfoMap.get(player.getUniqueId()).getCollected().size();
    }

    @Override
    public void disable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            CollectionInfo info = collectionsInfoMap.get(player.getUniqueId());

            // Don't save useless data (If player joins without data it takes default states)
            if (info == null || info.getStates().equals(CollectionInfo.createDefaultStates())) {
                CollectionInfoRepository.getInstance().deleteSync(player.getUniqueId());
                log.info("Sync deleted default collected items for {}", player.getUniqueId());
                return;
            }

            CollectionInfoRepository.getInstance().saveSync(info);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        CollectionInfoRepository.getInstance().loadAsync(player.getUniqueId())
                .thenAccept(optionalInfo -> {
                    CollectionInfo info = optionalInfo.orElse(new CollectionInfo(player));
                    collectionsInfoMap.put(player.getUniqueId(), info);
                });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        CollectionInfo info = collectionsInfoMap.remove(id);

        // Don't save useless data (If player joins without data it takes default states)
        if (info == null || info.getStates().equals(CollectionInfo.createDefaultStates())) {
            CollectionInfoRepository.getInstance().deleteAsync(id).thenRun(() ->
                    log.info("Deleted default collected items for {}", id));

            return;
        }

        CollectionInfoRepository.getInstance().saveAsync(info).thenRun(() -> log.info("Saved collected items of {}", id));
    }
}
