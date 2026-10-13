package threeadd.glimpsy.feature.help.article;

import org.bukkit.configuration.file.YamlConfiguration;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.manager.Manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Persists the admin-chosen display order of the combined /help Community Articles page (both
 * built-in and approved player-submitted entries), keyed by a stable per-entry string so the order
 * survives restarts even as articles are added or removed.
 */
public class ArticleOrderManager extends Manager {

    private static final File FILE = new File(Glimpsy.getInstance().getDataFolder(), "article-order.yml");
    private static List<String> order = new ArrayList<>();

    @Override
    public void enable() {
        if (!FILE.exists()) return;
        order = new ArrayList<>(YamlConfiguration.loadConfiguration(FILE).getStringList("order"));
    }

    private static void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("order", order);
        try {
            config.save(FILE);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't save article order", e);
        }
    }

    /**
     * Reorders {@code items} to match the stored order. Items not seen before (new articles) are
     * appended at the end, keeping their original relative order, and get remembered for next
     * time. Items no longer present (deleted/declined articles) are dropped from the stored order.
     */
    public static <T> List<T> applyOrder(List<T> items, Function<T, String> keyOf) {
        Map<String, T> byKey = new LinkedHashMap<>();
        for (T item : items) byKey.put(keyOf.apply(item), item);

        List<String> newOrder = new ArrayList<>();
        List<T> result = new ArrayList<>(items.size());

        for (String key : order) {
            T item = byKey.remove(key);
            if (item != null) {
                result.add(item);
                newOrder.add(key);
            }
        }

        for (T item : items) {
            String key = keyOf.apply(item);
            if (byKey.remove(key) != null) {
                result.add(item);
                newOrder.add(key);
            }
        }

        if (!newOrder.equals(order)) {
            order = newOrder;
            save();
        }

        return result;
    }

    /** Swaps {@code key} with whatever's directly before it. No-op if it's already first/unknown. */
    public static void moveUp(String key) {
        swap(key, -1);
    }

    /** Swaps {@code key} with whatever's directly after it. No-op if it's already last/unknown. */
    public static void moveDown(String key) {
        swap(key, 1);
    }

    private static void swap(String key, int direction) {
        int index = order.indexOf(key);
        if (index < 0) return;

        int target = index + direction;
        if (target < 0 || target >= order.size()) return;

        List<String> newOrder = new ArrayList<>(order);
        Collections.swap(newOrder, index, target);
        order = newOrder;
        save();
    }
}
