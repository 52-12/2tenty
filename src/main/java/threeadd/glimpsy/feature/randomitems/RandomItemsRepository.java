package threeadd.glimpsy.feature.randomitems;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.bukkit.Material;
import threeadd.glimpsy.util.persistence.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RandomItemsRepository extends Repository<RandomItemsInfo> {

    public static final RandomItemsRepository instance = new RandomItemsRepository();

    public static RandomItemsRepository getInstance() {
        return instance;
    }

    @Override
    protected void createTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS player_items (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "data TEXT NOT NULL" +
                    ")");
        }
    }

    // Only the disabled materials and the alert flag are ever non-default, so that's all that's
    // stored - not the full state map.
    private record StoredData(List<Material> disabled, boolean alertsEnabled) {
    }

    public void saveSync(RandomItemsInfo info) {
        String sql = "REPLACE INTO player_items (uuid, data) VALUES (?, ?)";
        executeUpdate(sql, info.getOwnerId().toString(), serialize(info));
    }

    private String serialize(RandomItemsInfo info) {

        List<Material> disabledMaterials = new ArrayList<>();
        info.getStates().forEach((material, state) -> {
            if (!state) // only store disabled states
                disabledMaterials.add(material);
        });

        return new Gson().toJson(new StoredData(disabledMaterials, info.isFullInventoryAlertEnabled()));
    }

    public Optional<RandomItemsInfo> loadSync(UUID uuid) {
        return querySingle("SELECT * FROM player_items WHERE uuid = ?", this::deserialize, uuid.toString());
    }

    private RandomItemsInfo deserialize(ResultSet rs) throws SQLException {
        String json = rs.getString("data");

        UUID ownerId = UUID.fromString(rs.getString("uuid"));

        // Rows saved before the alert flag existed are just a bare JSON array of materials.
        if (json.stripLeading().startsWith("[")) {
            List<Material> disabledItems = new Gson().fromJson(json, new TypeToken<List<Material>>() {
            }.getType());
            return new RandomItemsInfo(ownerId, disabledItems, true);
        }

        StoredData data = new Gson().fromJson(json, StoredData.class);
        return new RandomItemsInfo(ownerId, data.disabled(), data.alertsEnabled());
    }

    public void deleteSync(UUID uuid) {
        String sql = "DELETE FROM player_items WHERE uuid = ?";
        executeUpdate(sql, uuid.toString());
    }

    public CompletableFuture<Void> deleteAsync(UUID uuid) {
        return runAsync(() -> deleteSync(uuid));
    }

    public CompletableFuture<Optional<RandomItemsInfo>> loadAsync(UUID uuid) {
        return supplyAsync(() -> loadSync(uuid));
    }

    public CompletableFuture<Void> saveAsync(RandomItemsInfo info) {
        return runAsync(() -> saveSync(info));
    }
}
