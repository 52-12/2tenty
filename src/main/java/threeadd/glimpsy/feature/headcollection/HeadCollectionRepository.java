package threeadd.glimpsy.feature.headcollection;

import org.bukkit.inventory.ItemStack;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.persistence.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HeadCollectionRepository extends Repository<HeadCollection> {

    private static final HeadCollectionRepository instance = new HeadCollectionRepository();

    public static HeadCollectionRepository getInstance() {
        return instance;
    }

    @Override
    protected void createTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS head_collections (" +
                    "owner VARCHAR(36) PRIMARY KEY," +
                    "contents BLOB)");
        }
    }

    // Same trick as the trade chests - Paper's version-safe item serialization, with null slots
    // written as empty stacks so the slot layout survives a round trip
    private static byte[] serializeContents(ItemStack[] contents) {
        ItemStack[] safe = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++)
            safe[i] = contents[i] == null ? ItemStack.empty() : contents[i];
        return ItemStack.serializeItemsAsBytes(safe);
    }

    private static ItemStack[] deserializeContents(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return new ItemStack[0];

        ItemStack[] raw = ItemStack.deserializeItemsFromBytes(bytes);
        for (int i = 0; i < raw.length; i++)
            if (raw[i] != null && raw[i].isEmpty()) raw[i] = null;
        return raw;
    }

    public Map<UUID, HeadCollection> loadAllSync() {
        Map<UUID, HeadCollection> collections = new HashMap<>();

        try (Connection conn = Glimpsy.getDataBase().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM head_collections")) {
            while (rs.next()) {
                UUID owner = UUID.fromString(rs.getString("owner"));
                HeadCollection collection = new HeadCollection(owner);
                collection.setContents(deserializeContents(rs.getBytes("contents")));
                collections.put(owner, collection);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load head collections", e);
        }

        return collections;
    }

    public void saveSync(HeadCollection collection) {
        executeUpdate("REPLACE INTO head_collections (owner, contents) VALUES (?, ?)",
                collection.getOwner().toString(),
                serializeContents(collection.getContents()));
    }

    public void runAsyncSave(HeadCollection collection) {
        runAsync(() -> saveSync(collection));
    }
}
