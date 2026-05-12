package a88.jbay.dao;

import a88.jbay.common.item.Item;
import a88.jbay.testutil.TestDatabaseConnectionProvider;

import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Manual mock test for ItemDAO using Java 25 compatible approach
 * This replaces Mockito with manual test doubles
 */
class ItemDAOMockTest {
    
    private TestDatabaseConnectionProvider testDbProvider;
    private ItemDAO itemDAO;
    
    @BeforeEach
    void setUp() {
        testDbProvider = new TestDatabaseConnectionProvider();
        itemDAO = new ItemDAO(testDbProvider);
    }
    
    @Test
    @DisplayName("Should handle database exception in insertItem")
    void testInsertItem_DatabaseError() throws SQLException {
        // Arrange
        testDbProvider.setShouldThrowException(true);
        Item item = new Item(0, "Test Item", "Electronics", "Test Description", 100.0, new byte[0]);
        
        // Act
        int result = itemDAO.insertItem(item);
        
        // Assert
        assertEquals(-1, result, "Database error should return -1");
    }
    
    @Test
    @DisplayName("Should handle database exception in findItemById")
    void testFindItemById_DatabaseError() throws SQLException {
        // Arrange
        testDbProvider.setShouldThrowException(true);
        
        // Act
        Item result = itemDAO.findItemById(1);
        
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
        assertFalse(testDbProvider.shouldThrowException(), "Should reset exception flag");
        assertNull(testDbProvider.getMockData("test"), "Should clear mock data");
    }
}
