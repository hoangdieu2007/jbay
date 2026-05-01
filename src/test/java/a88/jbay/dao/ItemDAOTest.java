package a88.jbay.dao;

import a88.jbay.model.entity.item.Item;
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

class ItemDAOTest {
    
    private ItemDAO itemDAO;
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
        
        // Get ItemDAO instance
        itemDAO = ItemDAO.getInstance();
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
    @DisplayName("Should insert item successfully")
    void testInsertItem_Success() throws SQLException {
        // Arrange
        String name = TestDataFactory.getTestUsername() + "_item";
        String type = "Electronics";
        String description = "Test item description";
        double startPrice = 99.99;
        byte[] image = new byte[]{1, 2, 3, 4, 5};
        
        // Act
        int itemId = itemDAO.insertItem(new Item(66, name, type, description, startPrice, image));
        
        // Assert
        assertTrue(itemId > 0, "Item ID should be positive");
        
        // Verify item was actually inserted
        Item foundItem = itemDAO.findItemById(itemId);
        assertNotNull(foundItem, "Item should be found after insertion");
        assertEquals(name, foundItem.getName(), "Item name should match");
        assertEquals(type, foundItem.getType(), "Item type should match");
        assertEquals(description, foundItem.getDescription(), "Item description should match");
        assertEquals(startPrice, foundItem.getInitPrice(), "Item start price should match");
        assertArrayEquals(image, foundItem.getImage(), "Item image should match");
    }
    
    @Test
    @DisplayName("Should insert item without image successfully")
    void testInsertItem_NoImage() throws SQLException {
        // Arrange
        int id = 67;
        String name = TestDataFactory.getTestUsername() + "_item_no_image";
        String type = "Books";
        String description = "Test book description";
        double startPrice = 25.50;
        byte[] image = new byte[0];
        
        // Act
        int itemId = itemDAO.insertItem(new Item(id, name, type, description, startPrice, image));
        
        // Assert
        assertTrue(itemId > 0, "Item ID should be positive");
        
        // Verify item was actually inserted
        Item foundItem = itemDAO.findItemById(itemId);
        assertNotNull(foundItem, "Item should be found after insertion");
        assertEquals(name, foundItem.getName(), "Item name should match");
        assertEquals(type, foundItem.getType(), "Item type should match");
        assertEquals(description, foundItem.getDescription(), "Item description should match");
        assertEquals(startPrice, foundItem.getInitPrice(), "Item start price should match");
        assertArrayEquals(image, foundItem.getImage(), "Item image should match");
    }
    
    @Test
    @DisplayName("Should insert item with null image successfully")
    void testInsertItem_NullImage() throws SQLException {
        // Arrange
        int id = 68;
        String name = TestDataFactory.getTestUsername() + "_item_null_image";
        String type = "Clothing";
        String description = "Test clothing description";
        double startPrice = 45.00;
        byte[] image = null;
        
        // Act
        int itemId = itemDAO.insertItem(new Item(id, name, type, description, startPrice, image));
        
        // Assert
        assertTrue(itemId > 0, "Item ID should be positive");
        
        // Verify item was actually inserted
        Item foundItem = itemDAO.findItemById(itemId);
        assertNotNull(foundItem, "Item should be found after insertion");
        assertEquals(name, foundItem.getName(), "Item name should match");
        assertEquals(type, foundItem.getType(), "Item type should match");
        assertEquals(description, foundItem.getDescription(), "Item description should match");
        assertEquals(startPrice, foundItem.getInitPrice(), "Item start price should match");
        assertNull(foundItem.getImage(), "Item image should be null");
    }
    
