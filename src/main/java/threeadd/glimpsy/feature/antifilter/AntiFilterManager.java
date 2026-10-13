package threeadd.glimpsy.feature.antifilter;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.Repairable;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.manager.ListeningManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player anti-drop / anti-pickup filter. Each protected entry is a whole item sample, but the
 * two filters match differently:
 * <ul>
 *     <li><b>Anti-drop</b> matches a sample's <b>identity</b>: same material plus the same
 *         customisation (display name, lore, custom data, trim, ...), but ignoring the bits that
 *         change just from using or improving the item - durability, enchantments, stored
 *         enchants and anvil repair cost. So a registered tool keeps its protection after it's
 *         been worn down or re-enchanted, and a plain sample effectively covers its whole
 *         material.</li>
 *     <li><b>Anti-pickup</b> is always by <b>material</b> - any sample of a material stops all
 *         items of that material being picked up, enchanted or not.</li>
 * </ul>
 * The menu only ever holds display copies; the player keeps their real items (see
 * {@link AntiFilterInventory}).
 */
public class AntiFilterManager extends ListeningManager {

    private static final Map<UUID, Map<AntiFilterType, FilterSet>> filters = new ConcurrentHashMap<>();

    private static boolean enabled() {
        return ConfigManager.getBoolean("anti-filter.enabled", true);
    }

    public static boolean isEnabled() {
        return enabled();
    }

    @Override
    public void onEnable() {
        AntiFilterRepository.getInstance().loadAllSync().forEach((id, byType) ->
                byType.forEach((type, samples) ->
                        filters.computeIfAbsent(id, x -> new EnumMap<>(AntiFilterType.class))
                                .put(type, new FilterSet(samples))));
    }

    public static void open(Player player, AntiFilterType type) {
        FilterSet set = getSet(player.getUniqueId(), type);

        // Bring the stored samples up to date with the player's current items (new enchants, a
        // rename, ...) before drawing the menu, so the display follows the real item.
        if (set.syncFromInventory(player))
            AntiFilterRepository.getInstance().runAsyncSave(player.getUniqueId(), type, set.samples);

        AntiFilterInventory gui = new AntiFilterInventory(player.getUniqueId(), type);
        gui.render(set.samples);
        player.openInventory(gui.getInventory());
    }

    private static FilterSet getSet(UUID playerId, AntiFilterType type) {
        return filters.computeIfAbsent(playerId, x -> new EnumMap<>(AntiFilterType.class))
                .computeIfAbsent(type, x -> new FilterSet(new ArrayList<>()));
    }

