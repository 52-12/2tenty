package threeadd.glimpsy.feature.competition;

import threeadd.glimpsy.util.persistence.Repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CompetitionMessagesRepository extends Repository<Boolean> {

    private static final CompetitionMessagesRepository instance = new CompetitionMessagesRepository();

    public static CompetitionMessagesRepository getInstance() {
        return instance;
    }

    @Override
    protected void createTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Presence of a row means the player has disabled competition messages
            stmt.execute("CREATE TABLE IF NOT EXISTS competition_messages_disabled (" +
                    "uuid VARCHAR(36) PRIMARY KEY" +
                    ")");
        }
    }

    public boolean isDisabledSync(UUID uuid) {
        return querySingle("SELECT 1 FROM competition_messages_disabled WHERE uuid = ?",
                rs -> true, uuid.toString()).orElse(false);
    }

    public void setDisabledSync(UUID uuid, boolean disabled) {
        if (disabled)
            executeUpdate("REPLACE INTO competition_messages_disabled (uuid) VALUES (?)", uuid.toString());
        else
            executeUpdate("DELETE FROM competition_messages_disabled WHERE uuid = ?", uuid.toString());
    }

    public CompletableFuture<Boolean> isDisabledAsync(UUID uuid) {
        return supplyAsync(() -> isDisabledSync(uuid));
    }

    public CompletableFuture<Void> setDisabledAsync(UUID uuid, boolean disabled) {
        return runAsync(() -> setDisabledSync(uuid, disabled));
    }
}
