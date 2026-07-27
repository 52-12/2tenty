package threeadd.glimpsy.feature.simple;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.ScheduleUtil;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.inventory.ItemBuilder;
import threeadd.glimpsy.util.text.ComponentParser;
import threeadd.glimpsy.util.worldguard.WorldGuardFlags;
import threeadd.glimpsy.util.worldguard.WorldGuardHook;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Craft in a real crafting table by putting an egg in the center slot with a completed 64-stack
 * of some block in all 8 surrounding slots - the blocks decide what the bridge is made of,
 * weighted by how many of each you used (2+ stacks of the same block just means more of that
 * material in the total, so naturally more likely to come up), and how many blocks you get total
 * (capped at 2 stacks). The match requires each surrounding slot to have at least a full stack,
 * and taking the result consumes the entire stack from each of those 8 slots in one craft (the
 * egg itself is only consumed one at a time) - so each Bridge Egg costs a full 8-stack refill.
 * Thrown like a normal egg (Hypixel Bedwars style) - each tick the egg's position (offset 2
 * blocks below its actual flight line) is sampled, and the segment since the last sample is
 * filled in a few ticks later, well after the egg has moved on. 45 seconds after the trail
 * finishes, it flood-fills outward from its own blocks through anything a player is known to have
 * placed recently (however/whatever order it was built in) and clears the whole thing a few
 * blocks at a time - but the flood fill only ever expands through known-recent player placements
 * made at or after this specific bridge was thrown, never into pre-existing terrain/platforms the
 * bridge happens to connect to at either end, even ones a player built recently enough to still
 * be "tracked" - only things added on top of the bridge during/after its own life count as
 * extensions.
 */
public class BridgeEggListener implements Listener {

    private static int maxBridgeLength() {
        return ConfigManager.getInt("bridge-egg.max-length", 128); // 2 stacks
    }

    private static final NamespacedKey BRIDGE_EGG_KEY = new NamespacedKey(Glimpsy.getInstance(), "bridge_egg");
    private static final NamespacedKey BRIDGE_MATERIALS_KEY = new NamespacedKey(Glimpsy.getInstance(), "bridge_egg_materials");
    private static final NamespacedKey BRIDGE_LENGTH_KEY = new NamespacedKey(Glimpsy.getInstance(), "bridge_egg_length");

    private static final int CENTER_SLOT = 4;

    private static int fullStack() {
        return ConfigManager.getInt("bridge-egg.full-stack-required", 64);
    }

    private static int placementDelayTicks() {
        return ConfigManager.getInt("bridge-egg.placement-delay-ticks", 3);
    }

    // Global, not per-bridge - every block any player places, with when. At clear time this lets
    // a bridge flood-fill outward through legitimate player additions regardless of what order
    // they were built in (a real-time "is this touching the bridge right now" check misses
    // additions built in a scattered order), while never spreading into pre-existing terrain,
    // since that was never placed by a player and so never appears here. Pruned periodically so
    // it doesn't grow forever from ordinary building unrelated to any bridge.
    private static final Map<Block, Long> recentPlayerPlacements = new ConcurrentHashMap<>();

    private static long recentPlacementWindowMillis() {
        return ConfigManager.getLong("bridge-egg.recent-placement-window-minutes", 10) * 60 * 1000L;
    }

    public BridgeEggListener() {
        ScheduleUtil.scheduleSync(BridgeEggListener::pruneOldPlacements, 5, 5, ScheduleUtil.Unit.MINUTE);
    }

    private static void pruneOldPlacements() {
        long cutoff = System.currentTimeMillis() - recentPlacementWindowMillis();
        recentPlayerPlacements.values().removeIf(timestamp -> timestamp < cutoff);
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        Map<Material, Integer> blockCounts = matchBridgeRecipe(event.getInventory().getMatrix());
        if (blockCounts == null) return;

        event.getInventory().setResult(createBridgeEgg(blockCounts));
    }

