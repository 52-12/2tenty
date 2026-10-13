package threeadd.glimpsy.feature.simple;

import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.persistence.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers which block positions are player-placed Salt. Salt is stored as white concrete powder,
 * so without this a placed block would be indistinguishable from the vanilla block - breaking it
 * would yield plain powder and water would turn it into concrete.
 */
public class SaltBlockRepository extends Repository<SaltBlockRepository.Pos> {

    public record Pos(String world, int x, int y, int z) {
    }

    private static final SaltBlockRepository instance = new SaltBlockRepository();

    public static SaltBlockRepository getInstance() {
        return instance;
    }

    @Override
    protected void createTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS salt_blocks (" +
                    "world VARCHAR(64) NOT NULL," +
                    "x INTEGER NOT NULL," +
                    "y INTEGER NOT NULL," +
                    "z INTEGER NOT NULL," +
                    "PRIMARY KEY (world, x, y, z)" +
                    ")");
        }
    }

    public Set<Pos> loadAllSync() {
        Set<Pos> positions = ConcurrentHashMap.newKeySet();

        try (Connection conn = Glimpsy.getDataBase().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT world, x, y, z FROM salt_blocks")) {

            while (rs.next())
                positions.add(new Pos(rs.getString("world"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z")));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load placed salt blocks", e);
        }

        return positions;
    }

    public CompletableFuture<Void> addAsync(Pos pos) {
        return runAsync(() -> executeUpdate("REPLACE INTO salt_blocks (world, x, y, z) VALUES (?, ?, ?, ?)",
                pos.world(), pos.x(), pos.y(), pos.z()));
    }

    public CompletableFuture<Void> removeAsync(Pos pos) {
        return runAsync(() -> executeUpdate("DELETE FROM salt_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?",
                pos.world(), pos.x(), pos.y(), pos.z()));
    }
}
