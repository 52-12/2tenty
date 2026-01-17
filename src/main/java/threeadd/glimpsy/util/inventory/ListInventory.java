package threeadd.glimpsy.util.inventory;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;

public abstract class ListInventory<T> extends CustomInventory {

    private static final ItemStack PREVIOUS_PAGE_ITEM = new ItemBuilder(Material.ARROW)
            .withName(Component.text("Previous Page"))
            .build();
    private static final ItemStack NEXT_PAGE_ITEM = new ItemBuilder(Material.ARROW)
            .withName(Component.text("Next Page"))
            .build();

    private final List<T> items;
    private final List<Integer> pagedSlots;
    private final int previousPageSlot;
    private final int nextPageSlot;
    private int page = 0;

    public ListInventory(@NotNull Rows rows, @NotNull Component inventoryTitle, @NotNull Collection<T> items) {
        this(rows, inventoryTitle, items, rows.getAllSlots(), null);
    }

    public ListInventory(@NotNull Rows rows, @NotNull Component inventoryTitle, @NotNull Collection<T> items, @Nullable Player viewer) {
        this(rows, inventoryTitle, items, rows.getAllSlots(), viewer);
    }

    public ListInventory(@NotNull Rows rows, @NotNull Component inventoryTitle, @NotNull Collection<T> items, @NotNull LinkedHashSet<Integer> pagedSlots, @Nullable Player viewer) {
        super(rows, inventoryTitle, viewer);
        this.items = new ArrayList<>(items); // turn to regular list to create an ordered collection
        this.pagedSlots = new ArrayList<>(pagedSlots); // turn to regular list to create an ordered collection
        this.previousPageSlot = getInventory().getSize() - 6;
        this.nextPageSlot = getInventory().getSize() - 4;

        updateInventory(); // display on page 0
    }

    protected abstract @NotNull ItemStack mapToItem(@NotNull T object);

    protected abstract @Nullable Consumer<InventoryClickEvent> mapClick(@NotNull T object);

    public void updateInventory() {
        clearPagedSlots();

        int itemsPerPage = pagedSlots.size();
        int startIndex = page * itemsPerPage;

        for (int i = 0; i < pagedSlots.size(); i++) {
            int itemIndex = startIndex + i;
            if (itemIndex >= items.size()) break;

            T itemObject = items.get(itemIndex);
            ItemStack stack = mapToItem(itemObject);
            Consumer<InventoryClickEvent> click = mapClick(itemObject);

            setSlot(pagedSlots.get(i), stack, click);
        }

        updatePageButtons();
    }

    private void clearPagedSlots() {
        for (Integer slot : pagedSlots) {
            setSlot(slot, null, null);
        }
    }

    private void updatePageButtons() {
        if (page > 0)
            setSlot(previousPageSlot, PREVIOUS_PAGE_ITEM, event -> previousPage());
        else
            setSlot(previousPageSlot, PLACEHOLDER, null);

        int itemsPerPage = pagedSlots.size();
        boolean hasNextPage = (page + 1) * itemsPerPage < items.size();

        if (hasNextPage)
            setSlot(nextPageSlot, NEXT_PAGE_ITEM, event -> nextPage());
        else
            setSlot(nextPageSlot, PLACEHOLDER, null);
    }

    public void nextPage() {
        int itemsPerPage = pagedSlots.size();
        if ((page + 1) * itemsPerPage < items.size()) {
            page++;
            updateInventory();
        }
    }

    public void previousPage() {
        if (page > 0) {
            page--;
            updateInventory();
        }
    }
}
