package a88.jbay.server;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interface for database connection providers to enable dependency injection
 */
public interface DatabaseConnectionProvider {
    Connection getConnection() throws SQLException;
}
