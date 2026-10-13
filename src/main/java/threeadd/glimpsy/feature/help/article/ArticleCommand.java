package threeadd.glimpsy.feature.help.article;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import threeadd.glimpsy.feature.help.list.ArticleRequestInventory;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.List;

@SuppressWarnings({"UnstableApiUsage", "SameReturnValue"})
public class ArticleCommand extends CustomCommand {

    private static final String ADMIN_PERMISSION = PlayerArticleManager.ADMIN_PERMISSION;

    private static int request(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null || disabled(player)) return SUCCESS;

        String content = ArticleBooks.contentFromBook(player.getInventory().getItemInMainHand());
        if (content == null || content.isBlank()) {
            player.sendMessage(Messages.msg("articles.need-book",
                    "<error>Write your article in a book & quill and hold it, then run this again"));
            return SUCCESS;
        }

        String title = StringArgumentType.getString(ctx, "title");
        if (PlayerArticleManager.submit(player, title, content) == null) {
            player.sendMessage(Messages.msg("articles.already-pending",
                    "<error>You already have a request awaiting review - /article cancel to withdraw it"));
            return SUCCESS;
        }

        player.sendMessage(Messages.msg("articles.submitted",
                "<success>Article \"{title}\" submitted for review", "title", title));
        PlayerArticleManager.notifyReviewers(player);
        ArticleBooks.consumeHeld(player);
        return SUCCESS;
    }

    /** True (and tells them why) when the whole feature is switched off in config.yml. */
    private static boolean disabled(Player player) {
        if (PlayerArticleManager.enabled()) return false;

        player.sendMessage(Messages.msg("articles.disabled", "<error>Player articles are disabled"));
        return true;
    }

    private static int edit(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null || disabled(player)) return SUCCESS;

        PlayerArticle article = ownArticle(player, StringArgumentType.getString(ctx, "title"));
        if (article == null) {
            player.sendMessage(Messages.msg("articles.not-yours", "<error>You have no article with that title"));
            return SUCCESS;
        }

        PlayerArticle otherPending = PlayerArticleManager.pendingOf(player.getUniqueId());
        if (otherPending != null && otherPending.getId() != article.getId()) {
            player.sendMessage(Messages.msg("articles.already-pending",
                    "<error>You already have a request awaiting review - /article cancel to withdraw it"));
            return SUCCESS;
        }

        // Writing and saving the book submits the edit automatically - see ArticleEditListener
        for (ItemStack leftover : player.getInventory().addItem(ArticleBooks.writableCopy(article)).values())
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        player.sendMessage(Messages.msg("articles.edit-book-given",
                "<primary>Write your changes in the book you were just given - saving it submits the edit automatically"));
        return SUCCESS;
    }

    private static int delete(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null || disabled(player)) return SUCCESS;

        PlayerArticle article = ownArticle(player, StringArgumentType.getString(ctx, "title"));
        if (article == null) {
            player.sendMessage(Messages.msg("articles.not-yours", "<error>You have no article with that title"));
            return SUCCESS;
        }

        PlayerArticleManager.delete(article);
        player.sendMessage(Messages.msg("articles.deleted", "<success>Article deleted"));
        return SUCCESS;
    }

    private static int cancel(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null || disabled(player)) return SUCCESS;

        PlayerArticle pending = PlayerArticleManager.pendingOf(player.getUniqueId());
        if (pending == null) {
            player.sendMessage(Messages.msg("articles.no-pending", "<error>You have no pending request"));
            return SUCCESS;
        }

        PlayerArticleManager.delete(pending);
        player.sendMessage(Messages.msg("articles.request-cancelled", "<success>Your pending request was withdrawn"));
        return SUCCESS;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null || disabled(player)) return SUCCESS;

        List<PlayerArticle> own = PlayerArticleManager.byAuthor(player.getUniqueId());
        if (own.isEmpty()) {
            player.sendMessage(Messages.msg("articles.none", "<muted>You haven't written any articles yet"));
            return SUCCESS;
        }

        player.sendMessage(Messages.msg("articles.list-header", "<primary>Your articles:"));
        for (PlayerArticle article : own) {
            Component status = article.isPending()
                    ? Component.text("(pending)", NamedTextColor.YELLOW)
                    : Component.text("(published)", NamedTextColor.GREEN);
            player.sendMessage(Component.textOfChildren(
                    Component.text(article.getTitle(), NamedTextColor.WHITE),
                    Component.space(), status));
        }
        return SUCCESS;
    }

    private static int requests(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null || disabled(player)) return SUCCESS;

        player.openInventory(new ArticleRequestInventory(player).getInventory());
        return SUCCESS;
    }

    private static PlayerArticle ownArticle(Player player, String title) {
        return PlayerArticleManager.byAuthorAndTitle(player.getUniqueId(), title);
    }

    @Override
    public Component createUsage() {
        return ComponentParser.parseRichString("Submit and manage your /help articles: request, list, edit, delete, cancel");
    }

    // Switched off in config.yml - drop it out of /help too rather than listing a command that
    // only ever answers "that's disabled"
    @Override
    public boolean isVisibleTo(Player viewer) {
        return PlayerArticleManager.enabled() && super.isVisibleTo(viewer);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("article")
                .then(Commands.literal("request")
                        .then(Commands.argument("title", StringArgumentType.greedyString())
                                .executes(ArticleCommand::request)))
                .then(Commands.literal("list").executes(ArticleCommand::list))
                .then(Commands.literal("edit")
                        .then(Commands.argument("title", StringArgumentType.greedyString())
                                .executes(ArticleCommand::edit)))
                .then(Commands.literal("delete")
                        .then(Commands.argument("title", StringArgumentType.greedyString())
                                .executes(ArticleCommand::delete)))
                .then(Commands.literal("cancel").executes(ArticleCommand::cancel))
                .then(Commands.literal("requests")
                        .requires(source -> source.getSender().hasPermission(ADMIN_PERMISSION))
                        .executes(ArticleCommand::requests));
    }

    @Override
    public List<String> getAliases() {
        return List.of("articles");
    }
}
