package threeadd.glimpsy.feature.nickname;

import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.persistence.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NicknameRepository extends Repository<String> {

    private static final NicknameRepository instance = new NicknameRepository();

    public static NicknameRepository getInstance() {
        return instance;
    }

    @Override
    protected void createTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS player_nicknames (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "nickname TEXT NOT NULL" +
                    ")");
        }
    }

    public Map<UUID, String> loadAllSync() {
        Map<UUID, String> result = new HashMap<>();

        try (Connection conn = Glimpsy.getDataBase().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT uuid, nickname FROM player_nicknames")) {

            while (rs.next())
                result.put(UUID.fromString(rs.getString("uuid")), rs.getString("nickname"));

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load nicknames", e);
        }

        return result;
    }

    public void setNicknameSync(UUID uuid, String rawName) {
        executeUpdate("REPLACE INTO player_nicknames (uuid, nickname) VALUES (?, ?)", uuid.toString(), rawName);
    }

    public void clearNicknameSync(UUID uuid) {
        executeUpdate("DELETE FROM player_nicknames WHERE uuid = ?", uuid.toString());
    }

    public void runAsyncSave(UUID uuid, String rawName) {
        runAsync(() -> setNicknameSync(uuid, rawName));
    }

    public void runAsyncClear(UUID uuid) {
        runAsync(() -> clearNicknameSync(uuid));
    }
}
