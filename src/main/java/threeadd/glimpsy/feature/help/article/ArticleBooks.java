package threeadd.glimpsy.feature.help.article;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;
import threeadd.glimpsy.Glimpsy;

import java.util.ArrayList;
import java.util.List;

/** Converts between an article's stored content and in-game books. */
public final class ArticleBooks {

    /**
     * Marks a book handed out by /article edit as belonging to a specific article (the PDC value
     * is that article's id) - lets {@link ArticleEditListener} tell an in-progress article edit
     * apart from a player just writing themselves a normal book &amp; quill.
     */
    public static final NamespacedKey EDIT_ARTICLE_KEY = new NamespacedKey("2tenty", "article-edit-id");

    private ArticleBooks() {
    }

    /** Reads a held book's pages into stored-content form, or null if the item isn't a book. */
    public static String contentFromBook(ItemStack held) {
        if (held == null || !(held.getItemMeta() instanceof BookMeta meta)) return null;
        return contentFromMeta(meta);
    }

    /** Reads a book's pages into stored-content form. */
    public static String contentFromMeta(BookMeta meta) {
        List<String> pages = new ArrayList<>();
        for (Component page : meta.pages())
            pages.add(PlainTextComponentSerializer.plainText().serialize(page));
        return String.join(PlayerArticle.PAGE_SEPARATOR, pages);
    }

    /**
     * The held book's title, or null if it isn't a book or doesn't have one - a book &amp; quill
     * never does, only a signed written book. Lets /article request skip asking for a title when
     * the book already carries one.
     */
    public static String titleFromBook(ItemStack held) {
        if (held == null || !(held.getItemMeta() instanceof BookMeta meta) || !meta.hasTitle()) return null;
        return PlainTextComponentSerializer.plainText().serialize(meta.title());
    }

    /**
     * A book &amp; quill pre-filled with the article's content, tagged so
     * {@link ArticleEditListener} can pick up the edit automatically once it's written and saved -
     * no need to hold it and re-run the command.
     */
    public static ItemStack writableCopy(PlayerArticle article) {
        ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
        book.editMeta(BookMeta.class, meta -> {
            meta.pages(toComponents(article));
            meta.getPersistentDataContainer().set(EDIT_ARTICLE_KEY, PersistentDataType.LONG, article.getId());
        });
        return book;
    }

    /** Opens the article to the player as a read-only written book. */
    public static void open(Player player, PlayerArticle article) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        book.editMeta(BookMeta.class, meta -> {
            meta.title(Component.text(article.getTitle()));
            meta.author(Component.text(article.getAuthorName()));
            meta.pages(toComponents(article));
        });
        player.openBook(book);
    }

    /**
     * Removes the book the player just used to submit an article (always their main hand, since
     * that's where {@link #contentFromBook} read it from) - deferred a tick so it doesn't race
     * whatever's still processing the command that triggered it.
     */
    public static void consumeHeld(Player player) {
        Bukkit.getScheduler().runTask(Glimpsy.getInstance(), () -> player.getInventory().setItemInMainHand(null));
    }

    /**
     * Removes the book tagged for {@code articleId} from whichever hand it's actually in - books
     * can be signed from either hand and {@link org.bukkit.event.player.PlayerEditBookEvent}'s own
     * slot accessor is deprecated for exactly that reason, so this checks both directly instead.
     * Deferred a tick so it runs after the server's own post-processing of the event (applying the
     * new page contents to the edited book) instead of racing it.
     */
    public static void consumeMatching(Player player, long articleId) {
        Bukkit.getScheduler().runTask(Glimpsy.getInstance(), () -> {
            var inventory = player.getInventory();
            if (isTaggedFor(inventory.getItemInMainHand(), articleId)) inventory.setItemInMainHand(null);
            else if (isTaggedFor(inventory.getItemInOffHand(), articleId)) inventory.setItemInOffHand(null);
        });
    }

    private static boolean isTaggedFor(ItemStack item, long articleId) {
        if (item == null || !(item.getItemMeta() instanceof BookMeta meta)) return false;
        Long value = meta.getPersistentDataContainer().get(EDIT_ARTICLE_KEY, PersistentDataType.LONG);
        return value != null && value == articleId;
    }

    private static List<Component> toComponents(PlayerArticle article) {
        List<Component> pages = new ArrayList<>();
        for (String page : article.pages()) pages.add(Component.text(page));
        return pages;
    }
}