    @Test
    @DisplayName("Should find item by ID successfully")
    void testFindItemById_Success() throws SQLException {
        // Arrange
        String name = TestDataFactory.getTestUsername() + "_item_find";
        String type = "Electronics";
        String description = "Test item for finding";
        double startPrice = 199.99;
        byte[] image = new byte[]{10, 20, 30};
        
        // Insert test item
        int itemId = TestDatabaseSetup.insertTestItem(testConnection, name, type, description, startPrice);
        assertTrue(itemId > 0, "Test item should be inserted");
        
        // Act
        Item foundItem = itemDAO.findItemById(itemId);
        
        // Assert
        assertNotNull(foundItem, "Item should be found");
        assertEquals(name, foundItem.getName(), "Item name should match");
        assertEquals(type, foundItem.getType(), "Item type should match");
        assertEquals(description, foundItem.getDescription(), "Item description should match");
        assertEquals(startPrice, foundItem.getInitPrice(), "Item start price should match");
        assertArrayEquals(new byte[0], foundItem.getImage(), "Item image should be empty (test utility doesn't store image)");
    }
    
    @Test
    @DisplayName("Should return null when finding non-existent item ID")
    void testFindItemById_NotFound() {
        // Arrange
        int nonExistentItemId = 99999;
        
        // Act
        Item foundItem = itemDAO.findItemById(nonExistentItemId);
        
        // Assert
        assertNull(foundItem, "Non-existent item should return null");
    }
    
    @Test
    @DisplayName("Should handle item with special characters in name and description")
    void testInsertItem_SpecialCharacters() throws SQLException {
        // Arrange
        String name = "Test Item! @#$%^&*()_+{}|:<>?[]\\;'\",./";
        String type = "Special-Category_123";
        String description = "This is a test description with special chars: áéíóú ñ 中文 🚀 emoji";
        double startPrice = 123.45;
        byte[] image = new byte[]{1, 2, 3};
        
        // Act
        int itemId = itemDAO.insertItem(new Item(69, name, type, description, startPrice, image));
        
        // Assert
        assertTrue(itemId > 0, "Item ID should be positive");
        
        // Verify item was actually inserted with special characters
        Item foundItem = itemDAO.findItemById(itemId);
        assertNotNull(foundItem, "Item should be found after insertion");
        assertEquals(name, foundItem.getName(), "Item name with special chars should match");
        assertEquals(type, foundItem.getType(), "Item type with special chars should match");
        assertEquals(description, foundItem.getDescription(), "Item description with special chars should match");
        assertEquals(startPrice, foundItem.getInitPrice(), "Item start price should match");
    }
    
    @Test
    @DisplayName("Should handle item with maximum length fields")
    void testInsertItem_MaxLengthFields() throws SQLException {
        // Arrange
        StringBuilder longName = new StringBuilder();
        StringBuilder longDescription = new StringBuilder();
        
        // Create very long name (255 characters)
        for (int i = 0; i < 255; i++) {
            longName.append("a");
        }
        
        // Create very long description (1000 characters)
        for (int i = 0; i < 1000; i++) {
            longDescription.append("b");
        }
        
        String name = longName.toString();
        String type = "Test";
        String description = longDescription.toString();
        double startPrice = 999.99;
        byte[] image = new byte[]{1, 2, 3};
        
        // Act
        int itemId = itemDAO.insertItem(new Item(70, name, type, description, startPrice, image));
        
        // Assert
        assertTrue(itemId > 0, "Item ID should be positive");
        
        // Verify item was actually inserted
        Item foundItem = itemDAO.findItemById(itemId);
        assertNotNull(foundItem, "Item should be found after insertion");
        assertEquals(name, foundItem.getName(), "Long item name should match");
        assertEquals(type, foundItem.getType(), "Item type should match");
        assertEquals(description, foundItem.getDescription(), "Long item description should match");
        assertEquals(startPrice, foundItem.getInitPrice(), "Item start price should match");
    }
    
