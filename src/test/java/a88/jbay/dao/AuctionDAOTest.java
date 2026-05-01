package a88.jbay.dao;

import a88.jbay.model.entity.item.Item;
import a88.jbay.model.event.AuctionState;
import a88.jbay.testutil.TestDatabaseSetup;
import a88.jbay.testutil.TestDataFactory;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuctionDAOTest {
    
    private TestAuctionDAO auctionDAO;
    private Connection testConnection;
    
    private int testUserId;
    private int testItemId;
    
    @BeforeEach
    void setUp() throws SQLException {
        // Initialize test database
        TestDatabaseSetup.initializeTestDatabase();
        testConnection = TestDatabaseSetup.getTestConnection();
        
        
        // Get TestAuctionDAO instance
        auctionDAO = new TestAuctionDAO();
        
        // Insert test data
        testUserId = TestDatabaseSetup.insertTestUser(testConnection, "testuser", "password123", "SELLER");
        testItemId = TestDatabaseSetup.insertTestItem(testConnection, "Test Item", "Electronics", "Test Description", 100.0);
        
        assertTrue(testUserId > 0, "Test user should be inserted");
        assertTrue(testItemId > 0, "Test item should be inserted");
    }
    
    @AfterEach
    void tearDown() throws SQLException {
        // Clean up test database
        TestDatabaseSetup.cleanupTestDatabase();
        
        // Close test connection
        if (testConnection != null && !testConnection.isClosed()) {
            testConnection.close();
        }
        
    }
    
    @Test
    @DisplayName("Should insert auction successfully")
    void testInsertAuction_Success() throws SQLException {
        // Arrange
        double startPrice = 100.0;
        double curPrice = 100.0;
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(24);
        
        // Act - Create item for auction
        a88.jbay.model.entity.item.Item testItem = new a88.jbay.model.entity.item.Item(testItemId, "Test Item", "Electronics", "Test Description", startPrice, new byte[0]);
        int auctionId = auctionDAO.insertAuction(testItem, testUserId, startPrice, curPrice, startTime, endTime, "OPENING");
        
        // Assert
        assertTrue(auctionId > 0, "Auction ID should be positive");
        
        // Verify auction was actually inserted
        TestAuctionDAO.AuctionData auctionData = auctionDAO.findAuctionById(auctionId);
        assertNotNull(auctionData, "Auction should be found after insertion");
        assertEquals(auctionId, auctionData.id(), "Auction ID should match");
        assertEquals(testItemId, auctionData.item().getId(), "Item ID should match");
        assertEquals(testUserId, auctionData.sellerId(), "Seller ID should match");
        assertEquals(startPrice, auctionData.startPrice(), "Start price should match");
        assertEquals(curPrice, auctionData.curPrice(), "Current price should match");
        assertEquals("OPENING", auctionData.state(), "State should be OPENING");
    }
    
    @Test
    @DisplayName("Should return -1 when inserting auction with invalid item ID")
    void testInsertAuction_InvalidItemId() {
        // Arrange
        int invalidItemId = 99999;
        double startPrice = 100.0;
        double curPrice = 100.0;
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(24);
        
        // Act - Create item with invalid ID (this should fail in the DAO)
        a88.jbay.model.entity.item.Item invalidItem = new a88.jbay.model.entity.item.Item(invalidItemId, "Invalid Item", "Test", "Test", startPrice, new byte[0]);
        int auctionId = auctionDAO.insertAuction(invalidItem, testUserId, startPrice, curPrice, startTime, endTime, "OPENING");
        
        // Assert
        assertEquals(-1, auctionId, "Invalid item ID should return -1");
    }
    
    @Test
    @DisplayName("Should return -1 when inserting auction with invalid seller ID")
    void testInsertAuction_InvalidSellerId() {
        // Arrange
        int invalidSellerId = 99999;
        double startPrice = 100.0;
        double curPrice = 100.0;
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(24);
        
        // Act - Create item for auction with invalid seller
        a88.jbay.model.entity.item.Item testItem = new a88.jbay.model.entity.item.Item(testItemId, "Test Item", "Electronics", "Test Description", startPrice, new byte[0]);
        int auctionId = auctionDAO.insertAuction(testItem, invalidSellerId, startPrice, curPrice, startTime, endTime, "OPENING");
        
        // Assert
        assertEquals(-1, auctionId, "Invalid seller ID should return -1");
    }
    
    @Test
    @DisplayName("Should find auction by ID successfully")
    void testFindAuctionById_Success() throws SQLException {
        // Arrange
        double startPrice = 100.0;
        double curPrice = 100.0;
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(24);
        
        // Insert test auction
        int auctionId = TestDatabaseSetup.insertTestAuction(testConnection, testItemId, testUserId, 
                                                           startPrice, curPrice, startTime, endTime, "OPENING");
        assertTrue(auctionId > 0, "Test auction should be inserted");
        
        // Act
        TestAuctionDAO.AuctionData auctionData = auctionDAO.findAuctionById(auctionId);
        
        // Assert
        assertNotNull(auctionData, "Auction should be found");
        assertEquals(auctionId, auctionData.id(), "Auction ID should match");
        assertEquals(testItemId, auctionData.item().getId(), "Item ID should match");
        assertEquals(testUserId, auctionData.sellerId(), "Seller ID should match");
        assertEquals(startPrice, auctionData.startPrice(), "Start price should match");
        assertEquals(curPrice, auctionData.curPrice(), "Current price should match");
        assertEquals("OPENING", auctionData.state(), "State should match");
    }
    
    @Test
    @DisplayName("Should return null when finding non-existent auction ID")
    void testFindAuctionById_NotFound() {
        // Arrange
        int nonExistentAuctionId = 99999;
        
        // Act
        TestAuctionDAO.AuctionData auctionData = auctionDAO.findAuctionById(nonExistentAuctionId);
        
        // Assert
        assertNull(auctionData, "Non-existent auction should return null");
    }
    
    @Test
    @DisplayName("Should find auctions by seller ID successfully")
    void testFindAuctionsBySellerId_Success() throws SQLException {
        // Arrange
        double startPrice = 100.0;
        double curPrice = 100.0;
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(24);
        
        // Insert multiple test auctions for the same seller
        int auctionId1 = TestDatabaseSetup.insertTestAuction(testConnection, testItemId, testUserId, 
                                                            startPrice, curPrice, startTime, endTime, "OPENING");
        int auctionId2 = TestDatabaseSetup.insertTestAuction(testConnection, testItemId, testUserId, 
                                                            startPrice, curPrice, startTime.plusHours(1), endTime.plusHours(1), "RUNNING");
        
        assertTrue(auctionId1 > 0, "First auction should be inserted");
        assertTrue(auctionId2 > 0, "Second auction should be inserted");
        
        // Act
        List<TestAuctionDAO.AuctionData> auctions = auctionDAO.findAuctionsBySellerId(testUserId);
        
        // Assert
        assertNotNull(auctions, "Auctions list should not be null");
        assertEquals(2, auctions.size(), "Should find 2 auctions for seller");
        
        // Verify both auctions are found
        boolean foundAuction1 = auctions.stream().anyMatch(a -> a.id() == auctionId1);
        boolean foundAuction2 = auctions.stream().anyMatch(a -> a.id() == auctionId2);
        
        assertTrue(foundAuction1, "First auction should be found");
        assertTrue(foundAuction2, "Second auction should be found");
    }
    
    @Test
    @DisplayName("Should return empty list when seller has no auctions")
    void testFindAuctionsBySellerId_NoAuctions() {
        // Arrange
        int sellerWithNoAuctions = 99999;
        
        // Act
        List<TestAuctionDAO.AuctionData> auctions = auctionDAO.findAuctionsBySellerId(sellerWithNoAuctions);
        
        // Assert
        assertNotNull(auctions, "Auctions list should not be null");
        assertTrue(auctions.isEmpty(), "Should return empty list for seller with no auctions");
    }
    
    @Test
    @DisplayName("Should find auctions by winner ID successfully")
    void testFindAuctionsByWinnerId_Success() throws SQLException {
        // Arrange
        int winnerId = TestDatabaseSetup.insertTestUser(testConnection, "winner", "password123", "BIDDER");
        assertTrue(winnerId > 0, "Winner user should be inserted");
        
        double startPrice = 100.0;
        double curPrice = 150.0;
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(24);
        
        // Insert test auction with winner
        int auctionId1 = TestDatabaseSetup.insertTestAuction(testConnection, testItemId, testUserId, 
                                                            startPrice, curPrice, startTime, endTime, "FINISHED");
        
        // Update auction to have winner (need to manually update since test utility doesn't set winner)
        try (java.sql.PreparedStatement stmt = testConnection.prepareStatement(
                "UPDATE auctions SET winner = ? WHERE id = ?")) {
            stmt.setInt(1, winnerId);
            stmt.setInt(2, auctionId1);
            stmt.executeUpdate();
        }
        
        assertTrue(auctionId1 > 0, "Test auction should be inserted");
        
        // Act
        List<TestAuctionDAO.AuctionData> auctions = auctionDAO.findAuctionsByWinnerId(winnerId);
        
        // Assert
        assertNotNull(auctions, "Auctions list should not be null");
        assertEquals(1, auctions.size(), "Should find 1 auction for winner");
        
        TestAuctionDAO.AuctionData foundAuction = auctions.get(0);
        assertEquals(auctionId1, foundAuction.id(), "Auction ID should match");
        assertEquals(winnerId, foundAuction.winnerId(), "Winner ID should match");
    }
    
    @Test
    @DisplayName("Should return empty list when winner has no won auctions")
    void testFindAuctionsByWinnerId_NoAuctions() {
        // Arrange
        int winnerWithNoAuctions = 99999;
        
        // Act
        List<TestAuctionDAO.AuctionData> auctions = auctionDAO.findAuctionsByWinnerId(winnerWithNoAuctions);
        
        // Assert
        assertNotNull(auctions, "Auctions list should not be null");
        assertTrue(auctions.isEmpty(), "Should return empty list for winner with no auctions");
    }
    
    @Test
    @DisplayName("Should find all active auctions successfully")
    void testFindAllActiveAuctions_Success() throws SQLException {
        // Arrange
        double startPrice = 100.0;
        double curPrice = 100.0;
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(24);
        
        // Insert active auctions (OPENING and RUNNING states)
        int auctionId1 = TestDatabaseSetup.insertTestAuction(testConnection, testItemId, testUserId, 
                                                            startPrice, curPrice, startTime, endTime, "OPENING");
        int auctionId2 = TestDatabaseSetup.insertTestAuction(testConnection, testItemId, testUserId, 
                                                            startPrice, curPrice, startTime.plusHours(1), endTime.plusHours(1), "RUNNING");
        
        // Insert finished auction (should not be included)
        int auctionId3 = TestDatabaseSetup.insertTestAuction(testConnection, testItemId, testUserId, 
                                                            startPrice, curPrice, startTime.plusHours(2), endTime.plusHours(2), "FINISHED");
        
        assertTrue(auctionId1 > 0, "First auction should be inserted");
        assertTrue(auctionId2 > 0, "Second auction should be inserted");
        assertTrue(auctionId3 > 0, "Third auction should be inserted");
        
        // Act
        List<TestAuctionDAO.AuctionData> activeAuctions = auctionDAO.findAllActiveAuctions();
        
        // Assert
        assertNotNull(activeAuctions, "Active auctions list should not be null");
        assertEquals(2, activeAuctions.size(), "Should find 2 active auctions");
        
        // Verify only active auctions are found
        boolean foundAuction1 = activeAuctions.stream().anyMatch(a -> a.id() == auctionId1);
        boolean foundAuction2 = activeAuctions.stream().anyMatch(a -> a.id() == auctionId2);
        boolean foundAuction3 = activeAuctions.stream().anyMatch(a -> a.id() == auctionId3);
        
        assertTrue(foundAuction1, "OPENING auction should be found");
        assertTrue(foundAuction2, "RUNNING auction should be found");
        assertFalse(foundAuction3, "FINISHED auction should not be found");
    }
    
    @Test
    @DisplayName("Should return empty list when no active auctions exist")
    void testFindAllActiveAuctions_NoActiveAuctions() throws SQLException {
        // Arrange - Insert only finished auctions
        double startPrice = 100.0;
        double curPrice = 100.0;
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(24);
        
        TestDatabaseSetup.insertTestAuction(testConnection, testItemId, testUserId, 
                                          startPrice, curPrice, startTime, endTime, "FINISHED");
        TestDatabaseSetup.insertTestAuction(testConnection, testItemId, testUserId, 
                                          startPrice, curPrice, startTime.plusHours(1), endTime.plusHours(1), "CANCELED");
        
        // Act
        List<TestAuctionDAO.AuctionData> activeAuctions = auctionDAO.findAllActiveAuctions();
        
        // Assert
        assertNotNull(activeAuctions, "Active auctions list should not be null");
        assertTrue(activeAuctions.isEmpty(), "Should return empty list when no active auctions");
    }
    
    @Test
    @DisplayName("Should update auction state successfully")
    void testSetAuctionState_Success() throws SQLException {
        // Arrange
        double startPrice = 100.0;
        double curPrice = 100.0;
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(24);
        
        // Insert test auction
        int auctionId = TestDatabaseSetup.insertTestAuction(testConnection, testItemId, testUserId, 
                                                           startPrice, curPrice, startTime, endTime, "OPENING");
        assertTrue(auctionId > 0, "Test auction should be inserted");
        
        // Verify initial state
        TestAuctionDAO.AuctionData auctionData = auctionDAO.findAuctionById(auctionId);
        assertEquals("OPENING", auctionData.state(), "Initial state should be OPENING");
        
        // Act
        boolean result = auctionDAO.setAuctionState(auctionId, "RUNNING");
        
        // Assert
        assertTrue(result, "State update should succeed");
        
        // Verify state was changed
        auctionData = auctionDAO.findAuctionById(auctionId);
        assertEquals("RUNNING", auctionData.state(), "State should be changed to RUNNING");
    }
    
    @Test
    @DisplayName("Should return false when updating state for non-existent auction")
    void testSetAuctionState_NonExistentAuction() {
        // Arrange
        int nonExistentAuctionId = 99999;
        
        // Act
        boolean result = auctionDAO.setAuctionState(nonExistentAuctionId, "RUNNING");
        
        // Assert
        assertFalse(result, "State update should fail for non-existent auction");
    }
    
    @Test
    @DisplayName("Should update current price successfully")
    void testUpdateCurrentPrice_Success() throws SQLException {
        // Arrange
        double startPrice = 100.0;
        double curPrice = 100.0;
        double newPrice = 150.0;
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(24);
        
        // Insert test auction
        int auctionId = TestDatabaseSetup.insertTestAuction(testConnection, testItemId, testUserId, 
                                                           startPrice, curPrice, startTime, endTime, "RUNNING");
        assertTrue(auctionId > 0, "Test auction should be inserted");
        
        // Verify initial price
        TestAuctionDAO.AuctionData auctionData = auctionDAO.findAuctionById(auctionId);
        assertEquals(curPrice, auctionData.curPrice(), "Initial price should match");
        
        // Act
        boolean result = auctionDAO.updateCurrentPrice(auctionId, newPrice, testUserId);
        
        // Assert
        assertTrue(result, "Price update should succeed");
        
        // Verify price was changed
        auctionData = auctionDAO.findAuctionById(auctionId);
        assertEquals(newPrice, auctionData.curPrice(), "Price should be updated");
        assertEquals(testUserId, auctionData.winnerId(), "Winner should be set");
    }
    
    @Test
    @DisplayName("Should return false when updating price for non-existent auction")
    void testUpdateCurrentPrice_NonExistentAuction() {
        // Arrange
        int nonExistentAuctionId = 99999;
        double newPrice = 150.0;
        
        // Act
        boolean result = auctionDAO.updateCurrentPrice(nonExistentAuctionId, newPrice, testUserId);
        
        // Assert
        assertFalse(result, "Price update should fail for non-existent auction");
    }
    
    @Test
    @DisplayName("Should update end time successfully")
    void testUpdateEndTime_Success() throws SQLException {
        // Arrange
        double startPrice = 100.0;
        double curPrice = 100.0;
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(24);
        LocalDateTime newEndTime = startTime.plusHours(48);
        
        // Insert test auction
        int auctionId = TestDatabaseSetup.insertTestAuction(testConnection, testItemId, testUserId, 
                                                           startPrice, curPrice, startTime, endTime, "RUNNING");
        assertTrue(auctionId > 0, "Test auction should be inserted");
        
        // Act
        boolean result = auctionDAO.updateEndTime(auctionId, newEndTime);
        
        // Assert
        assertTrue(result, "End time update should succeed");
        
        // Verify end time was changed
        TestAuctionDAO.AuctionData auctionData = auctionDAO.findAuctionById(auctionId);
        // Compare timestamps without nanoseconds due to H2 precision limitations
        assertEquals(newEndTime.truncatedTo(java.time.temporal.ChronoUnit.MILLIS), 
                    auctionData.endTime().truncatedTo(java.time.temporal.ChronoUnit.MILLIS), 
                    "End time should be updated (ignoring nanoseconds)");
    }
    
    @Test
    @DisplayName("Should return false when updating end time for non-existent auction")
    void testUpdateEndTime_NonExistentAuction() {
        // Arrange
        int nonExistentAuctionId = 99999;
        LocalDateTime newEndTime = LocalDateTime.now().plusHours(48);
        
        // Act
        boolean result = auctionDAO.updateEndTime(nonExistentAuctionId, newEndTime);
        
        // Assert
        assertFalse(result, "End time update should fail for non-existent auction");
    }
    
    @Test
    @DisplayName("Should finalize auction successfully")
    void testFinalizeAuction_Success() throws SQLException {
        // Arrange
        double startPrice = 100.0;
        double curPrice = 150.0;
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(24);
        
        // Insert test auction
        int auctionId = TestDatabaseSetup.insertTestAuction(testConnection, testItemId, testUserId, 
                                                           startPrice, curPrice, startTime, endTime, "RUNNING");
        assertTrue(auctionId > 0, "Test auction should be inserted");
        
        // Act
        boolean result = auctionDAO.finalizeAuction(auctionId, testUserId);
        
        // Assert
        assertTrue(result, "Auction finalization should succeed");
        
        // Verify auction was finalized
        TestAuctionDAO.AuctionData auctionData = auctionDAO.findAuctionById(auctionId);
        assertEquals("FINISHED", auctionData.state(), "State should be FINISHED");
        assertEquals(testUserId, auctionData.winnerId(), "Winner should be set");
    }
    
    @Test
    @DisplayName("Should finalize auction without winner successfully")
    void testFinalizeAuction_NoWinner_Success() throws SQLException {
        // Arrange
        double startPrice = 100.0;
        double curPrice = 100.0;
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(24);
        
        // Insert test auction
        int auctionId = TestDatabaseSetup.insertTestAuction(testConnection, testItemId, testUserId, 
                                                           startPrice, curPrice, startTime, endTime, "RUNNING");
        assertTrue(auctionId > 0, "Test auction should be inserted");
        
        // Act
        boolean result = auctionDAO.finalizeAuction(auctionId, null);
        
        // Assert
        assertTrue(result, "Auction finalization should succeed");
        
        // Verify auction was finalized
        TestAuctionDAO.AuctionData auctionData = auctionDAO.findAuctionById(auctionId);
        assertEquals("FINISHED", auctionData.state(), "State should be FINISHED");
        assertNull(auctionData.winnerId(), "Winner should be null");
    }
    
    @Test
    @DisplayName("Should return false when finalizing non-existent auction")
    void testFinalizeAuction_NonExistentAuction() {
        // Arrange
        int nonExistentAuctionId = 99999;
        
        // Act
        boolean result = auctionDAO.finalizeAuction(nonExistentAuctionId, testUserId);
        
        // Assert
        assertFalse(result, "Auction finalization should fail for non-existent auction");
    }
    
    @Test
    @DisplayName("Should find seller ID successfully")
    void testFindSellerId_Success() throws SQLException {
        // Arrange
        double startPrice = 100.0;
        double curPrice = 100.0;
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(24);
        
        // Insert test auction
        int auctionId = TestDatabaseSetup.insertTestAuction(testConnection, testItemId, testUserId, 
                                                           startPrice, curPrice, startTime, endTime, "RUNNING");
        assertTrue(auctionId > 0, "Test auction should be inserted");
        
        // Act
        Integer sellerId = auctionDAO.findSellerId(auctionId);
        
        // Assert
        assertNotNull(sellerId, "Seller ID should not be null");
        assertEquals(testUserId, sellerId, "Seller ID should match");
    }
    
    @Test
    @DisplayName("Should return null when finding seller ID for non-existent auction")
    void testFindSellerId_NotFound() {
        // Arrange
        int nonExistentAuctionId = 99999;
        
        // Act
        Integer sellerId = auctionDAO.findSellerId(nonExistentAuctionId);
        
        // Assert
        assertNull(sellerId, "Seller ID should be null for non-existent auction");
    }
}
