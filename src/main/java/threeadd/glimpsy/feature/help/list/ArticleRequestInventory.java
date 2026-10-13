package threeadd.glimpsy.feature.help.list;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.feature.help.article.ArticleBooks;
import threeadd.glimpsy.feature.help.article.PlayerArticle;
import threeadd.glimpsy.feature.help.article.PlayerArticleManager;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.inventory.ItemBuilder;
import threeadd.glimpsy.util.inventory.ListInventory;

import java.util.function.Consumer;

/** Admin review menu of pending article requests (opened by /article requests). */
public class ArticleRequestInventory extends ListInventory<PlayerArticle> {

    public ArticleRequestInventory(Player viewer) {
        super(Rows.SIX, Component.text("Article Requests"), PlayerArticleManager.pending(), Rows.SIX.getInnerSlots(), viewer);
    }

    @Override
    protected @NotNull ItemStack mapToItem(@NotNull PlayerArticle article) {
        return new ItemBuilder(Material.WRITABLE_BOOK)
                .withName(Component.text(article.getTitle()))
                .withLore(
                        Component.text("By " + article.getAuthorName(), NamedTextColor.GRAY),
                        Component.empty(),
                        Messages.msg("articles.review-hint",
                                "<green>Left-click<muted> read  <green>Shift-Left<muted> approve  <red>Right<muted> decline"))
                .build();
    }

    @Override
    protected @Nullable Consumer<InventoryClickEvent> mapClick(@NotNull PlayerArticle article) {
        return event -> {
            Player admin = getViewer();
            switch (event.getClick()) {
                case SHIFT_LEFT, SHIFT_RIGHT -> {
                    PlayerArticleManager.approve(article);
                    admin.sendMessage(Messages.msg("articles.approved", "<success>Approved \"{title}\" by {author}",
                            "title", article.getTitle(), "author", article.getAuthorName()));
                    reopen();
                }
                case RIGHT -> {
                    PlayerArticleManager.decline(article);
                    admin.sendMessage(Messages.msg("articles.declined", "<error>Declined \"{title}\" by {author}",
                            "title", article.getTitle(), "author", article.getAuthorName()));
                    reopen();
                }
                default -> ArticleBooks.open(admin, article); // left-click reads it
            }
        };
    }

    private void reopen() {
        getViewer().openInventory(new ArticleRequestInventory(getViewer()).getInventory());
    }
}