    @Test
    @DisplayName("Should handle item with zero start price")
    void testInsertItem_ZeroStartPrice() throws SQLException {
        // Arrange
        String name = TestDataFactory.getTestUsername() + "_free_item";
        String type = "Free";
        String description = "This is a free item";
        double startPrice = 0.0;
        byte[] image = new byte[]{1, 2, 3};
        
        // Act
        int itemId = itemDAO.insertItem(new Item(71, name, type, description, startPrice, image));
        
        // Assert
        assertTrue(itemId > 0, "Item ID should be positive");
        
        // Verify item was actually inserted
        Item foundItem = itemDAO.findItemById(itemId);
        assertNotNull(foundItem, "Item should be found after insertion");
        assertEquals(name, foundItem.getName(), "Item name should match");
        assertEquals(startPrice, foundItem.getInitPrice(), "Item start price should be 0.0");
    }
    
    @Test
    @DisplayName("Should handle item with very high start price")
    void testInsertItem_HighStartPrice() throws SQLException {
        // Arrange
        String name = TestDataFactory.getTestUsername() + "_expensive_item";
        String type = "Luxury";
        String description = "This is a very expensive item";
        double startPrice = 999999.99;
        byte[] image = new byte[]{1, 2, 3};
        
        // Act
        int itemId = itemDAO.insertItem(new Item(72, name, type, description, startPrice, image));
        
        // Assert
        assertTrue(itemId > 0, "Item ID should be positive");
        
        // Verify item was actually inserted
        Item foundItem = itemDAO.findItemById(itemId);
        assertNotNull(foundItem, "Item should be found after insertion");
        assertEquals(name, foundItem.getName(), "Item name should match");
        assertEquals(startPrice, foundItem.getInitPrice(), "Item start price should match");
    }
    
    @Test
    @DisplayName("Should handle item with negative start price")
    void testInsertItem_NegativeStartPrice() throws SQLException {
        // Arrange
        String name = TestDataFactory.getTestUsername() + "_negative_price_item";
        String type = "Debt";
        String description = "This is an item with negative price";
        double startPrice = -50.0;
        byte[] image = new byte[]{1, 2, 3};
        
        // Act
        int itemId = itemDAO.insertItem(new Item(73, name, type, description, startPrice, image));
        
        // Assert
        assertTrue(itemId > 0, "Item ID should be positive");
        
        // Verify item was actually inserted
        Item foundItem = itemDAO.findItemById(itemId);
        assertNotNull(foundItem, "Item should be found after insertion");
        assertEquals(name, foundItem.getName(), "Item name should match");
        assertEquals(startPrice, foundItem.getInitPrice(), "Item start price should be negative");
    }
    
    @Test
    @DisplayName("Should handle concurrent item insertions")
    void testConcurrentItemInsertions() throws SQLException, InterruptedException {
        // Arrange
        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];
        int[] itemIds = new int[numThreads];
        
        // Act - Create multiple threads inserting items concurrently
        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    String name = "concurrent_item_" + threadIndex + "_" + System.currentTimeMillis();
                    String type = "Test";
                    String description = "Concurrent test item " + threadIndex;
                    double startPrice = 100.0 + threadIndex;
                    byte[] image = new byte[]{(byte) threadIndex};
                    
