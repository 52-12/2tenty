package threeadd.glimpsy.feature.timezone;

import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.persistence.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TimezoneRepository extends Repository<Void> {

    private static final TimezoneRepository instance = new TimezoneRepository();

    public static TimezoneRepository getInstance() {
        return instance;
    }

    @Override
    protected void createTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS player_timezones (" +
                    "player_id VARCHAR(36) PRIMARY KEY," +
                    "zone VARCHAR(64) NOT NULL)");
        }
    }

    public Map<UUID, ZoneId> loadAllSync() {
        Map<UUID, ZoneId> zones = new HashMap<>();

        try (Connection conn = Glimpsy.getDataBase().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM player_timezones")) {
            while (rs.next()) {
                try {
                    zones.put(UUID.fromString(rs.getString("player_id")), ZoneId.of(rs.getString("zone")));
                } catch (Exception ignored) {
                    // A zone id that's no longer valid on this JVM - skip it, falls back to default
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load player timezones", e);
        }

        return zones;
    }

    public void runAsyncSave(UUID playerId, ZoneId zone) {
        runAsync(() -> executeUpdate("REPLACE INTO player_timezones (player_id, zone) VALUES (?, ?)",
                playerId.toString(), zone.getId()));
    }

    public void runAsyncDelete(UUID playerId) {
        runAsync(() -> executeUpdate("DELETE FROM player_timezones WHERE player_id = ?", playerId.toString()));
    }
}
