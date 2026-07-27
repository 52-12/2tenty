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
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;
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
        if (chestsByOwner.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(Messages.msg("trade-chest.already-placed",
                    "<error>You already have a Trade Chest placed - break it first"));
            return;
        }

        TradeChestData data = new TradeChestData(player.getUniqueId(), event.getBlockPlaced().getLocation());
        chestsByOwner.put(player.getUniqueId(), data);
        TradeChestRepository.getInstance().runAsyncSaveChest(data);

        player.sendMessage(Messages.msg("trade-chest.placed",
                "<success>Trade Chest placed - right click it to load items and send trade offers"));
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        TradeChestData data = getChestAt(event.getBlock());
        if (data == null) return;

        // No vanilla ender chest drop (8 obsidian) - drop the Trade Chest item + its contents
        event.setDropItems(false);
        Location dropAt = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
        event.getBlock().getWorld().dropItemNaturally(dropAt, TradeChestItem.create());
        for (ItemStack item : data.getContents())
            if (item != null && !item.getType().isAir())
                event.getBlock().getWorld().dropItemNaturally(dropAt, item);

        chestsByOwner.remove(data.getOwner());
        TradeChestRepository.getInstance().runAsyncDeleteChest(data.getOwner());

        TradeOffer offer = offersByPlayer.get(data.getOwner());
        if (offer != null) cancelOffer(offer, "trade-chest.cancelled-broken", "The trade was cancelled - a Trade Chest was broken");

        Player owner = Bukkit.getPlayer(data.getOwner());
        if (owner != null && !owner.getUniqueId().equals(event.getPlayer().getUniqueId())) {
            owner.sendMessage(Messages.msg("trade-chest.broken-by-other",
                    "<error>{player} broke your Trade Chest", "player", event.getPlayer().getName()));
        }
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

        player.openInventory(new TradeChestInventory(data).getInventory());
    }

    private static @Nullable TradeChestData getChestAt(Block block) {
        for (TradeChestData data : chestsByOwner.values())
            if (data.getLocation().getWorld().equals(block.getWorld())
                    && data.getLocation().getBlockX() == block.getX()
                    && data.getLocation().getBlockY() == block.getY()
                    && data.getLocation().getBlockZ() == block.getZ())
                return data;
        return null;
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
                            "<success>{player} accepted your trade offer - open your Trade Chest to review the trade",
                            "player", player.getName()));

                openPreview(player, offer);
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
    }

    private static void openPreview(Player player, TradeOffer offer) {
        TradePreviewInventory preview = offer.getPreview();
        if (preview == null) {
            TradeChestData fromChest = chestsByOwner.get(offer.getFrom());
            TradeChestData toChest = chestsByOwner.get(offer.getTo());
            if (fromChest == null || toChest == null) return;

            preview = new TradePreviewInventory(offer, fromChest, toChest);
            offer.setPreview(preview);
        }

        player.openInventory(preview.getInventory());
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

        long minutes = deliveryIntervalMillis() / 60_000L;
        notifyBoth(offer, Messages.msg("trade-chest.trade-complete",
                "<success>Trade accepted! Deliveries arrive at your Trade Chest one stack every {minutes} minutes",
                "minutes", String.valueOf(minutes)));

        closePreview(offer);
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

    private static void closePreview(TradeOffer offer) {
        // Deferred a tick - may run from inside inventory events
        ScheduleUtil.scheduleSync(() -> {
            TradePreviewInventory preview = offer.getPreview();
            if (preview == null) return;

            for (HumanEntity viewer : new ArrayList<>(preview.getInventory().getViewers()))
                viewer.closeInventory();
            offer.setPreview(null);
        }, 1, ScheduleUtil.Unit.TICK);
    }

    private static @Nullable TradeChestInventory findOpenChestGui(UUID owner) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof TradeChestInventory chestInventory
                    && chestInventory.getOwner().equals(owner))
                return chestInventory;
        }
        return null;
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
        TradeChestInventory gui = findOpenChestGui(owner);
        if (gui != null) gui.refreshButtons();
    }

    private static String nameOf(UUID id) {
        String name = Bukkit.getOfflinePlayer(id).getName();
        return name != null ? name : id.toString();
    }
}
