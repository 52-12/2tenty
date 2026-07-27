package threeadd.glimpsy.feature.stats;

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

public class StatsRepository extends Repository<PlayerStats> {

    private static final StatsRepository instance = new StatsRepository();

    public static StatsRepository getInstance() {
        return instance;
    }

    @Override
    protected void createTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS player_stats (" +
                    "player_id VARCHAR(36) PRIMARY KEY," +
                    "fish_caught INTEGER NOT NULL DEFAULT 0," +
                    "kills INTEGER NOT NULL DEFAULT 0," +
                    "deaths INTEGER NOT NULL DEFAULT 0," +
                    "playtime_millis BIGINT NOT NULL DEFAULT 0)");
        }
    }

    public Map<UUID, PlayerStats> loadAllSync() {
        Map<UUID, PlayerStats> result = new HashMap<>();

        try (Connection conn = Glimpsy.getDataBase().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_stats");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("player_id"));
                result.put(id, new PlayerStats(
                        id,
                        rs.getInt("fish_caught"),
                        rs.getInt("kills"),
                        rs.getInt("deaths"),
                        rs.getLong("playtime_millis")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load player stats", e);
        }

        return result;
    }

    public void saveSync(PlayerStats stats) {
        executeUpdate("REPLACE INTO player_stats (player_id, fish_caught, kills, deaths, playtime_millis) " +
                        "VALUES (?, ?, ?, ?, ?)",
                stats.getPlayerId().toString(),
                stats.getFishCaught(),
                stats.getKills(),
                stats.getDeaths(),
                stats.getPlaytimeMillis());
    }

    public void runAsyncSave(PlayerStats stats) {
        runAsync(() -> saveSync(stats));
    }
}