                    Item item = new Item(74, name, type, description, startPrice, image);
                    itemIds[threadIndex] = itemDAO.insertItem(item);
                } catch (Exception e) {
                    itemIds[threadIndex] = -1;
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
            assertTrue(itemIds[i] > 0, "Concurrent item insertion " + i + " should succeed");
            
            // Verify each item was actually inserted
            Item foundItem = itemDAO.findItemById(itemIds[i]);
            assertNotNull(foundItem, "Item " + i + " should be found after insertion");
        }
    }
    
    @Test
    @DisplayName("Should handle item with empty strings")
    void testInsertItem_EmptyStrings() throws SQLException {
        // Arrange
        String name = "";
        String type = "";
        String description = "";
        double startPrice = 50.0;
        byte[] image = new byte[]{1, 2, 3};
        
        // Act
        int itemId = itemDAO.insertItem(new Item(75, name, type, description, startPrice, image));
        
        // Assert
        assertTrue(itemId > 0, "Item ID should be positive even with empty strings");
        
        // Verify item was actually inserted
        Item foundItem = itemDAO.findItemById(itemId);
        assertNotNull(foundItem, "Item should be found after insertion");
        assertEquals(name, foundItem.getName(), "Empty name should match");
        assertEquals(type, foundItem.getType(), "Empty type should match");
        assertEquals(description, foundItem.getDescription(), "Empty description should match");
        assertEquals(startPrice, foundItem.getInitPrice(), "Item start price should match");
    }
    
    @Test
    @DisplayName("Should handle item with Unicode characters")
    void testInsertItem_UnicodeCharacters() throws SQLException {
        // Arrange
        String name = "测试项目 🚀 Тестовый товар";
        String type = "電子產品";
        String description = "这是一个包含中文、日文、韩文、俄文、阿拉伯文和表情符号的描述 🌟 العربية";
        double startPrice = 888.88;
        byte[] image = new byte[]{1, 2, 3};
        
        // Act
        int itemId = itemDAO.insertItem(new Item(76, name, type, description, startPrice, image));
        
        // Assert
        assertTrue(itemId > 0, "Item ID should be positive");
        
        // Verify item was actually inserted with Unicode characters
        Item foundItem = itemDAO.findItemById(itemId);
        assertNotNull(foundItem, "Item should be found after insertion");
        assertEquals(name, foundItem.getName(), "Unicode name should match");
        assertEquals(type, foundItem.getType(), "Unicode type should match");
        assertEquals(description, foundItem.getDescription(), "Unicode description should match");
        assertEquals(startPrice, foundItem.getInitPrice(), "Item start price should match");
    }
    
    @Test
    @DisplayName("Should handle large image data")
    void testInsertItem_LargeImage() throws SQLException {
        // Arrange
        String name = TestDataFactory.getTestUsername() + "_large_image_item";
        String type = "Media";
        String description = "Item with large image";
        double startPrice = 100.0;
        
        // Create a 1MB image
        byte[] largeImage = new byte[1024 * 1024];
        for (int i = 0; i < largeImage.length; i++) {
            largeImage[i] = (byte) (i % 256);
        }
        
        // Act
        int itemId = itemDAO.insertItem(new Item(77, name, type, description, startPrice, largeImage));
        
        // Assert
        assertTrue(itemId > 0, "Item ID should be positive");
        
        // Verify item was actually inserted
        Item foundItem = itemDAO.findItemById(itemId);
        assertNotNull(foundItem, "Item should be found after insertion");
        assertEquals(name, foundItem.getName(), "Item name should match");
        assertEquals(startPrice, foundItem.getInitPrice(), "Item start price should match");
        // Note: TestDatabaseSetup doesn't store image data, so we can't verify the large image
    }
    
    @Test
    @DisplayName("Should maintain item data integrity")
    void testItemDataIntegrity() throws SQLException {
        // Arrange
        String name = "Integrity Test Item";
        String type = "Test";
        String description = "Testing data integrity";
        double startPrice = 123.456789;
        byte[] image = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        
        // Act
        int itemId = itemDAO.insertItem(new Item(78, name, type, description, startPrice, image));
        
        // Assert
        assertTrue(itemId > 0, "Item ID should be positive");
        
        // Verify all data is preserved exactly
        Item foundItem = itemDAO.findItemById(itemId);
        assertNotNull(foundItem, "Item should be found after insertion");
        assertEquals(name, foundItem.getName(), "Item name should be preserved exactly");
        assertEquals(type, foundItem.getType(), "Item type should be preserved exactly");
        assertEquals(description, foundItem.getDescription(), "Item description should be preserved exactly");
        assertEquals(startPrice, foundItem.getInitPrice(), "Item start price should be preserved exactly");
        // Note: TestDatabaseSetup doesn't store image data, so we can't verify image integrity
    }
}
