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

    /**
     * Stored in the (NOT NULL) zone column to mean "no zone of their own, follow the server" -
     * a player can turn seconds off or pick a date format without ever choosing a zone.
     */
    private static final String SERVER_DEFAULT = "-";

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

        // CREATE TABLE IF NOT EXISTS is a no-op on a database from before these columns existed -
        // add them separately, ignoring the failure if they're already there. The defaults match
        // the pre-existing behaviour, so rows written by the old zone-only version stay correct.
        addColumnIfMissing(conn, "daylight_savings INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing(conn, "show_seconds INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing(conn, "time_format VARCHAR(8) NOT NULL DEFAULT 'AUTO'");
    }

    private void addColumnIfMissing(Connection conn, String definition) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE player_timezones ADD COLUMN " + definition);
        } catch (SQLException ignored) {
            // Column already exists.
        }
    }

    public Map<UUID, TimezonePreferences> loadAllSync() {
        Map<UUID, TimezonePreferences> loaded = new HashMap<>();

        try (Connection conn = Glimpsy.getDataBase().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM player_timezones")) {
            while (rs.next()) {
                TimezonePreferences prefs = new TimezonePreferences();
                prefs.setZone(parseZone(rs.getString("zone")));
                prefs.setDaylightSavings(rs.getInt("daylight_savings") != 0);
                prefs.setShowSeconds(rs.getInt("show_seconds") != 0);
                prefs.setFormat(TimezonePreferences.Format.parse(rs.getString("time_format"),
                        TimezonePreferences.Format.AUTO));

                loaded.put(UUID.fromString(rs.getString("player_id")), prefs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load player timezones", e);
        }

        return loaded;
    }

    private static ZoneId parseZone(String raw) {
        if (raw == null || raw.equals(SERVER_DEFAULT)) return null;

        try {
            return ZoneId.of(raw);
        } catch (Exception e) {
            return null; // a zone id that's no longer valid on this JVM - fall back to the server's
        }
    }

    public void runAsyncSave(UUID playerId, TimezonePreferences prefs) {
        String zone = prefs.getZone() == null ? SERVER_DEFAULT : prefs.getZone().getId();
        boolean dst = prefs.isDaylightSavings();
        boolean seconds = prefs.isShowSeconds();
        String format = prefs.getFormat().name();

        runAsync(() -> executeUpdate(
                "REPLACE INTO player_timezones (player_id, zone, daylight_savings, show_seconds, time_format) " +
                        "VALUES (?, ?, ?, ?, ?)",
                playerId.toString(), zone, dst ? 1 : 0, seconds ? 1 : 0, format));
    }

    public void runAsyncDelete(UUID playerId) {
        runAsync(() -> executeUpdate("DELETE FROM player_timezones WHERE player_id = ?", playerId.toString()));
    }
}
