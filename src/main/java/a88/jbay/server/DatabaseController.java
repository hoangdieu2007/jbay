package a88.jbay.server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import a88.jbay.util.JBayLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

// singleton class for database controlling
// requirement: thread safe
public class DatabaseController implements DatabaseConnectionProvider {
    private static DatabaseController instance;
    private final HikariDataSource dataSource;
    private final JBayLogger logger;

    public static String url;
    public static String username;
    public static String password;

    private DatabaseController() {
        this.logger = JBayLogger.getInstance();
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);

        // Pool configuration
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(20000);
        config.setMaxLifetime(1800000);

        this.dataSource = new HikariDataSource(config);
        logger.info("Database connection pool initialized with URL: " + url);
    }

    public static synchronized DatabaseController getInstance() {
        if (instance == null) {
            instance = new DatabaseController();
        }
        return instance;
    }

    public static void setCredentials(String inputUrl, String inputUsername, String inputPassword) {
        url = inputUrl;
        username = inputUsername;
        password = inputPassword;
    }

    public Connection getConnection() throws SQLException {
        logger.debug("Database connection requested");
        Connection connection = dataSource.getConnection();
        logger.debug("Database connection established");
        return connection;
    }

    public void close() {
        logger.info("Closing database connection pool");
        if (dataSource != null) {
            dataSource.close();
            logger.info("Database connection pool closed successfully");
        }
    }
}