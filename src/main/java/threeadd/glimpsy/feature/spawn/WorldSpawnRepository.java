package threeadd.glimpsy.feature.spawn;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import threeadd.glimpsy.util.persistence.Repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class WorldSpawnRepository extends Repository<Location> {

    private static final WorldSpawnRepository instance = new WorldSpawnRepository();

    public static WorldSpawnRepository getInstance() {
        return instance;
    }

    @Override
    protected void createTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS world_spawn (" +
                    "id INTEGER PRIMARY KEY CHECK (id = 1)," +
                    "world VARCHAR(64) NOT NULL," +
                    "x DOUBLE NOT NULL," +
                    "y DOUBLE NOT NULL," +
                    "z DOUBLE NOT NULL," +
                    "yaw FLOAT NOT NULL," +
                    "pitch FLOAT NOT NULL" +
                    ")");
        }
    }

    public void saveSync(Location location) {
        executeUpdate("REPLACE INTO world_spawn (id, world, x, y, z, yaw, pitch) VALUES (1, ?, ?, ?, ?, ?, ?)",
                location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

    public Optional<Location> loadSync() {
        return querySingle("SELECT * FROM world_spawn WHERE id = 1", rs -> {
            World world = Bukkit.getWorld(rs.getString("world"));
            if (world == null) return null;

            return new Location(world, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                    rs.getFloat("yaw"), rs.getFloat("pitch"));
        });
    }
}
