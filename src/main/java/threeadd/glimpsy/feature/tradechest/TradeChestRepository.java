package threeadd.glimpsy.feature.tradechest;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.persistence.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TradeChestRepository extends Repository<TradeChestData> {

    private static final TradeChestRepository instance = new TradeChestRepository();

    public static TradeChestRepository getInstance() {
        return instance;
    }

    @Override
    protected void createTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS trade_chests (" +
                    "owner VARCHAR(36) PRIMARY KEY," +
                    "world VARCHAR(64) NOT NULL," +
                    "x INT NOT NULL, y INT NOT NULL, z INT NOT NULL," +
                    "contents BLOB)");
            stmt.execute("CREATE TABLE IF NOT EXISTS trade_offers (" +
                    "from_id VARCHAR(36) PRIMARY KEY," +
                    "to_id VARCHAR(36) NOT NULL," +
                    "stage VARCHAR(16) NOT NULL," +
                    "from_accepted INT NOT NULL DEFAULT 0," +
                    "to_accepted INT NOT NULL DEFAULT 0)");
            stmt.execute("CREATE TABLE IF NOT EXISTS trade_deliveries (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "recipient VARCHAR(36) NOT NULL," +
                    "sender VARCHAR(36) NOT NULL," +
                    "item BLOB NOT NULL," +
                    "deliver_at BIGINT NOT NULL)");
        }
    }

    // ItemStack[] <-> BLOB via Paper's version-safe serialization; null slots stored as empty
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

    public Map<UUID, TradeChestData> loadChestsSync() {
        Map<UUID, TradeChestData> chests = new HashMap<>();

        try (Connection conn = Glimpsy.getDataBase().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM trade_chests")) {
            while (rs.next()) {
                World world = Bukkit.getWorld(rs.getString("world"));
                if (world == null) continue;

                UUID owner = UUID.fromString(rs.getString("owner"));
                TradeChestData data = new TradeChestData(owner,
                        new Location(world, rs.getInt("x"), rs.getInt("y"), rs.getInt("z")));
                data.setContents(deserializeContents(rs.getBytes("contents")));
                chests.put(owner, data);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load trade chests", e);
        }

        return chests;
    }

    public void saveChestSync(TradeChestData data) {
        executeUpdate("REPLACE INTO trade_chests (owner, world, x, y, z, contents) VALUES (?, ?, ?, ?, ?, ?)",
                data.getOwner().toString(),
                data.getLocation().getWorld().getName(),
                data.getLocation().getBlockX(),
                data.getLocation().getBlockY(),
                data.getLocation().getBlockZ(),
                serializeContents(data.getContents()));
    }

    public void runAsyncSaveChest(TradeChestData data) {
        runAsync(() -> saveChestSync(data));
    }

    public void runAsyncDeleteChest(UUID owner) {
        runAsync(() -> executeUpdate("DELETE FROM trade_chests WHERE owner = ?", owner.toString()));
    }

    public List<TradeOffer> loadOffersSync() {
        List<TradeOffer> offers = new ArrayList<>();

        try (Connection conn = Glimpsy.getDataBase().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM trade_offers")) {
            while (rs.next()) {
                TradeOffer offer = new TradeOffer(
                        UUID.fromString(rs.getString("from_id")),
                        UUID.fromString(rs.getString("to_id")));
                offer.setStage(TradeOffer.Stage.valueOf(rs.getString("stage")));
                offer.setAccepted(offer.getFrom(), rs.getInt("from_accepted") != 0);
                offer.setAccepted(offer.getTo(), rs.getInt("to_accepted") != 0);
                offers.add(offer);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load trade offers", e);
        }

        return offers;
    }

    public void runAsyncSaveOffer(TradeOffer offer) {
        runAsync(() -> executeUpdate(
                "REPLACE INTO trade_offers (from_id, to_id, stage, from_accepted, to_accepted) VALUES (?, ?, ?, ?, ?)",
                offer.getFrom().toString(), offer.getTo().toString(), offer.getStage().name(),
                offer.isFromAccepted() ? 1 : 0, offer.isToAccepted() ? 1 : 0));
    }

    public void runAsyncDeleteOffer(UUID fromId) {
        runAsync(() -> executeUpdate("DELETE FROM trade_offers WHERE from_id = ?", fromId.toString()));
    }

    public List<TradeDelivery> loadDeliveriesSync() {
        List<TradeDelivery> deliveries = new ArrayList<>();

        try (Connection conn = Glimpsy.getDataBase().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM trade_deliveries")) {
            while (rs.next()) {
                TradeDelivery delivery = new TradeDelivery(
                        UUID.fromString(rs.getString("recipient")),
                        UUID.fromString(rs.getString("sender")),
                        ItemStack.deserializeBytes(rs.getBytes("item")),
                        rs.getLong("deliver_at"));
                delivery.setId(rs.getLong("id"));
                deliveries.add(delivery);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load trade deliveries", e);
        }

        return deliveries;
    }

    public long insertDeliverySync(TradeDelivery delivery) {
        try (Connection conn = Glimpsy.getDataBase().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO trade_deliveries (recipient, sender, item, deliver_at) VALUES (?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, delivery.getRecipient().toString());
            ps.setString(2, delivery.getSender().toString());
            ps.setBytes(3, delivery.getItem().serializeAsBytes());
            ps.setLong(4, delivery.getDeliverAt());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert trade delivery", e);
        }

        return -1;
    }

    public void runAsyncDeleteDelivery(long id) {
        runAsync(() -> executeUpdate("DELETE FROM trade_deliveries WHERE id = ?", id));
    }

    /** Inserts off-thread and only then adds to the live list, so the id is always set. */
    public void runAsyncInsertDelivery(TradeDelivery delivery, List<TradeDelivery> addTo) {
        runAsync(() -> {
            delivery.setId(insertDeliverySync(delivery));
            addTo.add(delivery);
        });
    }
}
