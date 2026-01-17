package threeadd.glimpsy.feature.map;

import threeadd.glimpsy.util.persistence.Repository;
import threeadd.glimpsy.util.persistence.RowMapper;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
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
        }
    }

    public void saveSync(MapResetTime data) {
        String sql = "REPLACE INTO scheduled_tasks (schedule_key, timestamp) VALUES (?, ?)";
        executeUpdate(sql, MapResetTime.key, Timestamp.valueOf(data.getTime()));
    }

    public Optional<MapResetTime> loadSync() {
        return querySingle("SELECT * FROM scheduled_tasks WHERE schedule_key = ?", MAPPER, MapResetTime.key);
    }
}

