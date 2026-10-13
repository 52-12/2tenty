package threeadd.glimpsy.feature.antifilter;

import org.bukkit.inventory.ItemStack;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.persistence.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persists each player's protected item samples per filter type. Storing the whole item (not just
 * its material) is what lets enchanted items be matched specifically - see AntiFilterManager.
 */
public class AntiFilterRepository extends Repository<Void> {

    private static final AntiFilterRepository instance = new AntiFilterRepository();

    public static AntiFilterRepository getInstance() {
        return instance;
    }

    @Override
    protected void createTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS anti_filters (" +
                    "player_id VARCHAR(36) NOT NULL," +
                    "filter_type VARCHAR(16) NOT NULL," +
                    "contents BLOB," +
                    "PRIMARY KEY (player_id, filter_type))");
        }
    }

    private static List<ItemStack> deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return new ArrayList<>();
        try {
            return new ArrayList<>(Arrays.asList(ItemStack.deserializeItemsFromBytes(bytes)));
        } catch (Exception e) {
            // Data from an older format of this feature - start that filter fresh rather than crash
            return new ArrayList<>();
        }
    }

    public Map<UUID, Map<AntiFilterType, List<ItemStack>>> loadAllSync() {
        Map<UUID, Map<AntiFilterType, List<ItemStack>>> result = new HashMap<>();

        try (Connection conn = Glimpsy.getDataBase().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM anti_filters")) {
            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("player_id"));
                AntiFilterType type;
                try {
                    type = AntiFilterType.valueOf(rs.getString("filter_type"));
                } catch (IllegalArgumentException ignored) {
                    continue;
                }

                result.computeIfAbsent(id, x -> new EnumMap<>(AntiFilterType.class))
                        .put(type, deserialize(rs.getBytes("contents")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load anti filters", e);
        }

        return result;
    }

    public void runAsyncSave(UUID playerId, AntiFilterType type, List<ItemStack> samples) {
        byte[] bytes = ItemStack.serializeItemsAsBytes(samples.toArray(new ItemStack[0]));
        runAsync(() -> executeUpdate("REPLACE INTO anti_filters (player_id, filter_type, contents) VALUES (?, ?, ?)",
                playerId.toString(), type.name(), bytes));
    }
}
