package threeadd.glimpsy.feature.help.article;

import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.persistence.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerArticleRepository extends Repository<PlayerArticle> {

    private static final PlayerArticleRepository instance = new PlayerArticleRepository();

    public static PlayerArticleRepository getInstance() {
        return instance;
    }

    @Override
    protected void createTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS player_articles (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "author_id VARCHAR(36) NOT NULL," +
                    "author_name VARCHAR(64) NOT NULL," +
                    "title TEXT NOT NULL," +
                    "content TEXT NOT NULL," +
                    "pending INTEGER NOT NULL," +
                    "created_at BIGINT NOT NULL)");
        }
    }

    public List<PlayerArticle> loadAllSync() {
        List<PlayerArticle> articles = new ArrayList<>();

        try (Connection conn = Glimpsy.getDataBase().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM player_articles")) {
            while (rs.next()) {
                articles.add(new PlayerArticle(
                        rs.getLong("id"),
                        UUID.fromString(rs.getString("author_id")),
                        rs.getString("author_name"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getInt("pending") != 0,
                        rs.getLong("created_at")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load player articles", e);
        }

        return articles;
    }

    /** Inserts a new article and returns its generated id. */
    public long insertSync(PlayerArticle article) {
        try (Connection conn = Glimpsy.getDataBase().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO player_articles (author_id, author_name, title, content, pending, created_at) " +
                             "VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, article.getAuthor().toString());
            ps.setString(2, article.getAuthorName());
            ps.setString(3, article.getTitle());
            ps.setString(4, article.getContent());
            ps.setInt(5, article.isPending() ? 1 : 0);
            ps.setLong(6, article.getCreatedAt());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert player article", e);
        }
        return -1;
    }

    public void runAsyncUpdate(PlayerArticle article) {
        runAsync(() -> executeUpdate("UPDATE player_articles SET title = ?, content = ?, pending = ? WHERE id = ?",
                article.getTitle(), article.getContent(), article.isPending() ? 1 : 0, article.getId()));
    }

    public void runAsyncDelete(long id) {
        runAsync(() -> executeUpdate("DELETE FROM player_articles WHERE id = ?", id));
    }
}
