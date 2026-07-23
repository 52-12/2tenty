package threeadd.glimpsy.feature.discord;

import threeadd.glimpsy.util.persistence.Repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DiscordMessagesRepository extends Repository<Boolean> {

    private static final DiscordMessagesRepository instance = new DiscordMessagesRepository();

    public static DiscordMessagesRepository getInstance() {
        return instance;
    }

    @Override
    protected void createTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Presence of a row means the player has disabled Discord announcements
            stmt.execute("CREATE TABLE IF NOT EXISTS discord_messages_disabled (" +
                    "uuid VARCHAR(36) PRIMARY KEY" +
                    ")");
        }
    }

    public boolean isDisabledSync(UUID uuid) {
        return querySingle("SELECT 1 FROM discord_messages_disabled WHERE uuid = ?",
                rs -> true, uuid.toString()).orElse(false);
    }

    public void setDisabledSync(UUID uuid, boolean disabled) {
        if (disabled)
            executeUpdate("REPLACE INTO discord_messages_disabled (uuid) VALUES (?)", uuid.toString());
        else
            executeUpdate("DELETE FROM discord_messages_disabled WHERE uuid = ?", uuid.toString());
    }

    public CompletableFuture<Boolean> isDisabledAsync(UUID uuid) {
        return supplyAsync(() -> isDisabledSync(uuid));
    }

    public CompletableFuture<Void> setDisabledAsync(UUID uuid, boolean disabled) {
        return runAsync(() -> setDisabledSync(uuid, disabled));
    }
}
