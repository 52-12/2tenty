package threeadd.glimpsy.feature.tradechest;

import java.util.UUID;

public class TradeOffer {

    public enum Stage {
        /** Sent, waiting for the target to accept or deny in their own trade chest. */
        PENDING,
        /** Target accepted - both sides review the combined preview and must both accept. */
        REVIEW
    }

    private final UUID from;
    private final UUID to;
    private Stage stage = Stage.PENDING;
    private boolean fromAccepted;
    private boolean toAccepted;
    // The one shared live preview GUI for this offer, while anyone has it open - not persisted
    private TradePreviewInventory preview;

    public TradeOffer(UUID from, UUID to) {
        this.from = from;
        this.to = to;
    }

    public UUID getFrom() {
        return from;
    }

    public UUID getTo() {
        return to;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public boolean isFromAccepted() {
        return fromAccepted;
    }

    public boolean isToAccepted() {
        return toAccepted;
    }

    public void setAccepted(UUID player, boolean accepted) {
        if (player.equals(from)) fromAccepted = accepted;
        else if (player.equals(to)) toAccepted = accepted;
    }

    public boolean isAccepted(UUID player) {
        return player.equals(from) ? fromAccepted : toAccepted;
    }

    public boolean bothAccepted() {
        return fromAccepted && toAccepted;
    }

    public void resetAccepts() {
        fromAccepted = false;
        toAccepted = false;
    }

    public TradePreviewInventory getPreview() {
        return preview;
    }

    public void setPreview(TradePreviewInventory preview) {
        this.preview = preview;
    }

    public boolean involves(UUID player) {
        return from.equals(player) || to.equals(player);
    }

    public UUID other(UUID player) {
        return from.equals(player) ? to : from;
    }
}
