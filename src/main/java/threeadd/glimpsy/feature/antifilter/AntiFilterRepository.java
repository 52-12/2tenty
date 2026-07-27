package threeadd.glimpsy.feature.antifilter;

import org.bukkit.Material;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.persistence.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Persists each player's protected material sets (a comma-separated list of material names per
 * filter type). The GUI only ever holds display copies, so there are no item stacks to store.
 */
public class AntiFilterRepository extends Repository<Void> {

    private static final AntiFilterRepository instance = new AntiFilterRepository();

    public static AntiFilterRepository getInstance() {
        return instance;
    }

    @Override
    protected void createTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 'contents' predates this feature's rework - it now holds a material-name CSV
            stmt.execute("CREATE TABLE IF NOT EXISTS anti_filters (" +
                    "player_id VARCHAR(36) NOT NULL," +
                    "filter_type VARCHAR(16) NOT NULL," +
                    "contents TEXT," +
                    "PRIMARY KEY (player_id, filter_type))");
        }
    }

    public Map<UUID, Map<AntiFilterType, Set<Material>>> loadAllSync() {
        Map<UUID, Map<AntiFilterType, Set<Material>>> result = new HashMap<>();

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

                Set<Material> materials = new HashSet<>();
                String csv = rs.getString("contents");
                if (csv != null && !csv.isBlank()) {
                    for (String name : csv.split(",")) {
                        Material material = Material.matchMaterial(name.trim());
                        if (material != null) materials.add(material);
                    }
                }

                result.computeIfAbsent(id, x -> new EnumMap<>(AntiFilterType.class)).put(type, materials);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load anti filters", e);
        }

        return result;
    }

    public void runAsyncSave(UUID playerId, AntiFilterType type, Set<Material> materials) {
        StringJoiner joiner = new StringJoiner(",");
        for (Material material : materials) joiner.add(material.name());
        String csv = joiner.toString();

        runAsync(() -> executeUpdate("REPLACE INTO anti_filters (player_id, filter_type, contents) VALUES (?, ?, ?)",
                playerId.toString(), type.name(), csv));
    }
}
