package threeadd.glimpsy.feature.discordsrv;

import threeadd.glimpsy.util.persistence.Repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DiscordChatMuteRepository extends Repository<Boolean> {

    private static final DiscordChatMuteRepository instance = new DiscordChatMuteRepository();

    public static DiscordChatMuteRepository getInstance() {
        return instance;
    }

    @Override
    protected void createTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Presence of a row means the player has muted Discord chat relay messages
            stmt.execute("CREATE TABLE IF NOT EXISTS discord_chat_muted (" +
                    "uuid VARCHAR(36) PRIMARY KEY" +
                    ")");
        }
    }

    public boolean isMutedSync(UUID uuid) {
        return querySingle("SELECT 1 FROM discord_chat_muted WHERE uuid = ?",
                rs -> true, uuid.toString()).orElse(false);
    }

    public void setMutedSync(UUID uuid, boolean muted) {
        if (muted)
            executeUpdate("REPLACE INTO discord_chat_muted (uuid) VALUES (?)", uuid.toString());
        else
            executeUpdate("DELETE FROM discord_chat_muted WHERE uuid = ?", uuid.toString());
    }

    public CompletableFuture<Boolean> isMutedAsync(UUID uuid) {
        return supplyAsync(() -> isMutedSync(uuid));
    }

    public CompletableFuture<Void> setMutedAsync(UUID uuid, boolean muted) {
        return runAsync(() -> setMutedSync(uuid, muted));
    }
}
