package threeadd.glimpsy.feature.randomitems;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threeadd.glimpsy.util.ScheduleUtil;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.manager.ListeningManager;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class RandomItemManager extends ListeningManager {

    // Map Material -> Weight see RandomItemManager#getRandomItem
    public static final Map<Material, Integer> RANDOM_ITEMS = Map.of(
            Material.IRON_NUGGET, 1,
            Material.DIRT, 2,
            Material.COBBLESTONE, 5,
            Material.OAK_PLANKS, 4
    );

    private static final Map<UUID, RandomItemsInfo> randomItemInfoMap = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(RandomItemManager.class);
    private BukkitTask task;

    // The material pool itself (RANDOM_ITEMS' keys) is structural - only each material's weight
    // is a balance knob, defaulting to whatever's hardcoded in the map above.
    private static int weightFor(Material material, int defaultWeight) {
        return ConfigManager.getInt("random-items.weights." + material.name().toLowerCase(), defaultWeight);
    }

    private static Material getRandomItem() {
        int totalWeight = 0;
        for (Map.Entry<Material, Integer> entry : RANDOM_ITEMS.entrySet())
            totalWeight += weightFor(entry.getKey(), entry.getValue());

        int random = ThreadLocalRandom.current().nextInt(totalWeight);

        for (Map.Entry<Material, Integer> entry : RANDOM_ITEMS.entrySet()) {
            random -= weightFor(entry.getKey(), entry.getValue());
            if (random < 0) {
                return entry.getKey();
            }
        }

        throw new IllegalStateException("Couldn't get random item");
    }

    public static @Nullable RandomItemsInfo getInfo(Player player) {
        return randomItemInfoMap.get(player.getUniqueId());
    }

    @Override
    public void onEnable() {
        task = ScheduleUtil.scheduleSync(this::tick,
                0,
                ConfigManager.getInt("random-items.tick-interval-seconds", 5),
                ScheduleUtil.Unit.SECOND);
    }

    private void tick() {
        Material material = getRandomItem();
        ItemStack item = ItemStack.of(material);

        for (Player player : Bukkit.getOnlinePlayers()) {

            RandomItemsInfo randomItemInfo = randomItemInfoMap.get(player.getUniqueId());

            if (randomItemInfo == null) { // Data hasn't loaded yet, skipping this tick
                continue;
            }

            if (!randomItemInfo.isEnabled(material))
                continue;

            if (!player.getInventory().addItem(item).isEmpty() && randomItemInfo.isFullInventoryAlertEnabled()) {
                player.sendActionBar(ComponentParser.parseRichString(
                        "<red>Your inventory is full - free up space to keep receiving items"));
            }
        }
    }

    @Override
    public void disable() {
        if (task != null) task.cancel();
        task = null;

        for (Player player : Bukkit.getOnlinePlayers()) {
            RandomItemsInfo info = randomItemInfoMap.get(player.getUniqueId());

            // Don't save useless data (If player joins without data it takes default states)
            if (info == null || info.isDefault()) {
                RandomItemsRepository.getInstance().deleteSync(player.getUniqueId());
                log.info("Sync deleted default items for {}", player.getUniqueId());
                return;
            }

            RandomItemsRepository.getInstance().saveSync(info);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        RandomItemsRepository.getInstance().loadAsync(player.getUniqueId())
                .thenAccept(optionalInfo -> {
                    RandomItemsInfo info = optionalInfo.orElse(new RandomItemsInfo(player));
                    randomItemInfoMap.put(player.getUniqueId(), info);
                });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        RandomItemsInfo info = randomItemInfoMap.remove(id);

        // Don't save useless data (If player joins without data it takes default states). isDefault()
        // includes the full-inventory alert flag, so a player who only changed that still gets saved
        // (the old check compared material states only and wiped the alert preference - #3).
        if (info == null || info.isDefault()) {
            RandomItemsRepository.getInstance().deleteAsync(id).thenRun(() ->
                    log.info("Deleted default items for {}", id));

            return;
        }

        RandomItemsRepository.getInstance().saveAsync(info).thenRun(() -> log.info("Saved items of {}", id));
    }
}
