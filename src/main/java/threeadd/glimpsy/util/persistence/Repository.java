package threeadd.glimpsy.util.persistence;

import org.bukkit.Bukkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threeadd.glimpsy.Glimpsy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public abstract class Repository<T> {

    private static final Logger log = LoggerFactory.getLogger(Repository.class);

    public Repository() {
        initTable(); // Ensure table exists on startup
    }

    protected abstract void createTable(Connection conn) throws SQLException;

    private void initTable() {
        try (Connection conn = Glimpsy.getDataBase().getConnection()) {
            createTable(conn);
        } catch (SQLException e) {
            log.error("Failed to init table for ", e);
        }
    }

    protected Optional<T> querySingle(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection conn = Glimpsy.getDataBase().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setParams(ps, params);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(mapper.map(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to query table for ", e);
        }
        return Optional.empty();
    }

    protected void executeUpdate(String sql, Object... params) {
        try (Connection conn = Glimpsy.getDataBase().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setParams(ps, params);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Database update failed", e);
        }
    }

    protected CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable,
                task -> Bukkit.getScheduler().runTaskAsynchronously(Glimpsy.getInstance(), task));
    }

    protected <R> CompletableFuture<R> supplyAsync(Supplier<R> supplier) {
        return CompletableFuture.supplyAsync(supplier,
                task -> Bukkit.getScheduler().runTaskAsynchronously(Glimpsy.getInstance(), task));
    }

    private void setParams(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
}