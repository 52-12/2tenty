package threeadd.glimpsy.feature.help.list;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.feature.help.HelpListInventory;
import threeadd.glimpsy.feature.help.article.Article;
import threeadd.glimpsy.feature.help.article.ArticleBooks;
import threeadd.glimpsy.feature.help.article.ArticleOrderManager;
import threeadd.glimpsy.feature.help.article.ArticleRegistry;
import threeadd.glimpsy.feature.help.article.PlayerArticle;
import threeadd.glimpsy.feature.help.article.PlayerArticleManager;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.inventory.ItemBuilder;
import threeadd.glimpsy.util.inventory.ListInventory;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The /help articles list. Shows both the built-in (file-based) {@link Article}s and approved
 * player-submitted {@link PlayerArticle}s - the latter open as a written book when clicked.
 * Admins ({@link PlayerArticleManager#ADMIN_PERMISSION}) can shift-click an entry to move it up
 * or down the (persisted) display order.
 */
public class ArticleListInventory extends ListInventory<Object> {

    public ArticleListInventory(Player viewer) {
        super(Rows.SIX, Component.text("Community Articles"), combined(), Rows.SIX.getInnerSlots(), viewer);

        setTransferSlot(Rows.SIX.getTotalSlots() - 9,
                new ItemBuilder(Material.RED_DYE)
                        .withName(Component.text("Return").color(TextColor.color(0xDB5858)))
                        .build(),
                () -> new HelpListInventory(getViewer()));
    }

    private static List<Object> combined() {
        List<Object> all = new ArrayList<>(ArticleRegistry.INSTANCE.getRegisteredItems());
        all.addAll(PlayerArticleManager.approved());
        return ArticleOrderManager.applyOrder(all, ArticleListInventory::keyOf);
    }

    /** Stable identity for the order file - the DB id for player articles, the title for built-in ones. */
    private static String keyOf(Object entry) {
        if (entry instanceof PlayerArticle article) return "player:" + article.getId();

        Article article = (Article) entry;
        return "builtin:" + PlainTextComponentSerializer.plainText().serialize(article.title());
    }

    private boolean canReorder() {
        return getViewer().hasPermission(PlayerArticleManager.ADMIN_PERMISSION);
    }

    @Override
    protected @NotNull ItemStack mapToItem(@NotNull Object entry) {
        if (entry instanceof PlayerArticle article) {
            List<Component> lore = new ArrayList<>(List.of(
                    Component.text("By " + article.getAuthorName(), NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("Click to read", NamedTextColor.GRAY)));
            if (canReorder()) lore.add(Messages.msg("articles.reorder-hint",
                    "<green>Shift-Left<muted> move up  <green>Shift-Right<muted> move down"));

            return new ItemBuilder(Material.WRITTEN_BOOK)
                    .withName(Component.text(article.getTitle()))
                    .withLore(lore)
                    .build();
        }

        Article article = (Article) entry;
        List<Component> lore = new ArrayList<>(List.of(
                ComponentParser.parseRichString("<#D75A5A>Authors:</#D75A5A> " + String.join("<#C4C4C4>,</#C4C4C4> ", article.authors())),
                ComponentParser.parseRichString("<#D75A5A>Creation Date:</#D75A5A> " + article.creationDate()),
                Component.empty(),
                article.content()));
        if (canReorder()) lore.add(Messages.msg("articles.reorder-hint",
                "<green>Shift-Left<muted> move up  <green>Shift-Right<muted> move down"));

        return new ItemBuilder(Material.PAPER)
                .withName(article.title().color(TextColor.color(0xFF3B3B)))
                .withLore(lore)
                .build();
    }

    @Override
    protected @Nullable Consumer<InventoryClickEvent> mapClick(@NotNull Object entry) {
        return event -> {
            if (canReorder() && (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT)) {
                String key = keyOf(entry);
                if (event.getClick() == ClickType.SHIFT_LEFT) ArticleOrderManager.moveUp(key);
                else ArticleOrderManager.moveDown(key);
                reopen();
                return;
            }

            if (entry instanceof PlayerArticle article) {
                ArticleBooks.open((Player) event.getWhoClicked(), article);
                return;
            }

            Article article = (Article) entry;
            event.getView().getPlayer().sendMessage(article.content());
        };
    }

    private void reopen() {
        int page = getPage();
        ArticleListInventory next = new ArticleListInventory(getViewer());
        next.setPage(page);
        getViewer().openInventory(next.getInventory());
    }
}
