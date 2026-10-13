package threeadd.glimpsy.feature.help.article;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;
import threeadd.glimpsy.util.config.Messages;

/**
 * Picks up /article edit's book &amp; quill the moment the player finishes writing and saves it -
 * no need to hold it and re-run the command, and no stray already-written book left behind for an
 * admin to clean up.
 */
public class ArticleEditListener implements Listener {

    @EventHandler
    public void onEditBook(PlayerEditBookEvent event) {
        BookMeta previous = event.getPreviousBookMeta();
        if (!previous.getPersistentDataContainer().has(ArticleBooks.EDIT_ARTICLE_KEY, PersistentDataType.LONG))
            return;

        long articleId = previous.getPersistentDataContainer().get(ArticleBooks.EDIT_ARTICLE_KEY, PersistentDataType.LONG);
        Player player = event.getPlayer();

        PlayerArticle article = PlayerArticleManager.byId(articleId);
        if (article == null || !article.getAuthor().equals(player.getUniqueId())) return;

        String content = ArticleBooks.contentFromMeta(event.getNewBookMeta());
        if (content.isBlank()) return;

        PlayerArticle otherPending = PlayerArticleManager.pendingOf(player.getUniqueId());
        if (otherPending != null && otherPending.getId() != article.getId()) {
            player.sendMessage(Messages.msg("articles.already-pending",
                    "<error>You already have a request awaiting review - /article cancel to withdraw it"));
            return;
        }

        PlayerArticleManager.editContent(article, content);
        player.sendMessage(Messages.msg("articles.edited",
                "<success>Article updated and sent back for review"));

        ArticleBooks.consumeMatching(player, articleId);
    }
}
