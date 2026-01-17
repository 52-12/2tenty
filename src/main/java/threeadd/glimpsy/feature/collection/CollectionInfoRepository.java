package threeadd.glimpsy.feature.collection;

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

public class CollectionInfoRepository extends Repository<CollectionInfo> {

    public static final CollectionInfoRepository instance = new CollectionInfoRepository();

    public static CollectionInfoRepository getInstance() {
        return instance;
    }

    @Override
    protected void createTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS player_collections (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "data TEXT NOT NULL" +
                    ")");
        }
    }

    public void saveSync(CollectionInfo info) {
        String sql = "REPLACE INTO player_collections (uuid, data) VALUES (?, ?)";
        executeUpdate(sql, info.getOwnerId().toString(), serialize(info));
    }

    private String serialize(CollectionInfo info) {

        List<Material> collectedMaterial = new ArrayList<>();
        info.getStates().forEach((material, state) -> {
            if (state) // only store collected states
                collectedMaterial.add(material);
        });

        return new Gson().toJson(collectedMaterial);
    }

    public Optional<CollectionInfo> loadSync(UUID uuid) {
        return querySingle("SELECT * FROM player_collections WHERE uuid = ?", this::deserialize, uuid.toString());
    }

    private CollectionInfo deserialize(ResultSet rs) throws SQLException {
        String json = rs.getString("data");

        UUID ownerId = UUID.fromString(rs.getString("uuid"));

        List<Material> collectedMaterials = new Gson().fromJson(
                json,
                new TypeToken<List<Material>>() {
                }.getType()
        );

        return new CollectionInfo(ownerId, collectedMaterials);
    }

    public void deleteSync(UUID uuid) {
        String sql = "DELETE FROM player_collections WHERE uuid = ?";
        executeUpdate(sql, uuid.toString());
    }

    public CompletableFuture<Void> deleteAsync(UUID uuid) {
        return runAsync(() -> deleteSync(uuid));
    }

    public CompletableFuture<Optional<CollectionInfo>> loadAsync(UUID uuid) {
        return supplyAsync(() -> loadSync(uuid));
    }

    public CompletableFuture<Void> saveAsync(CollectionInfo info) {
        return runAsync(() -> saveSync(info));
    }
}
