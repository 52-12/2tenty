package threeadd.glimpsy.util.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import threeadd.glimpsy.util.ScheduleUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DataBase {

    private final String host;
    private final String database;
    private final String username;
    private final String password;
    private HikariDataSource dataSource;

    public DataBase(String host, String database, String username, String password) {
        this.host = host;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public void connect() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource = new HikariDataSource(config);
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
