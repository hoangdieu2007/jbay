package a88.jbay.dao;

import a88.jbay.server.DatabaseController;
import a88.jbay.testutil.TestDatabaseSetup;
import a88.jbay.testutil.TestDataFactory;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserDAOTest {
    
    private UserDAO userDAO;
    private Connection testConnection;
    private MockedStatic<DatabaseController> mockedDatabaseController;
    
    @BeforeEach
    void setUp() throws SQLException {
        // Initialize test database
        TestDatabaseSetup.initializeTestDatabase();
        testConnection = TestDatabaseSetup.getTestConnection();
        
        // Mock DatabaseController to return test connection
        mockedDatabaseController = mockStatic(DatabaseController.class);
        mockedDatabaseController.when(DatabaseController::getInstance).thenReturn(mock(DatabaseController.class));
        when(DatabaseController.getInstance().getConnection()).thenReturn(testConnection);
        
        // Get UserDAO instance
        userDAO = UserDAO.getInstance();
    }
    
    @AfterEach
    void tearDown() throws SQLException {
        // Clean up test database
        TestDatabaseSetup.cleanupTestDatabase();
        
        // Close test connection
        if (testConnection != null && !testConnection.isClosed()) {
            testConnection.close();
        }
        
        // Close mocked static
        if (mockedDatabaseController != null) {
            mockedDatabaseController.close();
        }
    }
    
    @Test
    @DisplayName("Should insert user successfully")
    void testInsertUser_Success() throws SQLException {
        // Arrange
        String username = TestDataFactory.getTestUsername();
        String password = TestDataFactory.getTestPassword();
        String role = "BIDDER";
        
        // Act
        int userId = userDAO.insertUser(username, password, role);
        
        // Assert
        assertTrue(userId > 0, "User ID should be positive");
        
        // Verify user was actually inserted
        UserDAO.UserData userData = userDAO.findByUsername(username);
        assertNotNull(userData, "User should be found after insertion");
        assertEquals(username, userData.username(), "Username should match");
        assertEquals(role, userData.role(), "Role should match");
        assertNotNull(userData.password(), "Password should be stored");
    }
    
    @Test
    @DisplayName("Should return -1 when inserting user with duplicate username")
    void testInsertUser_DuplicateUsername() throws SQLException {
        // Arrange
        String username = TestDataFactory.getTestUsername();
        String password = TestDataFactory.getTestPassword();
        String role = "BIDDER";
        
        // Insert first user
        int firstUserId = userDAO.insertUser(username, password, role);
        assertTrue(firstUserId > 0, "First user should be inserted successfully");
        
        // Act - Try to insert duplicate
        int secondUserId = userDAO.insertUser(username, password, role);
        
        // Assert
        assertEquals(-1, secondUserId, "Duplicate username should return -1");
    }
    
    @Test
    @DisplayName("Should find user by username successfully")
    void testFindByUsername_Success() throws SQLException {
        // Arrange
        String username = TestDataFactory.getTestUsername();
        String password = TestDataFactory.getTestPassword();
        String role = "SELLER";
        
        // Insert test user
        int userId = TestDatabaseSetup.insertTestUser(testConnection, username, password, role);
        assertTrue(userId > 0, "Test user should be inserted");
        
        // Act
        UserDAO.UserData userData = userDAO.findByUsername(username);
        
        // Assert
        assertNotNull(userData, "User should be found");
        assertEquals(userId, userData.id(), "User ID should match");
        assertEquals(username, userData.username(), "Username should match");
        assertEquals(role, userData.role(), "Role should match");
        assertEquals(password, userData.password(), "Password should match");
    }
    
    @Test
    @DisplayName("Should return null when finding non-existent username")
    void testFindByUsername_NotFound() {
        // Arrange
        String nonExistentUsername = "nonexistentuser12345";
        
        // Act
        UserDAO.UserData userData = userDAO.findByUsername(nonExistentUsername);
        
        // Assert
        assertNull(userData, "Non-existent user should return null");
    }
    
    @Test
    @DisplayName("Should find user by ID successfully")
    void testFindByUserId_Success() throws SQLException {
        // Arrange
        String username = TestDataFactory.getTestUsername();
        String password = TestDataFactory.getTestPassword();
        String role = "ADMIN";
        
        // Insert test user
        int userId = TestDatabaseSetup.insertTestUser(testConnection, username, password, role);
        assertTrue(userId > 0, "Test user should be inserted");
        
        // Act
        UserDAO.UserData userData = userDAO.findByUserId(userId);
        
        // Assert
        assertNotNull(userData, "User should be found");
        assertEquals(userId, userData.id(), "User ID should match");
        assertEquals(username, userData.username(), "Username should match");
        assertEquals(role, userData.role(), "Role should match");
        assertEquals(password, userData.password(), "Password should match");
    }
    
    @Test
    @DisplayName("Should return null when finding non-existent user ID")
    void testFindByUserId_NotFound() {
        // Arrange
        int nonExistentUserId = 99999;
        
        // Act
        UserDAO.UserData userData = userDAO.findByUserId(nonExistentUserId);
        
        // Assert
        assertNull(userData, "Non-existent user ID should return null");
    }
    
    @Test
    @DisplayName("Should find user by session ID successfully")
    void testFindBySessionId_Success() throws SQLException {
        // Arrange
        String username = TestDataFactory.getTestUsername();
        String password = TestDataFactory.getTestPassword();
        String role = "BIDDER";
        String sessionId = "test-session-123";
        
        // Insert test user
        int userId = TestDatabaseSetup.insertTestUser(testConnection, username, password, role);
        assertTrue(userId > 0, "Test user should be inserted");
        
        // Insert session
        assertTrue(userDAO.insertSession(sessionId, userId), "Session should be inserted successfully");
        
        // Act
        UserDAO.UserData userData = userDAO.findBySessionId(sessionId);
        
        // Assert
        assertNotNull(userData, "User should be found by session ID");
        assertEquals(userId, userData.id(), "User ID should match");
        assertEquals(username, userData.username(), "Username should match");
        assertEquals(role, userData.role(), "Role should match");
    }
    
    @Test
    @DisplayName("Should return null when finding non-existent session ID")
    void testFindBySessionId_NotFound() {
        // Arrange
        String nonExistentSessionId = "non-existent-session";
        
        // Act
        UserDAO.UserData userData = userDAO.findBySessionId(nonExistentSessionId);
        
        // Assert
        assertNull(userData, "Non-existent session should return null");
    }
    
    @Test
    @DisplayName("Should insert session successfully")
    void testInsertSession_Success() throws SQLException {
        // Arrange
        String username = TestDataFactory.getTestUsername();
        String password = TestDataFactory.getTestPassword();
        String role = "BIDDER";
        String sessionId = "test-session-456";
        
        // Insert test user
        int userId = TestDatabaseSetup.insertTestUser(testConnection, username, password, role);
        assertTrue(userId > 0, "Test user should be inserted");
        
        // Act
        boolean result = userDAO.insertSession(sessionId, userId);
        
        // Assert
        assertTrue(result, "Session should be inserted successfully");
        
        // Verify session was actually inserted
        UserDAO.UserData userData = userDAO.findBySessionId(sessionId);
        assertNotNull(userData, "User should be found by session ID");
        assertEquals(userId, userData.id(), "User ID should match");
    }
    
    @Test
    @DisplayName("Should return false when inserting session for non-existent user")
    void testInsertSession_NonExistentUser() {
        // Arrange
        String sessionId = "test-session-789";
        int nonExistentUserId = 99999;
        
        // Act
        boolean result = userDAO.insertSession(sessionId, nonExistentUserId);
        
        // Assert
        assertFalse(result, "Session insertion should fail for non-existent user");
    }
    
    @Test
    @DisplayName("Should delete session successfully")
    void testDeleteSession_Success() throws SQLException {
        // Arrange
        String username = TestDataFactory.getTestUsername();
        String password = TestDataFactory.getTestPassword();
        String role = "BIDDER";
        String sessionId = "test-session-to-delete";
        
        // Insert test user and session
        int userId = TestDatabaseSetup.insertTestUser(testConnection, username, password, role);
        assertTrue(userId > 0, "Test user should be inserted");
        assertTrue(userDAO.insertSession(sessionId, userId), "Session should be inserted successfully");
        
        // Verify session exists
        assertNotNull(userDAO.findBySessionId(sessionId), "Session should exist before deletion");
        
        // Act
        userDAO.deleteSession(sessionId);
        
        // Assert
        assertNull(userDAO.findBySessionId(sessionId), "Session should be deleted");
    }
    
    @Test
    @DisplayName("Should check if username exists successfully")
    void testExistsByUsername_Success() throws SQLException {
        // Arrange
        String username = TestDataFactory.getTestUsername();
        String password = TestDataFactory.getTestPassword();
        String role = "BIDDER";
        
        // Insert test user
        int userId = TestDatabaseSetup.insertTestUser(testConnection, username, password, role);
        assertTrue(userId > 0, "Test user should be inserted");
        
        // Act
        boolean exists = userDAO.existsByUsername(username);
        
        // Assert
        assertTrue(exists, "Username should exist");
    }
    
    @Test
    @DisplayName("Should return false for non-existent username")
    void testExistsByUsername_NotFound() {
        // Arrange
        String nonExistentUsername = "nonexistent-user-12345";
        
        // Act
        boolean exists = userDAO.existsByUsername(nonExistentUsername);
        
        // Assert
        assertFalse(exists, "Non-existent username should return false");
    }
    
    @Test
    @DisplayName("Should change user role successfully")
    void testChangeUserRole_Success() throws SQLException {
        // Arrange
        String username = TestDataFactory.getTestUsername();
        String password = TestDataFactory.getTestPassword();
        String originalRole = "BIDDER";
        String newRole = "SELLER";
        
        // Insert test user
        int userId = TestDatabaseSetup.insertTestUser(testConnection, username, password, originalRole);
        assertTrue(userId > 0, "Test user should be inserted");
        
        // Verify original role
        UserDAO.UserData userData = userDAO.findByUserId(userId);
        assertEquals(originalRole, userData.role(), "Original role should match");
        
        // Act
        boolean result = userDAO.changeUserRole(userId, newRole);
        
        // Assert
        assertTrue(result, "Role change should succeed");
        
        // Verify role was changed
        userData = userDAO.findByUserId(userId);
        assertEquals(newRole, userData.role(), "Role should be changed");
    }
    
    @Test
    @DisplayName("Should return false when changing role for non-existent user")
    void testChangeUserRole_NonExistentUser() {
        // Arrange
        int nonExistentUserId = 99999;
        String newRole = "ADMIN";
        
        // Act
        boolean result = userDAO.changeUserRole(nonExistentUserId, newRole);
        
        // Assert
        assertFalse(result, "Role change should fail for non-existent user");
    }
    
    @Test
    @DisplayName("Should handle multiple concurrent operations")
    void testConcurrentOperations() throws SQLException, InterruptedException {
        // Arrange
        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];
        boolean[] results = new boolean[numThreads];
        
        // Act - Create multiple threads inserting users concurrently
        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    String username = "concurrent_user_" + threadIndex;
                    String password = "password123";
                    String role = "BIDDER";
                    
                    int userId = userDAO.insertUser(username, password, role);
                    results[threadIndex] = userId > 0;
                } catch (Exception e) {
                    results[threadIndex] = false;
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Assert - All operations should succeed
        for (int i = 0; i < numThreads; i++) {
            assertTrue(results[i], "Concurrent operation " + i + " should succeed");
        }
    }
}
