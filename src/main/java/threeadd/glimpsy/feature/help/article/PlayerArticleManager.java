package threeadd.glimpsy.feature.help.article;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.feature.achievement.Achievement;
import threeadd.glimpsy.feature.achievement.AchievementManager;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.manager.Manager;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Holds all player-submitted articles (pending + approved) in memory, backed by
 * {@link PlayerArticleRepository}. One pending request per player is enforced here.
 */
public class PlayerArticleManager extends Manager {

    private static final List<PlayerArticle> articles = new CopyOnWriteArrayList<>();

    @Override
    public void enable() {
        articles.addAll(PlayerArticleRepository.getInstance().loadAllSync());
    }

    public static boolean hasPending(UUID author) {
        return articles.stream().anyMatch(a -> a.getAuthor().equals(author) && a.isPending());
    }

    /** Creates a new pending article (blocks if the author already has one pending). */
    public static @Nullable PlayerArticle submit(Player player, String title, String content) {
        if (hasPending(player.getUniqueId())) return null;

        PlayerArticle article = new PlayerArticle(-1, player.getUniqueId(), player.getName(),
                title, content, true, System.currentTimeMillis());
        article.setId(PlayerArticleRepository.getInstance().insertSync(article));
        articles.add(article);
        return article;
    }

    /** Admin permission that gates /article requests - also who gets pinged about new ones. */
    public static final String ADMIN_PERMISSION = "2tenty.command.article.admin";

    /**
     * Whether players can write articles at all. Off hides /article and stops approved articles
     * showing in /help - nothing is deleted, so flipping it back brings everything with it.
     */
    public static boolean enabled() {
        return ConfigManager.getBoolean("articles.enabled", false);
    }

    /** Tells every online reviewer that this player has something waiting. */
    public static void notifyReviewers(Player author) {
        Component notice = Messages.msg("articles.new-request",
                "<primary>{author} submitted an article for review - /article requests",
                "author", author.getName());

        for (Player admin : Bukkit.getOnlinePlayers())
            if (!admin.equals(author) && admin.hasPermission(ADMIN_PERMISSION)) admin.sendMessage(notice);
    }

    public static void approve(PlayerArticle article) {
        article.setPending(false);
        PlayerArticleRepository.getInstance().runAsyncUpdate(article);

        Player author = Bukkit.getPlayer(article.getAuthor());
        if (author != null)
            author.sendMessage(Messages.msg("articles.your-approved",
                    "<success>Your article \"{title}\" was approved and is now live in /help",
                    "title", article.getTitle()));

        AchievementManager.award(article.getAuthor(), Achievement.PUBLISHED_AUTHOR);
    }

    public static void decline(PlayerArticle article) {
        articles.remove(article);
        PlayerArticleRepository.getInstance().runAsyncDelete(article.getId());

        Player author = Bukkit.getPlayer(article.getAuthor());
        if (author != null)
            author.sendMessage(Messages.msg("articles.your-declined",
                    "<error>Your article \"{title}\" was declined",
                    "title", article.getTitle()));
    }

    /** Replaces an article's content and sends it back to pending re-approval. */
    public static void editContent(PlayerArticle article, String content) {
        article.setContent(content);
        article.setPending(true);
        PlayerArticleRepository.getInstance().runAsyncUpdate(article);
    }

    public static void delete(PlayerArticle article) {
        articles.remove(article);
        PlayerArticleRepository.getInstance().runAsyncDelete(article.getId());
    }

    /** Empty while the feature is off, which is what keeps them out of the /help article list. */
    public static List<PlayerArticle> approved() {
        if (!enabled()) return List.of();
        return articles.stream().filter(a -> !a.isPending()).toList();
    }

    public static List<PlayerArticle> pending() {
        return articles.stream().filter(PlayerArticle::isPending).toList();
    }

    public static List<PlayerArticle> byAuthor(UUID author) {
        return articles.stream().filter(a -> a.getAuthor().equals(author)).toList();
    }

    public static @Nullable PlayerArticle byId(long id) {
        return articles.stream().filter(a -> a.getId() == id).findFirst().orElse(null);
    }

    /** Case-insensitive lookup among one author's own articles, since that's what they know it by. */
    public static @Nullable PlayerArticle byAuthorAndTitle(UUID author, String title) {
        return articles.stream()
                .filter(a -> a.getAuthor().equals(author) && a.getTitle().equalsIgnoreCase(title))
                .findFirst().orElse(null);
    }

    public static @Nullable PlayerArticle pendingOf(UUID author) {
        return articles.stream().filter(a -> a.getAuthor().equals(author) && a.isPending()).findFirst().orElse(null);
    }
}
