package a88.jbay.server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

// singleton class for database controlling
// requirement: thread safe
public class DatabaseController implements DatabaseConnectionProvider {
    private static DatabaseController instance;
    private final HikariDataSource dataSource;

    public static String url;
    public static String username;
    public static String password;

    private DatabaseController() {
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
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}