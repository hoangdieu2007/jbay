package a88.jbay.dao;

import a88.jbay.server.DatabaseConnectionProvider;
import a88.jbay.testutil.TestDatabaseConnectionProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Manual mock test for UserDAO using Java 25 compatible approach
 * This replaces Mockito with manual test doubles
 */
class UserDAOMockTest {
    
    private TestDatabaseConnectionProvider testDbProvider;
    private UserDAO userDAO;
    
    @BeforeEach
    void setUp() {
        // Set up test database credentials
        a88.jbay.server.DatabaseController.setCredentials(
            "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", 
            "testuser", 
            "testpass"
        );
        
        testDbProvider = new TestDatabaseConnectionProvider();
        userDAO = new UserDAO(testDbProvider);
    }
    
    @Test
    @DisplayName("Should handle database exception using manual mock")
    void testFindByUsername_DatabaseError() throws SQLException {
        // Arrange
        testDbProvider.setShouldThrowException(true);
        
        // Act
        UserDAO.UserData result = userDAO.findByUsername("testuser");
        
        // Assert - should return null on exception
        assertNull(result, "Database error should return null");
    }
    
    @Test
    @DisplayName("Should handle insert user database error using manual mock")
    void testInsertUser_DatabaseError() throws SQLException {
        // Arrange
        testDbProvider.setShouldThrowException(true);
        
        // Act
        int result = userDAO.insertUser("testuser", "password", "BIDDER");
        
        // Assert
        assertEquals(-1, result, "Database error should return -1");
    }
    
    @Test
    @DisplayName("Should handle existsByUsername database error using manual mock")
    void testExistsByUsername_DatabaseError() throws SQLException {
        // Arrange
        testDbProvider.setShouldThrowException(true);
        
        // Act
        boolean result = userDAO.existsByUsername("testuser");
        
        // Assert
        assertFalse(result, "Database error should return false");
    }
    
    @Test
    @DisplayName("Should handle findBySessionId database error using manual mock")
    void testFindBySessionId_DatabaseError() throws SQLException {
        // Arrange
        testDbProvider.setShouldThrowException(true);
        
        // Act
        UserDAO.UserData result = userDAO.findBySessionId("session123");
        
        // Assert
        assertNull(result, "Database error should return null");
    }
    
    @Test
    @DisplayName("Should reset test state")
    void testReset() {
        // Arrange
        testDbProvider.setShouldThrowException(true);
        testDbProvider.addMockData("test", "value");
        
        // Act
        testDbProvider.reset();
        
        // Assert
        assertFalse(testDbProvider.isShouldThrowException(), "Should reset exception flag");
        assertNull(testDbProvider.getMockData("test"), "Should clear mock data");
    }
}
