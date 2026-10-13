package threeadd.glimpsy.feature.achievement;

import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.persistence.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AchievementRepository extends Repository<Achievement> {

    private static final AchievementRepository instance = new AchievementRepository();

    public static AchievementRepository getInstance() {
        return instance;
    }

    @Override
    protected void createTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // One row per unlock rather than a packed column - adding an achievement then needs
            // no migration, and an achievement that's later removed just stops being read
            stmt.execute("CREATE TABLE IF NOT EXISTS player_achievements (" +
                    "player_id VARCHAR(36) NOT NULL," +
                    "achievement VARCHAR(64) NOT NULL," +
                    "earned_at BIGINT NOT NULL," +
                    "PRIMARY KEY (player_id, achievement))");
        }
    }

    public Map<UUID, Set<Achievement>> loadAllSync() {
        Map<UUID, Set<Achievement>> unlocked = new HashMap<>();

        try (Connection conn = Glimpsy.getDataBase().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT player_id, achievement FROM player_achievements")) {
            while (rs.next()) {
                Achievement achievement = parse(rs.getString("achievement"));
                if (achievement == null) continue; // an achievement that no longer exists

                unlocked.computeIfAbsent(UUID.fromString(rs.getString("player_id")),
                        id -> EnumSet.noneOf(Achievement.class)).add(achievement);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load player achievements", e);
        }

        return unlocked;
    }

    private static Achievement parse(String name) {
        try {
            return Achievement.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void runAsyncSave(UUID playerId, Achievement achievement) {
        long now = System.currentTimeMillis();
        runAsync(() -> executeUpdate(
                "REPLACE INTO player_achievements (player_id, achievement, earned_at) VALUES (?, ?, ?)",
                playerId.toString(), achievement.name(), now));
    }
}