    // Every non-center slot must be a completed stack of some block, and the center must be a
    // plain (not already-crafted) egg - null if the given grid doesn't match at all.
    private @Nullable Map<Material, Integer> matchBridgeRecipe(ItemStack[] matrix) {
        if (matrix.length != 9) return null; // needs a real 3x3 crafting table, not the 2x2 inventory grid

        ItemStack center = matrix[CENTER_SLOT];
        if (center == null || center.getType() != Material.EGG || isBridgeEgg(center)) return null;

        Map<Material, Integer> blockCounts = new LinkedHashMap<>();

        for (int i = 0; i < matrix.length; i++) {
            if (i == CENTER_SLOT) continue;

            ItemStack item = matrix[i];
            if (item == null || !item.getType().isBlock() || item.getAmount() < fullStack()) return null;

            blockCounts.merge(item.getType(), item.getAmount(), Integer::sum);
        }

        return blockCounts;
    }

    // Without a real registered Recipe backing the result (it's set dynamically via
    // PrepareItemCraftEvent), Bukkit's default result-slot click handling doesn't reliably
    // decrement the grid at all - shift-click left it in a broken leftover state, and a plain
    // click was found to just hand out the egg without touching the ingredients (a straight
    // dupe). So every click on this result is cancelled and replaced with manual logic: consume
    // the egg (1) and the entire stack from each of the 8 surrounding slots, give one Bridge Egg.
    // One craft always fully empties the grid, so there's nothing left to loop for on shift-click.
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof CraftingInventory craftingInventory)) return;
        if (event.getSlot() != 0) return;

        ItemStack result = craftingInventory.getResult();
        if (!isBridgeEgg(result)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        event.setCancelled(true);

        int limit = maxPerPlayer();
        if (countBridgeEggs(player) >= limit) {
            player.sendMessage(limit == 1
                    ? Messages.msg("bridge-egg.limit-one", "<error>You can only carry one Bridge Egg at a time")
                    : Messages.msg("bridge-egg.limit-many", "<error>You can only carry {limit} Bridge Eggs at a time",
                            "limit", String.valueOf(limit)));
            return;
        }

        ItemStack[] matrix = craftingInventory.getMatrix();
        Map<Material, Integer> blockCounts = matchBridgeRecipe(matrix);
        if (blockCounts == null) return; // grid changed since the result was computed - ignore this click

        if (event.getClick().isShiftClick()) {
            giveOrDrop(player, createBridgeEgg(blockCounts));
        } else {
            ItemStack cursor = event.getView().getCursor();
            if (cursor != null && !cursor.getType().isAir()) return; // cursor occupied - do nothing, same as vanilla

            event.getView().setCursor(createBridgeEgg(blockCounts));
        }

        consumeIngredients(matrix);
        craftingInventory.setMatrix(matrix);
    }

    private void giveOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack overflow : leftover.values())
            player.getWorld().dropItem(player.getLocation(), overflow);
    }

    // The egg is consumed one at a time like normal, but every block stack is consumed whole -
    // a Bridge Egg costs the entire 8-stack refill in one craft, not 8 individual blocks.
    private void consumeIngredients(ItemStack[] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            ItemStack item = matrix[i];
            if (item == null || item.getType() == Material.AIR) continue;

            if (i == CENTER_SLOT) {
                item.setAmount(item.getAmount() - 1);
                if (item.getAmount() <= 0) matrix[i] = null;
            } else {
                matrix[i] = null;
            }
        }
    }

    private boolean isBridgeEgg(ItemStack item) {
        if (item == null || item.getType() != Material.EGG) return false;

        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(BRIDGE_EGG_KEY, PersistentDataType.BOOLEAN);
    }

    private static int maxPerPlayer() {
        return ConfigManager.getInt("bridge-egg.max-per-player", 1);
    }

    // Anti-hoarding - a limited number of Bridge Eggs per player at a time, counted anywhere in
    // their inventory including inside bundles and carried shulker boxes (otherwise stashing an
    // egg in one was a trivial workaround for the limit). getContents() already includes the
    // offhand slot.
    private int countBridgeEggs(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents())
            count += countBridgeEggs(item);

        return count;
    }

    private int countBridgeEggs(ItemStack item) {
        if (item == null) return 0;
        if (isBridgeEgg(item)) return item.getAmount();

        int count = 0;
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof BundleMeta bundle) {
            for (ItemStack inside : bundle.getItems())
                count += countBridgeEggs(inside);
        }

        if (meta instanceof BlockStateMeta blockState
                && blockState.hasBlockState()
                && blockState.getBlockState() instanceof ShulkerBox shulkerBox) {
            for (ItemStack inside : shulkerBox.getInventory().getContents())
                count += countBridgeEggs(inside);
        }

        return count;
    }

    private ItemStack createBridgeEgg(Map<Material, Integer> blockCounts) {
        int totalItems = blockCounts.values().stream().mapToInt(Integer::intValue).sum();
        int bridgeLength = Math.min(totalItems, maxBridgeLength());

        String materialList = blockCounts.keySet().stream()
                .map(material -> material.name().toLowerCase().replace('_', ' '))
                .collect(Collectors.joining(", "));

        return new ItemBuilder(Material.EGG)
                .withName(Component.text("Bridge Egg").color(NamedTextColor.WHITE))
                .withLore(ComponentParser.parseRichString(
                        "Throw to bridge " + bridgeLength + " blocks using: " + materialList))
                .withItemMeta(meta -> {
                    meta.getPersistentDataContainer().set(BRIDGE_EGG_KEY, PersistentDataType.BOOLEAN, true);
                    meta.getPersistentDataContainer().set(BRIDGE_MATERIALS_KEY, PersistentDataType.STRING, serializeWeights(blockCounts));
                    meta.getPersistentDataContainer().set(BRIDGE_LENGTH_KEY, PersistentDataType.INTEGER, bridgeLength);
                })
                .build();
    }

    private String serializeWeights(Map<Material, Integer> weights) {
        return weights.entrySet().stream()
                .map(entry -> entry.getKey().name() + ":" + entry.getValue())
                .collect(Collectors.joining(","));
    }

    private Map<Material, Integer> deserializeWeights(String raw) {
        Map<Material, Integer> weights = new LinkedHashMap<>();
        for (String entry : raw.split(",")) {
            String[] parts = entry.split(":", 2);
            if (parts.length != 2) continue;

            Material material = Material.matchMaterial(parts[0]);
            if (material == null) continue;

            weights.put(material, Integer.parseInt(parts[1]));
        }
        return weights;
    }

    // ThrowableProjectile#getItem() hands back the exact ItemStack that was thrown, PDC and all
    // - no need to stash anything on the preceding interact event.
    @EventHandler
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Egg egg)) return;
        if (!(egg.getShooter() instanceof Player player)) return;
        if (!isBridgeEgg(egg.getItem())) return;

        ItemMeta meta = egg.getItem().getItemMeta();
        String serialized = meta.getPersistentDataContainer().get(BRIDGE_MATERIALS_KEY, PersistentDataType.STRING);
        Integer length = meta.getPersistentDataContainer().get(BRIDGE_LENGTH_KEY, PersistentDataType.INTEGER);
        if (serialized == null || length == null) return;

        egg.getPersistentDataContainer().set(BRIDGE_EGG_KEY, PersistentDataType.BOOLEAN, true);
        // A normal thrown egg arcs down from gravity and hits the ground within a handful of
        // blocks - nowhere near enough distance to actually use up the bridge's length budget.
        egg.setGravity(false);
        startBridgeTrail(egg, player.getLocation(), deserializeWeights(serialized), length);
    }

    // Bridge eggs shouldn't hatch a chicken when they land.
    @EventHandler
    public void onEggThrow(PlayerEggThrowEvent event) {
        if (event.getEgg().getPersistentDataContainer().has(BRIDGE_EGG_KEY, PersistentDataType.BOOLEAN))
            event.setHatching(false);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        recentPlayerPlacements.put(block, System.currentTimeMillis());
    }

    // Without this, mining a tracked block away leaves a stale registry entry at that now-empty
    // coordinate - the flood fill would then treat it as still "connected" and jump straight
    // across what's actually just air, bridging a gap that should have stayed disconnected.
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        recentPlayerPlacements.remove(event.getBlock());
    }

    // Samples the egg's position (offset 2 blocks below its actual flight line) every tick, and
    // fills in the segment since the last sample - but the actual placement happens a few ticks
    // later than it was sampled. That combination (well below the egg's own hitbox, and well
    // after it's already moved past) makes it structurally impossible for the egg to collide
    // with its own trail, instead of the fragile "avoid this one exact cell" checks a live,
    // same-position approach needed.
    private void startBridgeTrail(Egg egg, Location origin, Map<Material, Integer> weights, int length) {
        // Anything a player placed before this exact moment (a pre-existing platform the bridge
        // happens to touch, say) must never be swept up by the clear, even though it's still a
        // "recent" placement by the global window's own definition - only chase placements from
        // this point on, i.e. actual extensions built onto the bridge during/after its own life.
        long bridgeStartTime = System.currentTimeMillis();

        // LinkedHashSet - preserves the order blocks were placed in, so clearing can go
        // beginning-to-end the same way the bridge was built, while still deduping in O(1).
        Set<Block> placedBlocks = new LinkedHashSet<>();
        Location[] previousSample = {null};
        BukkitTask[] taskRef = new BukkitTask[1];

        taskRef[0] = ScheduleUtil.scheduleSync(() -> {
            if (!egg.isValid()) {
                taskRef[0].cancel();
                scheduleBridgeClear(placedBlocks, bridgeStartTime);
                return;
            }

            Location sample = egg.getLocation().subtract(0, 2, 0);

            if (sample.distance(origin) >= length) {
                taskRef[0].cancel();
                egg.remove(); // budget's spent - no reason to keep flying
                scheduleBridgeClear(placedBlocks, bridgeStartTime);
                return;
            }

            if (previousSample[0] != null) {
                Location from = previousSample[0];
                Location to = sample.clone();

                ScheduleUtil.scheduleSync(() -> fillSegment(from, to, weights, placedBlocks),
                        placementDelayTicks(), ScheduleUtil.Unit.TICK);
            }

            previousSample[0] = sample.clone();
        }, 0, 1, ScheduleUtil.Unit.TICK);
    }

    // The egg can easily cover more than one block per tick, especially right after the throw -
    // fill in every block along the whole segment it crossed between samples instead of just the
    // two endpoints, or the trail ends up full of gaps.
    private void fillSegment(Location from, Location to, Map<Material, Integer> weights, Set<Block> placedBlocks) {
        Vector segment = to.toVector().subtract(from.toVector());
        int steps = Math.max(1, (int) Math.ceil(segment.length()));

        for (int i = 0; i <= steps; i++) {
            Location point = from.clone().add(segment.clone().multiply((double) i / steps));
            tryPlace(point.getBlock(), weights, placedBlocks);
        }
    }

    private boolean tryPlace(Block target, Map<Material, Integer> weights, Set<Block> placedBlocks) {
        if (target.getType().isSolid()) return false; // never overwrite real terrain
        if (!WorldGuardHook.isAllowed(target.getLocation(), WorldGuardFlags.BRIDGE_EGG)) return false;
        if (!placedBlocks.add(target)) return false;
        if (wouldTrapPlayerHead(target)) return false;

        target.setType(pickWeightedMaterial(weights));
        return true;
    }

    // Don't place directly on a player's feet or head - that's the classic "block spawns on
    // your head" trap that forces them into the crawling hitbox.
    private boolean wouldTrapPlayerHead(Block block) {
        for (Player player : block.getWorld().getPlayers()) {
            Block feet = player.getLocation().getBlock();
            if (block.equals(feet) || block.equals(feet.getRelative(BlockFace.UP))) return true;
        }
        return false;
    }

    private Material pickWeightedMaterial(Map<Material, Integer> weights) {
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        int roll = ThreadLocalRandom.current().nextInt(total);

        for (Map.Entry<Material, Integer> entry : weights.entrySet()) {
            roll -= entry.getValue();
            if (roll < 0) return entry.getKey();
        }

        throw new IllegalStateException("Couldn't pick a weighted bridge material");
    }

    private static int clearBatchSize() {
        return ConfigManager.getInt("bridge-egg.clear-batch-size", 3); // blocks removed per step
    }

    private static int clearStepDelayTicks() {
        return ConfigManager.getInt("bridge-egg.clear-step-delay-ticks", 2);
    }

    // A flat ceiling, not scaled to the bridge's own size - the actual safety here comes from
    // only ever expanding through known-recent player placements (never arbitrary terrain), so
    // this only exists to stop a truly pathological case, not to limit ordinary spamming.
    private static int maxClearBlocks() {
        return ConfigManager.getInt("bridge-egg.max-clear-blocks", 5000);
    }

    private static int clearDelaySeconds() {
        return ConfigManager.getInt("bridge-egg.clear-delay-seconds", 45);
    }

    private void scheduleBridgeClear(Set<Block> orderedBlocks, long bridgeStartTime) {
        if (orderedBlocks.isEmpty()) return;

        ScheduleUtil.scheduleSync(() -> clearNextBatch(new ArrayDeque<>(orderedBlocks), new LinkedHashSet<>(), orderedBlocks.size(), bridgeStartTime),
                clearDelaySeconds(), ScheduleUtil.Unit.SECOND);
    }

    // Flood-fills outward from the trail's own blocks through anything a player is known to have
    // placed recently, clearing as it goes - a few blocks per step instead of all at once. Merging
    // the flood fill into the clearing loop itself (rather than collecting the full connected set
    // upfront) means it keeps checking recentPlayerPlacements fresh throughout the several seconds
    // clearing takes, so a block placed while clearing is already in progress can still get caught
    // instead of only whatever existed in one snapshot taken right as the 45 seconds elapsed.
    // Never expands into anything outside that registry, which pre-existing terrain/platforms
    // never are - and even among tracked placements, only ones from at or after this bridge's own
    // start time count, so a platform placed before the egg was thrown (but still touching the
    // bridge) is never treated as an extension.
    private void clearNextBatch(Deque<Block> queue, Set<Block> visited, int originalCount, long bridgeStartTime) {
        int cleared = 0;

        while (!queue.isEmpty() && visited.size() < maxClearBlocks() && cleared < clearBatchSize()) {
            Block block = queue.poll();
            if (!visited.add(block)) continue;

            block.setType(Material.AIR);
            recentPlayerPlacements.remove(block); // claimed - don't let another bridge double-clear it
            cleared++;

            for (BlockFace face : BlockFace.values()) {
                if (!face.isCartesian()) continue;

                Block neighbor = block.getRelative(face);
                // Belt and suspenders on top of the BlockBreakEvent cleanup - if a tracked block
                // was ever removed by something other than mining (fire, explosion, flowing
                // water, etc.) without the registry entry being cleaned up, this still won't
                // treat a since-emptied coordinate as a real connection.
                Long placedAt = recentPlayerPlacements.get(neighbor);
                if (!visited.contains(neighbor) && !neighbor.getType().isAir()
                        && placedAt != null && placedAt >= bridgeStartTime) {
                    queue.add(neighbor);
                }
            }
        }

        if (!queue.isEmpty() && visited.size() < maxClearBlocks()) {
            ScheduleUtil.scheduleSync(() -> clearNextBatch(queue, visited, originalCount, bridgeStartTime), clearStepDelayTicks(), ScheduleUtil.Unit.TICK);
        }
    }
}
