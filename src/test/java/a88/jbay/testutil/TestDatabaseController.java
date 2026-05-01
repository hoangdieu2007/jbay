package a88.jbay.testutil;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Test database controller that uses H2 in-memory database for testing
 */
public class TestDatabaseController {
    
    private static final String TEST_DB_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    private static final String TEST_DB_USER = "sa";
    private static final String TEST_DB_PASSWORD = "";
    
    public static Connection getConnection() throws SQLException {
        return java.sql.DriverManager.getConnection(TEST_DB_URL, TEST_DB_USER, TEST_DB_PASSWORD);
    }
}
