package threeadd.glimpsy.util.inventory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.Glimpsy;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class CustomInventory implements InventoryHolder {

    protected static final ItemStack PLACEHOLDER = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
            .withName(Component.text("Glimpsy.Minehut.gg")
                    .color(TextColor.color(0xC4C4C4)))
            .build();

    private final Map<Integer, @Nullable Consumer<InventoryClickEvent>> slotClickEvents = new HashMap<>();
    private final @Nullable Player viewer;
    private Inventory inventory;

    public CustomInventory(@NotNull Rows rows, Component inventoryTitle) {
        this(rows, inventoryTitle, null);
    }

    public CustomInventory(@NotNull Rows rows, Component inventoryTitle, @Nullable Player viewer) {
        this.inventory = Bukkit.createInventory(this, rows.getTotalSlots(), inventoryTitle);
        this.viewer = viewer;

        for (int slot = 0; slot < rows.getTotalSlots(); slot++) {
            setSlot(slot, PLACEHOLDER);
        }
    }

    protected void setSlot(int slot, @Nullable Consumer<InventoryClickEvent> event) {
        setSlot(slot, null, event);
    }

    protected void setSlot(int slot, ItemStack item) {
        setSlot(slot, item, null);
    }

    protected void setSlot(int slot, @Nullable ItemStack item, @Nullable Consumer<InventoryClickEvent> event) {
        inventory.setItem(slot, item);
        slotClickEvents.put(slot, event);
    }

    protected void setUpdatingSlot(int slot, @NotNull Supplier<@Nullable ItemStack> itemSupplier, @NotNull Supplier<@Nullable Consumer<InventoryClickEvent>> eventSupplier, int interval, TimeUnit intervalUnit) {
        long millis = intervalUnit.toMillis(interval);
        long ticks = Math.max(1, millis / 50);
        Bukkit.getScheduler().runTaskTimer(
                Glimpsy.getInstance(), task -> {
                    if (getInventory().getViewers().isEmpty()) {
                        task.cancel();
                        return;
                    }

                    setSlot(slot, itemSupplier.get(), eventSupplier.get());
                },
                0L,
                ticks);
    }

    protected void setTransferSlot(int slot, @Nullable ItemStack item, Supplier<CustomInventory> inventorySupplier) {
        setSlot(slot, item, event -> {
            CustomInventory newInventory = inventorySupplier.get();
            event.getView().getPlayer().openInventory(newInventory.getInventory());
        });
    }

    /**
     * Change the title of this CustomInventory
     *
     * @param newTitle The new Component to apply to the title of this CustomInventory
     * @apiNote Because bukkit title's of Bukkit Inventories are immutable,
     * this will create a new Inventory instance whilst copying all contents and open the new Inventory
     * to all viewers of the old one.
     */
    protected void setTitle(Component newTitle) {
        Inventory newInventory = Bukkit.createInventory(this, inventory.getSize(), newTitle);

        newInventory.setContents(inventory.getContents());
        for (HumanEntity viewer : inventory.getViewers())
            viewer.openInventory(newInventory);

        inventory = newInventory;
    }

    public void handleSlotClickEvent(InventoryClickEvent event) {
        event.setCancelled(true);

        Consumer<InventoryClickEvent> consumer = slotClickEvents.get(event.getSlot());
        if (consumer != null)
            consumer.accept(event);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public @NotNull Player getViewer() {
        if (viewer == null) throw new IllegalStateException("This inventory wasn't init with a viewer");
        return viewer;
    }

    public enum Rows {
        ONE(), TWO(), THREE(), FOUR(), FIVE(), SIX();

        public int getTotalSlots() {
            return (ordinal() + 1) * 9;
        }

        public LinkedHashSet<Integer> getAllSlots() {
            int invSize = getTotalSlots();
            LinkedHashSet<Integer> list = new LinkedHashSet<>(invSize);
            for (int i = 0; i < invSize; i++) list.add(i);
            return list;
        }

        public LinkedHashSet<Integer> getInnerSlots() {
            int totalRows = getTotalSlots() / 9;
            LinkedHashSet<Integer> innerSlots = new LinkedHashSet<>();

            for (int row = 1; row < totalRows - 1; row++) {
                for (int col = 1; col < 9 - 1; col++) {
                    int slotIndex = row * 9 + col;
                    innerSlots.add(slotIndex);
                }
            }

            return innerSlots;
        }
    }
}
