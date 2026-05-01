package a88.jbay.server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

// singleton class for database controlling
// requirement: thread safe
public class DatabaseController implements DatabaseConnectionProvider {
    private static DatabaseController instance;
    private final HikariDataSource dataSource;

    private DatabaseController() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/jbay_db");
        config.setUsername("root");
        config.setPassword("220407");

        // Pool configuration
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(20000);
        config.setMaxLifetime(1800000);

        this.dataSource = new HikariDataSource(config);
    }

    public static synchronized DatabaseController getInstance() {
        if (instance == null) {
            instance = new DatabaseController();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}