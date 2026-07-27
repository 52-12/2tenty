package threeadd.glimpsy.feature.tradechest;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class TradeDelivery {

    private long id = -1;
    private final UUID recipient;
    private final UUID sender;
    private final ItemStack item;
    private final long deliverAt;

    public TradeDelivery(UUID recipient, UUID sender, ItemStack item, long deliverAt) {
        this.recipient = recipient;
        this.sender = sender;
        this.item = item;
        this.deliverAt = deliverAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UUID getRecipient() {
        return recipient;
    }

    public UUID getSender() {
        return sender;
    }

    public ItemStack getItem() {
        return item;
    }

    public long getDeliverAt() {
        return deliverAt;
    }
}
