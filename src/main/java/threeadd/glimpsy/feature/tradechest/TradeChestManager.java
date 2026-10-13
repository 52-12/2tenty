package threeadd.glimpsy.feature.tradechest;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.feature.achievement.Achievement;
import threeadd.glimpsy.feature.achievement.AchievementManager;
import threeadd.glimpsy.feature.teams.TeamManager;
import threeadd.glimpsy.util.ScheduleUtil;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.inventory.ItemBuilder;
import threeadd.glimpsy.util.manager.ListeningManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TradeChestManager extends ListeningManager {

    private static final Map<UUID, TradeChestData> chestsByOwner = new ConcurrentHashMap<>();
    // Both participants map to the same offer instance - a player can only be in one at a time
    private static final Map<UUID, TradeOffer> offersByPlayer = new ConcurrentHashMap<>();
    private static final List<TradeDelivery> deliveries = new CopyOnWriteArrayList<>();

    private static boolean enabled() {
        return ConfigManager.getBoolean("trade-chest.enabled", true);
    }

    private static long deliveryIntervalMillis() {
        return ConfigManager.getLong("trade-chest.delivery-interval-minutes", 5) * 60_000L;
    }

    @Override
    public void onEnable() {
        chestsByOwner.putAll(TradeChestRepository.getInstance().loadChestsSync());

        for (TradeOffer offer : TradeChestRepository.getInstance().loadOffersSync()) {
            offersByPlayer.put(offer.getFrom(), offer);
            offersByPlayer.put(offer.getTo(), offer);
        }

        deliveries.addAll(TradeChestRepository.getInstance().loadDeliveriesSync());

        ScheduleUtil.scheduleSync(TradeChestManager::deliveryTick, 10, 10, ScheduleUtil.Unit.SECOND);
    }

    // The chest owner themself, or anyone on their team, can use the chest and act in trades
    private static boolean canUse(Player player, UUID owner) {
        return player.getUniqueId().equals(owner) || TeamManager.isSameTeam(player.getUniqueId(), owner);
    }

    /**
     * Which side of the offer this player acts for - exact participants first, then teammates.
     * Null if uninvolved, or a teammate of both sides (ambiguous).
     */
    private static @Nullable UUID actingSide(Player player, TradeOffer offer) {
        UUID id = player.getUniqueId();
        if (id.equals(offer.getFrom())) return offer.getFrom();
        if (id.equals(offer.getTo())) return offer.getTo();

        boolean fromTeam = TeamManager.isSameTeam(id, offer.getFrom());
        boolean toTeam = TeamManager.isSameTeam(id, offer.getTo());
        if (fromTeam && !toTeam) return offer.getFrom();
        if (toTeam && !fromTeam) return offer.getTo();
        return null;
    }

    // Placement / breaking

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (!enabled()) return;
        if (!TradeChestItem.isTradeChest(event.getItemInHand())) return;

        Player player = event.getPlayer();
        TradeChestData existing = chestsByOwner.get(player.getUniqueId());
        if (existing != null) {
            // A registered chest whose block is gone (map reset, world edit, or an explosion
            // from before those were handled) would otherwise lock the owner out of ever
            // placing another one, with nothing left to break to clear it - self-heal instead
            if (isStale(existing)) {
                // Deliberately no DB delete here: the save below is a REPLACE on the same owner
                // primary key, so it overwrites the stale row anyway - and since runAsync hands
                // tasks to a thread pool rather than an ordered queue, a delete queued here could
                // just as easily land after that save and wipe the chest the player just placed.
                removeChest(existing, false);
                player.sendMessage(Messages.msg("trade-chest.previous-gone",
                        "<muted>Your previous Trade Chest no longer exists - placing a new one"));
            } else {
                event.setCancelled(true);
                Location at = existing.getLocation();
                player.sendMessage(Messages.msg("trade-chest.already-placed",
                        "<error>You already have a Trade Chest placed at {x}, {y}, {z} - break it first",
                        "x", String.valueOf(at.getBlockX()),
                        "y", String.valueOf(at.getBlockY()),
                        "z", String.valueOf(at.getBlockZ())));
                return;
            }
        }

        TradeChestData data = new TradeChestData(player.getUniqueId(), event.getBlockPlaced().getLocation());
        chestsByOwner.put(player.getUniqueId(), data);
        TradeChestRepository.getInstance().runAsyncSaveChest(data);

        player.sendMessage(Messages.msg("trade-chest.placed",
                "<success>Trade Chest placed - right click it to load items and send trade offers"));
    }

    // Anyone can break anyone's Trade Chest and walk off with it - stealing one (and everything
    // loaded into it) is fair game, so this deliberately doesn't check ownership and doesn't tell
    // the owner who took it.
    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        TradeChestData data = getChestAt(event.getBlock());
        if (data == null) return;

        // No vanilla ender chest drop (8 obsidian) - drop the Trade Chest item + its contents
        event.setDropItems(false);
        dropChestAndContents(event.getBlock(), data);
        removeChest(data);
    }

    /**
     * Called by explosion handling (TNT trail, explosive arrows) instead of letting the block
     * break naturally - explosions never fire {@link BlockBreakEvent}, so without this the chest
     * would both drop vanilla obsidian (an ender chest's non-silk-touch drop) AND leave a stale
     * {@link #chestsByOwner} entry at that coordinate. That stale entry is a dupe: it's matched
     * purely by block position ({@link #getChestAt}), so any later block placed on that same spot
     * would falsely register as "the Trade Chest" the next time someone broke it, handing out a
     * second Trade Chest (+ its old contents) for a completely unrelated block.
     *
     * @return true if a Trade Chest stood at this block (and was handled) - false otherwise, so
     * the caller knows to fall back to its normal explosion-break behaviour.
     */
    public static boolean breakByExplosion(Block block) {
        TradeChestData data = getChestAt(block);
        if (data == null) return false;

        dropChestAndContents(block, data);
        block.setType(Material.AIR);
        removeChest(data);
        return true;
    }

    // Creepers, beds/anchors, end crystals etc. still go through the normal explosion events
    // (TNT doesn't - TNTTrailListener cancels those and does its own breaking, calling
    // breakByExplosion directly). Pulling handled chests out of the block list stops vanilla
    // from also dropping the ender chest's 8 obsidian on top of what we already dropped.
    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(TradeChestManager::breakByExplosion);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(TradeChestManager::breakByExplosion);
    }

    private static void dropChestAndContents(Block block, TradeChestData data) {
        Location dropAt = block.getLocation().add(0.5, 0.5, 0.5);
        block.getWorld().dropItemNaturally(dropAt, TradeChestItem.create());
        for (ItemStack item : data.getContents())
            if (item != null && !item.getType().isAir())
                block.getWorld().dropItemNaturally(dropAt, item);
    }

    private static void removeChest(TradeChestData data) {
        removeChest(data, true);
    }

    private static void removeChest(TradeChestData data, boolean deleteFromDatabase) {
        chestsByOwner.remove(data.getOwner());
        if (deleteFromDatabase) TradeChestRepository.getInstance().runAsyncDeleteChest(data.getOwner());

        TradeOffer offer = offersByPlayer.get(data.getOwner());
        if (offer != null) cancelOffer(offer, "trade-chest.cancelled-broken", "The trade was cancelled - a Trade Chest was broken");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!enabled()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.ENDER_CHEST) return;

        TradeChestData data = getChestAt(block);
        if (data == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (!canUse(player, data.getOwner())) {
            player.sendMessage(Messages.msg("trade-chest.not-yours",
                    "<error>This is {player}'s Trade Chest", "player", nameOf(data.getOwner())));
            return;
        }

        // Once a trade is under review the preview *is* the chest for both sides - it shows both
        // players' items and each side edits their own half in it. Opening straight into it means
        // neither player has to find and press a button to see what the other one put in.
        TradeOffer offer = offersByPlayer.get(data.getOwner());
        if (offer != null && offer.getStage() == TradeOffer.Stage.REVIEW && openPreview(player, offer)) return;

        player.openInventory(new TradeChestInventory(data).getInventory());
    }

    private static @Nullable TradeChestData getChestAt(Block block) {
        // Position alone isn't enough to identify a Trade Chest - a stale entry (chest destroyed
        // without the registry being cleaned up) would otherwise match whatever unrelated block
        // a player later puts on that spot, handing them a free Trade Chest + its old contents
        // when they broke it. Requiring the block to actually still be an ender chest closes that.
        if (block.getType() != Material.ENDER_CHEST) return null;

        for (TradeChestData data : chestsByOwner.values()) {
            Location location = data.getLocation();
            // isWorldLoaded() first - a chest registered in a world that's since been unloaded or
            // deleted (map reset, world removed) has a Location whose getWorld() is null, and the
            // NPE that used to throw here aborted the whole break handler before it could suppress
            // the vanilla drop, so every Trade Chest on the server dropped 8 obsidian instead
            if (!location.isWorldLoaded()) continue;

            if (location.getWorld().equals(block.getWorld())
                    && location.getBlockX() == block.getX()
                    && location.getBlockY() == block.getY()
                    && location.getBlockZ() == block.getZ())
                return data;
        }
        return null;
    }

    /** True if the registered block is gone - world unloaded, or no longer an ender chest. */
    private static boolean isStale(TradeChestData data) {
        Location location = data.getLocation();
        return !location.isWorldLoaded() || location.getBlock().getType() != Material.ENDER_CHEST;
    }

    // Trade chest GUI interaction

    private static final int LEFT = TradeChestInventory.LEFT_BUTTON_SLOT;
    private static final int RIGHT = TradeChestInventory.RIGHT_BUTTON_SLOT;

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof TradeChestInventory chestInventory)) return;

        // Button slots are managed, everything else (0-24 + own inventory) is free editing
        if (event.getClickedInventory() == top && event.getSlot() >= LEFT) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player)
                handleButtonClick(player, chestInventory, event.getSlot());
            return;
        }

        // Double-click collect can pull the button items off their slots - just block it
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        // Shift-clicking pane/head items from the player inventory could merge them into the
        // matching button stacks at 25/26 - block those materials from being shifted in
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && event.getClickedInventory() != top) {
            ItemStack moved = event.getCurrentItem();
            if (moved != null && isButtonMaterial(moved.getType()))
                event.setCancelled(true);
        }
    }

    private static boolean isButtonMaterial(Material material) {
        return material == Material.GRAY_STAINED_GLASS_PANE
                || material == Material.LIME_STAINED_GLASS_PANE
                || material == Material.RED_STAINED_GLASS_PANE
                || material == Material.PLAYER_HEAD;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof TradeChestInventory)) return;

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot == LEFT || rawSlot == RIGHT) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof TradeChestInventory chestInventory)) return;

        TradeChestData data = chestsByOwner.get(chestInventory.getOwner());
        if (data == null) return; // chest was broken while the GUI was open

        ItemStack[] newContents = chestInventory.readContents();
        boolean changed = !Arrays.equals(data.getContents(), newContents);

        data.setContents(newContents);
        TradeChestRepository.getInstance().runAsyncSaveChest(data);

        if (!changed) return;

        TradeOffer offer = offersByPlayer.get(data.getOwner());
        if (offer == null) return;

        // Mirror the change into the shared preview (if open) and reset any accepts - nobody
        // should end up approving a different trade than the one they saw
        TradePreviewInventory preview = offer.getPreview();
        if (preview != null) preview.pushSide(data.getOwner(), data.getContents());

        if (offer.getStage() == TradeOffer.Stage.REVIEW && (offer.isFromAccepted() || offer.isToAccepted())) {
            offer.resetAccepts();
            TradeChestRepository.getInstance().runAsyncSaveOffer(offer);
            notifyBoth(offer, Messages.msg("trade-chest.accepts-reset",
                    "<error>Trade contents changed - all accepts were reset, review the trade again"));
        }
        if (preview != null) preview.refreshButtons();
    }

    // Trade preview interaction - each side's item slots are editable by that side only

    @EventHandler
    public void onPreviewClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof TradePreviewInventory preview)) return;

        TradeOffer offer = preview.getOffer();
        if (!(event.getWhoClicked() instanceof Player player)
                || offersByPlayer.get(offer.getFrom()) != offer) { // stale preview
            event.setCancelled(true);
            return;
        }

        // Double-click collect gathers matching items from every slot, including the other side
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        UUID side = actingSide(player, offer);

        if (event.getClickedInventory() == top) {
            UUID buttonSide = preview.buttonSide(event.getSlot());
            if (buttonSide != null) {
                event.setCancelled(true);
                if (buttonSide.equals(side)) {
                    if (preview.isAcceptSlot(event.getSlot())) acceptReview(side, offer);
                    else denyOffer(player, offer);
                }
                return;
            }

            if (side == null || !preview.isItemSlotOf(side, event.getSlot())) {
                event.setCancelled(true);
                return;
            }

            scheduleSyncPreview(offer);
            return;
        }

        // Clicked their own inventory - route shift-clicks into their own side manually,
        // since vanilla would drop the item into the first empty slot of either side
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
            if (side == null) return;

            ItemStack moved = event.getCurrentItem();
            if (moved == null || moved.getType().isAir()) return;

            int slot = preview.firstEmptySlotOf(side);
            if (slot == -1) return;

            top.setItem(slot, moved.clone());
            event.setCurrentItem(null);
            scheduleSyncPreview(offer);
        }
    }

    @EventHandler
    public void onPreviewDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof TradePreviewInventory preview)) return;

        TradeOffer offer = preview.getOffer();
        if (!(event.getWhoClicked() instanceof Player player)
                || offersByPlayer.get(offer.getFrom()) != offer) {
            event.setCancelled(true);
            return;
        }

        UUID side = actingSide(player, offer);
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < 54 && (side == null || !preview.isItemSlotOf(side, rawSlot))) {
                event.setCancelled(true);
                return;
            }
        }

        scheduleSyncPreview(offer);
    }

    @EventHandler
    public void onPreviewClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof TradePreviewInventory preview)) return;

        TradeOffer offer = preview.getOffer();
        syncPreview(offer);

        // Drop the shared instance once the last viewer is gone (checked next tick, since the
        // closing viewer is still in the list during this event)
        ScheduleUtil.scheduleSync(() -> {
            if (offer.getPreview() == preview && preview.getInventory().getViewers().isEmpty())
                offer.setPreview(null);
        }, 1, ScheduleUtil.Unit.TICK);
    }

    private static void scheduleSyncPreview(TradeOffer offer) {
        // A tick later, so the inventory reflects the click's result
        ScheduleUtil.scheduleSync(() -> syncPreview(offer), 1, ScheduleUtil.Unit.TICK);
    }

    // Persists preview edits into the backing chests; edits reset any accepts already given
    private static void syncPreview(TradeOffer offer) {
        TradePreviewInventory preview = offer.getPreview();
        if (preview == null || offersByPlayer.get(offer.getFrom()) != offer) return;

        boolean changed = syncSide(offer.getFrom(), preview) | syncSide(offer.getTo(), preview);

        if (changed && (offer.isFromAccepted() || offer.isToAccepted())) {
            offer.resetAccepts();
            TradeChestRepository.getInstance().runAsyncSaveOffer(offer);
            notifyBoth(offer, Messages.msg("trade-chest.accepts-reset",
                    "<error>Trade contents changed - all accepts were reset, review the trade again"));
        }
        preview.refreshButtons();
    }

    private static boolean syncSide(UUID side, TradePreviewInventory preview) {
        TradeChestData chest = chestsByOwner.get(side);
        if (chest == null) return false;

        ItemStack[] read = preview.readSide(side);
        if (Arrays.equals(chest.getContents(), read)) return false;

        chest.setContents(read);
        TradeChestRepository.getInstance().runAsyncSaveChest(chest);
        return true;
    }

    // Buttons (state-dependent) - built here so TradeChestInventory stays dumb

    static ItemStack buildLeftButton(UUID owner) {
        TradeOffer offer = offersByPlayer.get(owner);

        if (offer != null && offer.getStage() == TradeOffer.Stage.PENDING && offer.getTo().equals(owner)) {
            return new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                    .withName(Messages.msg("trade-chest.button-accept-offer",
                            "<green>Accept trade offer from {player}", "player", nameOf(offer.getFrom())))
                    .build();
        }

        if (offer != null && offer.getStage() == TradeOffer.Stage.REVIEW) {
            return new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                    .withName(Messages.msg("trade-chest.button-open-preview", "<green>Open the trade preview"))
                    .build();
        }

        return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .withName(Component.empty())
                .build();
    }

    static ItemStack buildRightButton(UUID owner) {
        TradeOffer offer = offersByPlayer.get(owner);

        if (offer == null) {
            return new ItemBuilder(Material.PLAYER_HEAD)
                    .withName(Messages.msg("trade-chest.button-send", "<primary>Send a trade offer"))
                    .withLore(Messages.msg("trade-chest.button-send-lore",
                            "Pick another player's Trade Chest to offer your items to"))
                    .build();
        }

        if (offer.getStage() == TradeOffer.Stage.PENDING && offer.getFrom().equals(owner)) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(offer.getTo());
            return new ItemBuilder(Material.PLAYER_HEAD)
                    .withItemMeta(SkullMeta.class, meta -> meta.setOwningPlayer(target))
                    .withName(Messages.msg("trade-chest.button-outgoing",
                            "Offer sent to {player}", "player", nameOf(offer.getTo())))
                    .withLore(Messages.msg("trade-chest.button-outgoing-lore", "<error>Click to cancel the offer"))
                    .build();
        }

        return new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .withName(offer.getStage() == TradeOffer.Stage.PENDING
                        ? Messages.msg("trade-chest.button-deny-offer", "<red>Deny trade offer")
                        : Messages.msg("trade-chest.button-cancel-trade", "<red>Cancel the trade"))
                .build();
    }

    private static void handleButtonClick(Player player, TradeChestInventory chestInventory, int slot) {
        UUID owner = chestInventory.getOwner();
        TradeOffer offer = offersByPlayer.get(owner);

        if (slot == RIGHT) {
            if (offer == null) {
                List<TradeChestData> targets = new ArrayList<>();
                for (TradeChestData data : chestsByOwner.values())
                    if (!data.getOwner().equals(owner)) targets.add(data);

                if (targets.isEmpty()) {
                    player.sendMessage(Messages.msg("trade-chest.no-targets",
                            "<error>No other players have a Trade Chest placed right now"));
                    return;
                }

                // Opening the list closes this GUI, which saves the contents via onClose
                player.openInventory(new TradeTargetListInventory(player, owner, targets).getInventory());
            } else {
                denyOffer(player, offer);
            }
            return;
        }

        if (slot == LEFT && offer != null) {
            if (offer.getStage() == TradeOffer.Stage.PENDING && offer.getTo().equals(owner)) {
                offer.setStage(TradeOffer.Stage.REVIEW);
                TradeChestRepository.getInstance().runAsyncSaveOffer(offer);

                Player initiator = Bukkit.getPlayer(offer.getFrom());
                if (initiator != null)
                    initiator.sendMessage(Messages.msg("trade-chest.offer-accepted",
                            "<success>{player} accepted your trade offer - review the trade in your Trade Chest",
                            "player", player.getName()));

                // Anyone sitting in either Trade Chest right now (the initiator especially) gets
                // moved straight into the shared preview - no button to press, no reopening, and
                // they see the other side's items the moment the trade goes under review
                openPreview(player, offer);
                movePreviewViewersIn(offer);
            } else if (offer.getStage() == TradeOffer.Stage.REVIEW) {
                openPreview(player, offer);
            }
        }
    }

    // Offer lifecycle

    static void sendOffer(Player actor, UUID fromOwner, UUID to) {
        if (offersByPlayer.containsKey(fromOwner)) {
            actor.sendMessage(Messages.msg("trade-chest.already-trading",
                    "<error>This Trade Chest is already in an active trade - finish or cancel it first"));
            return;
        }
        if (offersByPlayer.containsKey(to)) {
            actor.sendMessage(Messages.msg("trade-chest.target-busy",
                    "<error>{player} is already in an active trade", "player", nameOf(to)));
            return;
        }
        if (!chestsByOwner.containsKey(to) || !chestsByOwner.containsKey(fromOwner)) return;

        TradeOffer offer = new TradeOffer(fromOwner, to);
        offersByPlayer.put(offer.getFrom(), offer);
        offersByPlayer.put(offer.getTo(), offer);
        TradeChestRepository.getInstance().runAsyncSaveOffer(offer);

        actor.sendMessage(Messages.msg("trade-chest.offer-sent",
                "<success>Trade offer sent to {player}'s Trade Chest", "player", nameOf(to)));

        Player target = Bukkit.getPlayer(to);
        if (target != null)
            target.sendMessage(Messages.msg("trade-chest.offer-received",
                    "<primary>{player} sent a trade offer to your Trade Chest - open it to accept or deny",
                    "player", nameOf(fromOwner)));

        // If either side already has their Trade Chest GUI open, flip the buttons live instead
        // of waiting for them to close and reopen it
        refreshOpenChests(fromOwner);
        refreshOpenChests(to);
    }

    /**
     * Puts the player back into the Trade Chest GUI they opened the target list from, rather than
     * dropping them out to the world - picking a trade partner shouldn't cost them the chest.
     */
    static void reopenChest(Player player, UUID owner) {
        TradeChestData data = chestsByOwner.get(owner);
        if (data == null) {
            player.closeInventory();
            return;
        }

        // A tick later - opening an inventory from inside the click event that's still being
        // processed leaves the client's view out of sync with the server's
        ScheduleUtil.scheduleSync(() -> player.openInventory(new TradeChestInventory(data).getInventory()),
                1, ScheduleUtil.Unit.TICK);
    }

    /**
     * Pulls everyone currently sitting in either side's Trade Chest GUI into the shared preview.
     * Closing their chest first is deliberate: {@link #onClose} is what writes their live, still
     * unsaved edits into the chest data (and mirrors them into an existing preview), so doing it
     * the other way round would show a preview built from what they had a moment ago.
     */
    private static void movePreviewViewersIn(TradeOffer offer) {
        for (Player viewer : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            if (!(viewer.getOpenInventory().getTopInventory().getHolder() instanceof TradeChestInventory chestGui))
                continue;
            if (!chestGui.getOwner().equals(offer.getFrom()) && !chestGui.getOwner().equals(offer.getTo()))
                continue;

            viewer.closeInventory();
            openPreview(viewer, offer);
        }
    }

    /** @return false if there's no preview to show (a chest went missing) and nothing was opened. */
    private static boolean openPreview(Player player, TradeOffer offer) {
        TradePreviewInventory preview = offer.getPreview();
        if (preview == null) {
            TradeChestData fromChest = chestsByOwner.get(offer.getFrom());
            TradeChestData toChest = chestsByOwner.get(offer.getTo());
            if (fromChest == null || toChest == null) return false;

            preview = new TradePreviewInventory(offer, fromChest, toChest);
            offer.setPreview(preview);
        }

        player.openInventory(preview.getInventory());
        return true;
    }

    static void acceptReview(UUID side, TradeOffer offer) {
        if (offersByPlayer.get(offer.getFrom()) != offer) return; // stale

        offer.setAccepted(side, true);
        TradeChestRepository.getInstance().runAsyncSaveOffer(offer);

        TradePreviewInventory preview = offer.getPreview();
        if (preview != null) preview.refreshButtons();

        if (offer.bothAccepted()) executeTrade(offer);
    }

    static void denyOffer(Player player, TradeOffer offer) {
        if (offersByPlayer.get(offer.getFrom()) != offer) return; // stale

        cancelOffer(offer, "trade-chest.cancelled", "The trade was cancelled by {player}", player.getName());
    }

    private static void cancelOffer(TradeOffer offer, String messageKey, String messageDefault, String... placeholderValues) {
        offersByPlayer.remove(offer.getFrom());
        offersByPlayer.remove(offer.getTo());
        TradeChestRepository.getInstance().runAsyncDeleteOffer(offer.getFrom());

        String[] placeholders = placeholderValues.length > 0
                ? new String[]{"player", placeholderValues[0]}
                : new String[0];
        notifyBoth(offer, Messages.msg(messageKey, "<error>" + messageDefault, placeholders));
        closePreview(offer);
        refreshOpenChests(offer.getFrom());
        refreshOpenChests(offer.getTo());
    }

    private static void executeTrade(TradeOffer offer) {
        TradeChestData fromChest = chestsByOwner.get(offer.getFrom());
        TradeChestData toChest = chestsByOwner.get(offer.getTo());
        if (fromChest == null || toChest == null) return;

        // The preview is the live editing surface - make sure the last edits are in the data
        TradePreviewInventory preview = offer.getPreview();
        if (preview != null) {
            fromChest.setContents(preview.readSide(offer.getFrom()));
            toChest.setContents(preview.readSide(offer.getTo()));
        }

        offersByPlayer.remove(offer.getFrom());
        offersByPlayer.remove(offer.getTo());
        TradeChestRepository.getInstance().runAsyncDeleteOffer(offer.getFrom());

        scheduleDeliveries(fromChest, offer.getTo());
        scheduleDeliveries(toChest, offer.getFrom());

        TradeChestRepository.getInstance().runAsyncSaveChest(fromChest);
        TradeChestRepository.getInstance().runAsyncSaveChest(toChest);

        // Both chests were just emptied into deliveries - anyone still looking at one of them
        // (a teammate with the GUI open, or the accepting player themself once the preview
        // closes) is showing the pre-trade items, and would write them straight back over the
        // emptied chest on close. Push the new contents into every open GUI instead.
        pushToOpenGuis(offer.getFrom(), fromChest);
        pushToOpenGuis(offer.getTo(), toChest);

        awardTradeAchievement(offer.getFrom());
        awardTradeAchievement(offer.getTo());

        long minutes = deliveryIntervalMillis() / 60_000L;
        notifyBoth(offer, Messages.msg("trade-chest.trade-complete",
                "<success>Trade accepted! Deliveries arrive at your Trade Chest one stack every {minutes} minutes",
                "minutes", String.valueOf(minutes)));

        closePreview(offer);
    }

    // Both sides earn it, and only if they're online to see it - an offline owner whose teammate
    // closed the trade for them gets it the next time they complete one themselves
    private static void awardTradeAchievement(UUID owner) {
        Player player = Bukkit.getPlayer(owner);
        if (player != null) AchievementManager.award(player, Achievement.ENDER_EXPRESS);
    }

    // Empties the chest into scheduled deliveries for the other player, one stack every interval
    private static void scheduleDeliveries(TradeChestData senderChest, UUID recipient) {
        long base = System.currentTimeMillis();
        long interval = deliveryIntervalMillis();
        int scheduled = 0;

        ItemStack[] contents = senderChest.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) continue;
            contents[i] = null;

            TradeDelivery delivery = new TradeDelivery(recipient, senderChest.getOwner(), item, base + (++scheduled) * interval);
            TradeChestRepository.getInstance().runAsyncInsertDelivery(delivery, deliveries);
        }
    }

    // Deliveries

    private static void deliveryTick() {
        if (!enabled()) return;

        long now = System.currentTimeMillis();

        List<TradeDelivery> due = new ArrayList<>();
        for (TradeDelivery delivery : deliveries)
            if (delivery.getDeliverAt() <= now) due.add(delivery);

        for (TradeDelivery delivery : due) {
            if (!deliveries.contains(delivery)) continue; // already returned by a full-chest cancel below

            TradeChestData chest = chestsByOwner.get(delivery.getRecipient());

            // If someone has the GUI open right now, their live (unsaved) edits are the truth -
            // sync them in first so the delivery lands on top instead of clobbering them
            TradeChestInventory openGui = findOpenChestGui(delivery.getRecipient());
            if (chest != null && openGui != null) chest.setContents(openGui.readContents());

            if (chest != null && chest.addItem(delivery.getItem())) {
                deliveries.remove(delivery);
                TradeChestRepository.getInstance().runAsyncDeleteDelivery(delivery.getId());
                TradeChestRepository.getInstance().runAsyncSaveChest(chest);
                if (openGui != null) pushContents(openGui, chest);

                Player recipient = Bukkit.getPlayer(delivery.getRecipient());
                if (recipient != null) {
                    long remaining = deliveries.stream().filter(d -> d.getRecipient().equals(delivery.getRecipient())).count();
                    recipient.sendMessage(Messages.msg("trade-chest.delivered",
                            "<success>Delivered to your Trade Chest: {amount}x {item} from {player}"
                                    + (remaining > 0 ? " <muted>({remaining} more incoming)" : ""),
                            "amount", String.valueOf(delivery.getItem().getAmount()),
                            "item", delivery.getItem().getType().name().toLowerCase().replace('_', ' '),
                            "player", nameOf(delivery.getSender()),
                            "remaining", String.valueOf(remaining)));
                }
            } else {
                // Full (or chest gone) - cancel this and every remaining delivery from this
                // sender to this recipient, returning the items to the sender's chest
                returnRemainingDeliveries(delivery.getRecipient(), delivery.getSender());
            }
        }
    }

    private static void returnRemainingDeliveries(UUID recipient, UUID sender) {
        List<TradeDelivery> group = new ArrayList<>();
        for (TradeDelivery delivery : deliveries)
            if (delivery.getRecipient().equals(recipient) && delivery.getSender().equals(sender))
                group.add(delivery);

        TradeChestData senderChest = chestsByOwner.get(sender);
        TradeChestInventory senderGui = findOpenChestGui(sender);
        if (senderChest != null && senderGui != null) senderChest.setContents(senderGui.readContents());

        for (TradeDelivery delivery : group) {
            deliveries.remove(delivery);
            TradeChestRepository.getInstance().runAsyncDeleteDelivery(delivery.getId());

            if (senderChest != null && senderChest.addItem(delivery.getItem())) continue;

            // Nowhere to return it either - drop at whichever trade chest still exists
            TradeChestData dropChest = senderChest != null ? senderChest : chestsByOwner.get(recipient);
            if (dropChest != null) {
                Location dropAt = dropChest.getLocation().clone().add(0.5, 1.0, 0.5);
                dropAt.getWorld().dropItemNaturally(dropAt, delivery.getItem());
            }
        }

        if (senderChest != null) {
            TradeChestRepository.getInstance().runAsyncSaveChest(senderChest);
            if (senderGui != null) pushContents(senderGui, senderChest);
        }

        Component message = Messages.msg("trade-chest.delivery-cancelled",
                "<error>A delivery didn't fit in {recipient}'s Trade Chest - the remaining {count} item stack(s) were returned to {sender}",
                "recipient", nameOf(recipient), "sender", nameOf(sender), "count", String.valueOf(group.size()));
        Player recipientPlayer = Bukkit.getPlayer(recipient);
        Player senderPlayer = Bukkit.getPlayer(sender);
        if (recipientPlayer != null) recipientPlayer.sendMessage(message);
        if (senderPlayer != null) senderPlayer.sendMessage(message);
    }

    // Helpers

    private static void notifyBoth(TradeOffer offer, Component message) {
        Player from = Bukkit.getPlayer(offer.getFrom());
        Player to = Bukkit.getPlayer(offer.getTo());
        if (from != null) from.sendMessage(message);
        if (to != null) to.sendMessage(message);
    }

    // Whether the trade completed or was cancelled, everyone watching the preview gets put back
    // into their own Trade Chest rather than dumped out to the world - the chest they see is
    // rebuilt from the post-trade data, so nobody has to reopen it to find out what happened.
    private static void closePreview(TradeOffer offer) {
        // Deferred a tick - may run from inside inventory events
        ScheduleUtil.scheduleSync(() -> {
            TradePreviewInventory preview = offer.getPreview();
            if (preview == null) return;

            for (HumanEntity viewer : new ArrayList<>(preview.getInventory().getViewers())) {
                UUID side = viewer instanceof Player player ? actingSide(player, offer) : null;
                TradeChestData chest = side == null ? null : chestsByOwner.get(side);

                if (chest != null) viewer.openInventory(new TradeChestInventory(chest).getInventory());
                else viewer.closeInventory();
            }
            offer.setPreview(null);
        }, 1, ScheduleUtil.Unit.TICK);
    }

    // Teammates share a chest, so more than one GUI for the same owner can be open at once
    private static List<TradeChestInventory> openChestGuis(UUID owner) {
        List<TradeChestInventory> open = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof TradeChestInventory chestInventory
                    && chestInventory.getOwner().equals(owner))
                open.add(chestInventory);
        }
        return open;
    }

    private static @Nullable TradeChestInventory findOpenChestGui(UUID owner) {
        List<TradeChestInventory> open = openChestGuis(owner);
        return open.isEmpty() ? null : open.getFirst();
    }

    private static void pushToOpenGuis(UUID owner, TradeChestData data) {
        for (TradeChestInventory gui : openChestGuis(owner))
            pushContents(gui, data);
    }

    // Writes the stored contents into the open GUI - only safe right after the GUI's own live
    // contents were synced into the data (see the delivery flows), never with stale data
    private static void pushContents(TradeChestInventory gui, TradeChestData data) {
        for (int i = 0; i < TradeChestInventory.ITEM_SLOTS; i++)
            gui.getInventory().setItem(i, data.getContents()[i]);
        gui.refreshButtons();
    }

    // Only re-renders the button slots - item slots are the player's live editing surface
    private static void refreshOpenChests(UUID owner) {
        for (TradeChestInventory gui : openChestGuis(owner))
            gui.refreshButtons();
    }

    private static String nameOf(UUID id) {
        String name = Bukkit.getOfflinePlayer(id).getName();
        return name != null ? name : id.toString();
    }
}
