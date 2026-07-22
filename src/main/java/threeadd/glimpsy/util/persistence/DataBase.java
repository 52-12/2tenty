package threeadd.glimpsy.util.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import threeadd.glimpsy.util.ScheduleUtil;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DataBase {

    private final File file;
    private HikariDataSource dataSource;

    public DataBase(File file) {
        this.file = file;
    }

    public void connect() throws SQLException {
        file.getParentFile().mkdirs();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
        config.setMaximumPoolSize(1);
        config.setConnectionTestQuery("SELECT 1");
        dataSource = new HikariDataSource(config);

        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute("PRAGMA journal_mode=WAL");
        }
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void disconnect() {
        if (dataSource != null) dataSource.close();
    }

    public void runAsyncUpdate(String sql, SQLConsumer<PreparedStatement> setter) {
        ScheduleUtil.scheduleAsync(() -> {
            try {
                this.runSyncUpdate(sql, setter);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void runSyncUpdate(String sql, SQLConsumer<PreparedStatement> setter) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            if (setter != null) setter.accept(ps);
            ps.executeUpdate();
        }
    }

    @FunctionalInterface
    public interface SQLConsumer<T> {
        void accept(T t);
    }
}
