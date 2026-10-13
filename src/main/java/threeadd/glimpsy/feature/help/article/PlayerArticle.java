package threeadd.glimpsy.feature.help.article;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * A player-submitted help article (written in a book &amp; quill). {@code pending} true means it's
 * awaiting admin review and not shown in /help yet; false means approved and visible. Content is
 * the book's pages joined by a form-feed, so it round-trips to and from a book cleanly.
 */
public class PlayerArticle {

    public static final String PAGE_SEPARATOR = ""; // form feed

    private long id;
    private final UUID author;
    private final String authorName;
    private String title;
    private String content;
    private boolean pending;
    private final long createdAt;

    public PlayerArticle(long id, UUID author, String authorName, String title, String content,
                         boolean pending, long createdAt) {
        this.id = id;
        this.author = author;
        this.authorName = authorName;
        this.title = title;
        this.content = content;
        this.pending = pending;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UUID getAuthor() {
        return author;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isPending() {
        return pending;
    }

    public void setPending(boolean pending) {
        this.pending = pending;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    /** The content split back into book pages. */
    public List<String> pages() {
        if (content == null || content.isEmpty()) return List.of("");
        return Arrays.asList(content.split(PAGE_SEPARATOR, -1));
    }
}
