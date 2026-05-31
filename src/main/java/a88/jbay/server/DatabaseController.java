package a88.jbay.server;

import a88.jbay.di.ApplicationContext;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import a88.jbay.util.JBayLogger;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseController {
    private volatile HikariDataSource dataSource;
    private final JBayLogger logger;

    public DatabaseController() {
        this.logger = JBayLogger.getLogger(DatabaseController.class);
    }

    public synchronized void initializePool(String url, String username, String password) {
        initializePool(url, username, password, 10);
    }

    public synchronized void initializePool(String url, String username, String password, int maxPoolSize) {
        if (url == null || username == null) {
            throw new IllegalStateException("Database credentials have not been set!");
        }

        // Close old pool before reinitializing
        if (dataSource != null) {
            dataSource.close();
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);

        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(20000);
        config.setMaxLifetime(1800000);

        this.dataSource = new HikariDataSource(config);
        logger.info("Database connection pool initialized with URL: " + url);
    }

    public Connection getConnection() throws SQLException {
        // ensure pool exists before handing out connections
        if (dataSource == null) {
            throw new IllegalStateException("Database connection pool has not been initialized");
        }

        logger.debug("Database connection requested");
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
            logger.info("Database connection pool closed successfully");
        }
    }
}