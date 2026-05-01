package a88.jbay.testutil;

import a88.jbay.server.DatabaseController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * test database utilities
 */
public class TestDatabaseSetup {
    
    private static final String TEST_DB_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    private static final String TEST_DB_USER = "sa";
    private static final String TEST_DB_PASSWORD = "";
    
    /**
     * init the test database with schema
     */
    public static void initializeTestDatabase() throws SQLException {
        try (Connection conn = getTestConnection()) {
            createTables(conn);
        }
    }
    
    /**
     * clean up all test data
     */
    public static void cleanupTestDatabase() throws SQLException {
        try (Connection conn = getTestConnection()) {
            // Delete in order of dependencies
            executeUpdate(conn, "DELETE FROM bids");
            executeUpdate(conn, "DELETE FROM sessionids");
            executeUpdate(conn, "DELETE FROM auctions");
            executeUpdate(conn, "DELETE FROM items");
            executeUpdate(conn, "DELETE FROM users");
        }
    }
    
    /**
     * Get test database connection
     */
    public static Connection getTestConnection() throws SQLException {
        return java.sql.DriverManager.getConnection(TEST_DB_URL, TEST_DB_USER, TEST_DB_PASSWORD);
    }
    
    /**
     * Create test database schema
     */
    private static void createTables(Connection conn) throws SQLException {
        // Users table
        executeUpdate(conn, """
            CREATE TABLE users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(50) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL,
                role VARCHAR(20) NOT NULL
            )
        """);
        
        // Items table
        executeUpdate(conn, """
            CREATE TABLE items (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                type VARCHAR(50) NOT NULL,
                desc TEXT,
                start_price DOUBLE NOT NULL,
                image LONGBLOB
            )
        """);
        
        // Auctions table
        executeUpdate(conn, """
            CREATE TABLE auctions (
                id INT AUTO_INCREMENT PRIMARY KEY,
                item INT NOT NULL,
                seller INT NOT NULL,
                start_price DOUBLE NOT NULL,
                cur_price DOUBLE NOT NULL,
                winner INT,
                start_time TIMESTAMP NOT NULL,
                end_time TIMESTAMP NOT NULL,
                state VARCHAR(20) NOT NULL,
                FOREIGN KEY (item) REFERENCES items(id),
                FOREIGN KEY (seller) REFERENCES users(id)
            )
        """);
        
        // Bids table
        executeUpdate(conn, """
            CREATE TABLE bids (
                userid INT NOT NULL,
                auctionid INT NOT NULL,
                amt DOUBLE NOT NULL,
                time TIMESTAMP NOT NULL,
                PRIMARY KEY (userid, auctionid, time),
                FOREIGN KEY (userid) REFERENCES users(id),
                FOREIGN KEY (auctionid) REFERENCES auctions(id)
            )
        """);
        
        // Sessions table
        executeUpdate(conn, """
            CREATE TABLE sessionids (
                id VARCHAR(255) PRIMARY KEY,
                userid INT NOT NULL,
                FOREIGN KEY (userid) REFERENCES users(id)
            )
        """);
    }
    
    /**
     * execute a simple update statement
     */
    private static void executeUpdate(Connection conn, String sql) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }
    
    /**
     * Insert test user
     */
    public static int insertTestUser(Connection conn, String username, String password, String role) throws SQLException {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, role);
            stmt.executeUpdate();
            
            var rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }
    
    /**
     * Insert test item
     */
    public static int insertTestItem(Connection conn, String name, String type, String description, double startPrice) throws SQLException {
        String sql = "INSERT INTO items (name, type, desc, start_price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setString(2, type);
            stmt.setString(3, description);
            stmt.setDouble(4, startPrice);
            stmt.executeUpdate();
            
            var rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }
    
    /**
     * Insert test auction
     */
    public static int insertTestAuction(Connection conn, int itemId, int sellerId, double startPrice, 
                                      double curPrice, LocalDateTime startTime, LocalDateTime endTime, String state) throws SQLException {
        String sql = "INSERT INTO auctions (item, seller, start_price, cur_price, start_time, end_time, state) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, itemId);
            stmt.setInt(2, sellerId);
            stmt.setDouble(3, startPrice);
            stmt.setDouble(4, curPrice);
            stmt.setObject(5, startTime);
            stmt.setObject(6, endTime);
            stmt.setString(7, state);
            stmt.executeUpdate();
            
            var rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }
}
