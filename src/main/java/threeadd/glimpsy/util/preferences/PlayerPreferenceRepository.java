package threeadd.glimpsy.util.preferences;

import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.persistence.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Generic per-player boolean preferences (scoreboard shown, alerts, chat modes, sound opt-outs,
 * ...), stored as one row per (player, key). Keeps every small "remember my toggle" out of its own
 * bespoke table - see issue #3.
 */
public class PlayerPreferenceRepository extends Repository<Void> {

    private static final PlayerPreferenceRepository instance = new PlayerPreferenceRepository();

    public static PlayerPreferenceRepository getInstance() {
        return instance;
    }

    @Override
    protected void createTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS player_preferences (" +
                    "uuid VARCHAR(36) NOT NULL," +
                    "pref_key VARCHAR(64) NOT NULL," +
                    "enabled INTEGER NOT NULL," +
                    "PRIMARY KEY (uuid, pref_key)" +
                    ")");
        }
    }

    public Map<String, Boolean> loadSync(UUID uuid) {
        Map<String, Boolean> values = new HashMap<>();

        try (Connection conn = Glimpsy.getDataBase().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT pref_key, enabled FROM player_preferences WHERE uuid = ?")) {

            ps.setString(1, uuid.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    values.put(rs.getString("pref_key"), rs.getInt("enabled") != 0);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load preferences for " + uuid, e);
        }

        return values;
    }

    public CompletableFuture<Void> setAsync(UUID uuid, String key, boolean value) {
        return runAsync(() -> executeUpdate(
                "REPLACE INTO player_preferences (uuid, pref_key, enabled) VALUES (?, ?, ?)",
                uuid.toString(), key, value ? 1 : 0));
    }
}