    private static boolean isProtected(UUID playerId, AntiFilterType type, ItemStack item) {
        Map<AntiFilterType, FilterSet> byType = filters.get(playerId);
        if (byType == null) return false;
        FilterSet set = byType.get(type);
        if (set == null) return false;

        // Anti-drop matches enchanted items specifically; anti-pickup always matches by material.
        return type == AntiFilterType.DROP ? set.matchesSpecific(item) : set.matchesByMaterial(item);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AntiFilterInventory gui)) return;

        event.setCancelled(true); // this is a menu - no real item ever moves
        if (!(event.getWhoClicked() instanceof Player player)) return;

        FilterSet set = getSet(gui.getOwner(), gui.getType());
        boolean changed;

        if (event.getClickedInventory() == event.getInventory()) {
            // A click in the menu: on a marker removes that sample; with an item on the cursor adds it
            ItemStack marker = event.getCurrentItem();
            if (marker != null && !marker.getType().isAir()) {
                changed = set.remove(gui.sampleAt(event.getSlot()));
            } else {
                changed = set.add(event.getCursor());
            }
        } else {
            // A click on any item in the player's own inventory adds it (no shift needed)
            changed = set.add(event.getCurrentItem());
        }

        if (changed) {
            AntiFilterRepository.getInstance().runAsyncSave(gui.getOwner(), gui.getType(), set.samples);
            gui.render(set.samples);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof AntiFilterInventory)
            event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!enabled()) return;

        Player player = event.getPlayer();
        ItemStack stack = event.getItemDrop().getItemStack();
        if (!isProtected(player.getUniqueId(), AntiFilterType.DROP, stack)) return;

        // Prevent the drop by despawning the dropped entity and putting the stack straight back
        // into the inventory (rather than cancelling, which can lose the item if a pickup fills
        // the freed slot before the cancel returns it). This all happens synchronously in this
        // handler, so no pickup can interleave. If the inventory is genuinely full, re-spawn the
        // item on the ground instead of deleting it.
        event.getItemDrop().remove();
        for (ItemStack leftover : player.getInventory().addItem(stack).values())
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof AntiFilterInventory)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        // Safety for the rare case of an item left on the cursor when the menu closes: give it
        // back to the inventory, and if that's full drop it via the world (not a player-drop, so
        // anti-drop can't swallow it) rather than letting it vanish.
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            player.setItemOnCursor(null);
            for (ItemStack leftover : player.getInventory().addItem(cursor).values())
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!enabled()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        if (isProtected(player.getUniqueId(), AntiFilterType.PICKUP, event.getItem().getItemStack()))
            event.setCancelled(true);
    }

    /**
     * A player's protected samples for one filter type. Anti-pickup uses the material set; anti-drop
     * compares against pre-normalised copies (see {@link #normalizeForDrop}).
     */
    private static final class FilterSet {

        final List<ItemStack> samples;
        private final Set<Material> allMaterials = new HashSet<>();            // any sample's material (pickup)
        private final List<ItemStack> normalizedSamples = new ArrayList<>();   // identity copies (drop)

        FilterSet(List<ItemStack> samples) {
            this.samples = samples;
            recompute();
        }

        // Anti-drop: same material and same normalised identity (name/lore/custom data/etc.).
        boolean matchesSpecific(ItemStack item) {
            if (item == null || !allMaterials.contains(item.getType())) return false;

            ItemStack normalizedItem = normalizeForDrop(item);
            for (ItemStack sample : normalizedSamples)
                if (sample.isSimilar(normalizedItem)) return true;
            return false;
        }

        // Anti-pickup: any sample of a material matches every item of that material.
        boolean matchesByMaterial(ItemStack item) {
            return item != null && allMaterials.contains(item.getType());
        }

        boolean add(ItemStack item) {
            if (item == null || item.getType().isAir() || !item.getType().isItem()) return false;

            ItemStack sample = item.clone();
            sample.setAmount(1);
            if (samples.stream().anyMatch(existing -> existing.isSimilar(sample))) return false; // already there

            samples.add(sample);
            recompute();
            return true;
        }

        boolean remove(ItemStack sample) {
            if (sample == null || !samples.remove(sample)) return false;
            recompute();
            return true;
        }

        private void recompute() {
            allMaterials.clear();
            normalizedSamples.clear();
            for (ItemStack sample : samples) {
                allMaterials.add(sample.getType());
                normalizedSamples.add(normalizeForDrop(sample));
            }
        }

        // Pulls each stored sample up to date from a matching item the player currently has, so
        // enchanting/renaming the protected item shows through in the menu and the stored sample
        // follows the real item. Same identity comparison as matchesSpecific, with an exact match
        // preferred so the richest copy wins.
        boolean syncFromInventory(Player player) {
            List<ItemStack> contents = new ArrayList<>(Arrays.asList(player.getInventory().getContents()));
            contents.add(player.getInventory().getItemInOffHand());

            boolean changed = false;

            for (int i = 0; i < samples.size(); i++) {
                ItemStack sample = samples.get(i);
                ItemStack identityMatch = null;
                boolean exact = false;

                for (ItemStack candidate : contents) {
                    if (candidate == null || candidate.getType() != sample.getType()) continue;
                    if (!normalizeForDrop(candidate).isSimilar(normalizeForDrop(sample))) continue;

                    if (candidate.isSimilar(sample)) {
                        exact = true;
                        break;
                    }

                    if (identityMatch == null) identityMatch = candidate;
                }

                if (exact || identityMatch == null) continue;

                ItemStack updated = identityMatch.clone();
                updated.setAmount(1);
                samples.set(i, updated);
                changed = true;
            }

            if (changed) recompute();
            return changed;
        }

        // The "identity" of an item for anti-drop purposes: everything that says what the item is,
        // with the mutable wear/improvement state stripped out. Neutralising those on both sides
        // before isSimilar is what stops a durability change or an added enchantment from ending
        // the protection (issue #11).
        private static ItemStack normalizeForDrop(ItemStack item) {
            ItemStack copy = item.clone();
            copy.setAmount(1);
            copy.editMeta(meta -> {
                meta.removeEnchantments();
                if (meta instanceof Damageable damageable) damageable.setDamage(0);
                if (meta instanceof EnchantmentStorageMeta storage) {
                    for (Enchantment enchantment : new ArrayList<>(storage.getStoredEnchants().keySet()))
                        storage.removeStoredEnchant(enchantment);
                }
                if (meta instanceof Repairable repairable) repairable.setRepairCost(0);
            });
            return copy;
        }
    }
}
