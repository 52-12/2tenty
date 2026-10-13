package threeadd.glimpsy.feature.tradechest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.inventory.ItemBuilder;
import threeadd.glimpsy.util.inventory.ListInventory;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * The head-per-player list opened from a Trade Chest's button - everyone who currently has a
 * Trade Chest placed (except the viewer), click one to send them a trade offer.
 */
public class TradeTargetListInventory extends ListInventory<TradeChestData> {

    // The chest the offer is sent from - its owner, not necessarily the viewer (teammates can
    // act on the owner's behalf)
    private final UUID fromOwner;

    public TradeTargetListInventory(Player viewer, UUID fromOwner, Collection<TradeChestData> targets) {
        super(Rows.FOUR,
                Messages.blackTitle("trade-chest.target-list-title", "Send Trade Offer To..."),
                targets, Rows.FOUR.getInnerSlots(), viewer);
        this.fromOwner = fromOwner;
    }

    @Override
    protected @NotNull ItemStack mapToItem(@NotNull TradeChestData target) {
        OfflinePlayer owner = Bukkit.getOfflinePlayer(target.getOwner());
        String name = owner.getName() != null ? owner.getName() : target.getOwner().toString();

        return new ItemBuilder(Material.PLAYER_HEAD)
                .withItemMeta(SkullMeta.class, meta -> meta.setOwningPlayer(owner))
                .withName(Component.text(name).color(NamedTextColor.WHITE))
                .withLore(Messages.msg("trade-chest.target-lore", "Click to send a trade offer to {player}'s Trade Chest",
                        "player", name))
                .build();
    }

    @Override
    protected @Nullable Consumer<InventoryClickEvent> mapClick(@NotNull TradeChestData target) {
        return event -> {
            Player player = getViewer();
            TradeChestManager.sendOffer(player, fromOwner, target.getOwner());
            // Back to the chest rather than out to the world - whether the offer went through or
            // was rejected (target busy, etc.), the player stays where they were working
            TradeChestManager.reopenChest(player, fromOwner);
        };
    }
}
