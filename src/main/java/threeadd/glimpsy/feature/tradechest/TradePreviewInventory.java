package threeadd.glimpsy.feature.tradechest;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.inventory.ItemBuilder;

import java.util.UUID;

/**
 * The combined double-chest trade preview: initiator's items on the top half, target's on the
 * bottom, each side ending in that player's own Accept (lime pane) / Deny (red pane) buttons.
 * <p>
 * One shared instance per offer - every viewer (both sides, including teammates acting for a
 * side) looks at the same live inventory, so edits and accepts appear for everyone instantly.
 * Each side's 25 item slots are freely editable by that side's players (so items don't have to
 * be loaded before the offer), the other side is read-only; permissions and persistence are
 * enforced in {@link TradeChestManager}'s listeners.
 */
public class TradePreviewInventory implements InventoryHolder {

    public static final int SIDE_SIZE = TradeChestData.SIZE;
    public static final int TOP_ACCEPT = 25;
    public static final int TOP_DENY = 26;
    public static final int BOTTOM_ACCEPT = 52;
    public static final int BOTTOM_DENY = 53;

    private final Inventory inventory;
    private final TradeOffer offer;

    public TradePreviewInventory(TradeOffer offer, TradeChestData fromChest, TradeChestData toChest) {
        this.offer = offer;
        this.inventory = Bukkit.createInventory(this, 54,
                Messages.blackTitle("trade-chest.preview-title", "Trade Preview"));

        for (int i = 0; i < SIDE_SIZE; i++) {
            inventory.setItem(i, fromChest.getContents()[i]);
            inventory.setItem(27 + i, toChest.getContents()[i]);
        }

        refreshButtons();
    }

    /** True if the raw slot is one of {@code side}'s 25 editable item slots. */
    public boolean isItemSlotOf(UUID side, int slot) {
        if (side.equals(offer.getFrom())) return slot >= 0 && slot < SIDE_SIZE;
        if (side.equals(offer.getTo())) return slot >= 27 && slot < 27 + SIDE_SIZE;
        return false;
    }

    public @Nullable UUID buttonSide(int slot) {
        if (slot == TOP_ACCEPT || slot == TOP_DENY) return offer.getFrom();
        if (slot == BOTTOM_ACCEPT || slot == BOTTOM_DENY) return offer.getTo();
        return null;
    }

    public boolean isAcceptSlot(int slot) {
        return slot == TOP_ACCEPT || slot == BOTTOM_ACCEPT;
    }

    /** The current 25 item slots of one side, empty slots as null. */
    public @Nullable ItemStack[] readSide(UUID side) {
        int offset = side.equals(offer.getFrom()) ? 0 : 27;
        ItemStack[] contents = new ItemStack[SIDE_SIZE];
        for (int i = 0; i < SIDE_SIZE; i++) {
            ItemStack item = inventory.getItem(offset + i);
            contents[i] = item == null || item.getType().isAir() ? null : item.clone();
        }
        return contents;
    }

    public void pushSide(UUID side, @Nullable ItemStack[] contents) {
        int offset = side.equals(offer.getFrom()) ? 0 : 27;
        for (int i = 0; i < SIDE_SIZE; i++)
            inventory.setItem(offset + i, contents[i]);
    }

    /** First empty item slot of the side, or -1 - used for manual shift-click routing. */
    public int firstEmptySlotOf(UUID side) {
        int offset = side.equals(offer.getFrom()) ? 0 : 27;
        for (int i = 0; i < SIDE_SIZE; i++) {
            ItemStack item = inventory.getItem(offset + i);
            if (item == null || item.getType().isAir()) return offset + i;
        }
        return -1;
    }

    public void refreshButtons() {
        inventory.setItem(TOP_ACCEPT, acceptPane(offer.getFrom()));
        inventory.setItem(TOP_DENY, denyPane(offer.getFrom()));
        inventory.setItem(BOTTOM_ACCEPT, acceptPane(offer.getTo()));
        inventory.setItem(BOTTOM_DENY, denyPane(offer.getTo()));
    }

    private static String nameOf(UUID id) {
        String name = Bukkit.getOfflinePlayer(id).getName();
        return name != null ? name : id.toString();
    }

    private ItemStack acceptPane(UUID side) {
        boolean accepted = offer.isAccepted(side);
        return new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                .withName(Messages.msg("trade-chest.accept-pane", "<green>{player} - Accept", "player", nameOf(side)))
                .withLore(accepted
                        ? Messages.msg("trade-chest.accept-pane-done", "<green>Accepted - waiting for the other side")
                        : Messages.msg("trade-chest.accept-pane-hint", "<muted>Only {player}'s side can click this", "player", nameOf(side)))
                .withEnchantedStated(accepted)
                .build();
    }

    private ItemStack denyPane(UUID side) {
        return new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .withName(Messages.msg("trade-chest.deny-pane", "<red>{player} - Deny", "player", nameOf(side)))
                .build();
    }

    public TradeOffer getOffer() {
        return offer;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
