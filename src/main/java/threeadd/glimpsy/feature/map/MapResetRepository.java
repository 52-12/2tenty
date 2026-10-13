package threeadd.glimpsy.feature.map;

import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.persistence.Repository;
import threeadd.glimpsy.util.persistence.RowMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

public class MapResetRepository extends Repository<MapResetTime> {

    private final static MapResetRepository instance = new MapResetRepository();
    private final RowMapper<MapResetTime> MAPPER = rs -> new MapResetTime(
            rs.getString("schedule_key"),
            rs.getTimestamp("timestamp").toLocalDateTime()
    );

    public static MapResetRepository getInstance() {
        return instance;
    }

    @Override
    protected void createTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS scheduled_tasks (" +
                    "schedule_key VARCHAR(32) PRIMARY KEY," +
                    "timestamp DATETIME NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS map_reset_state (" +
                    "id INTEGER PRIMARY KEY," +
                    "paused INTEGER NOT NULL," +
                    "remaining_seconds BIGINT NOT NULL)");
        }
    }

    // Whether the countdown is currently paused, and (if so) how many seconds were left when it
    // was paused - a single row keyed on id 0.
    public record ResetPauseState(boolean paused, long remainingSeconds) {
    }

    public void saveResetPausedSync(boolean paused, long remainingSeconds) {
        executeUpdate("REPLACE INTO map_reset_state (id, paused, remaining_seconds) VALUES (0, ?, ?)",
                paused ? 1 : 0, remainingSeconds);
    }

    public void saveAsync(MapResetTime data) {
        runAsync(() -> saveSync(data));
    }

    public void saveResetPausedAsync(boolean paused, long remainingSeconds) {
        runAsync(() -> saveResetPausedSync(paused, remainingSeconds));
    }

    public void saveLastResetAsync(LocalDateTime time) {
        runAsync(() -> saveLastResetSync(time));
    }

    public void saveFullResetAsync(LocalDateTime time) {
        runAsync(() -> saveFullResetSync(time));
    }

    public Optional<ResetPauseState> loadResetPausedSync() {
        try (Connection conn = Glimpsy.getDataBase().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM map_reset_state WHERE id = 0");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next())
                return Optional.of(new ResetPauseState(rs.getInt("paused") != 0, rs.getLong("remaining_seconds")));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load map reset pause state", e);
        }

        return Optional.empty();
    }

    public void saveSync(MapResetTime data) {
        String sql = "REPLACE INTO scheduled_tasks (schedule_key, timestamp) VALUES (?, ?)";
        executeUpdate(sql, MapResetTime.key, Timestamp.valueOf(data.getTime()));
    }

    public Optional<MapResetTime> loadSync() {
        return querySingle("SELECT * FROM scheduled_tasks WHERE schedule_key = ?", MAPPER, MapResetTime.key);
    }

    // Reuses the same generic (key, timestamp) table under a different key - this is when the
    // map was actually last reset, as opposed to MapResetTime's "when is the next one scheduled".
    private static final String LAST_RESET_KEY = "last-map-reset";

    public void saveLastResetSync(LocalDateTime time) {
        executeUpdate("REPLACE INTO scheduled_tasks (schedule_key, timestamp) VALUES (?, ?)",
                LAST_RESET_KEY, Timestamp.valueOf(time));
    }

    // Can't use querySingle here - it's bound to Repository<MapResetTime>'s own type, and this
    // returns a plain LocalDateTime instead.
    public Optional<LocalDateTime> loadLastResetSync() {
        return loadTimestamp(LAST_RESET_KEY);
    }

    private static final String LAST_FULL_RESET_KEY = "last-full-reset";

    public void saveFullResetSync(LocalDateTime time) {
        executeUpdate("REPLACE INTO scheduled_tasks (schedule_key, timestamp) VALUES (?, ?)",
                LAST_FULL_RESET_KEY, Timestamp.valueOf(time));
    }

    public Optional<LocalDateTime> loadFullResetSync() {
        return loadTimestamp(LAST_FULL_RESET_KEY);
    }

    private Optional<LocalDateTime> loadTimestamp(String scheduleKey) {
        try (Connection conn = Glimpsy.getDataBase().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM scheduled_tasks WHERE schedule_key = ?")) {
            ps.setString(1, scheduleKey);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(rs.getTimestamp("timestamp").toLocalDateTime());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load timestamp for " + scheduleKey, e);
        }

        return Optional.empty();
    }
}

