package a88.jbay.dao;

import a88.jbay.testutil.TestDatabaseSetup;
import a88.jbay.testutil.TestDataFactory;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BidDAOTest {
    
    private TestBidDAO bidDAO;
    private Connection testConnection;
    
    private int testUserId;
    private int testSellerId;
    private int testItemId;
    private int testAuctionId;
    
    @BeforeEach
    void setUp() throws SQLException {
        // Initialize test database
        TestDatabaseSetup.initializeTestDatabase();
        testConnection = TestDatabaseSetup.getTestConnection();
        
        
        // Get TestBidDAO instance
        bidDAO = new TestBidDAO();
        
        // Insert test data
        testUserId = TestDatabaseSetup.insertTestUser(testConnection, "bidder", "password123", "BIDDER");
        testSellerId = TestDatabaseSetup.insertTestUser(testConnection, "seller", "password123", "SELLER");
        testItemId = TestDatabaseSetup.insertTestItem(testConnection, "Test Item", "Electronics", "Test Description", 100.0);
        
        assertTrue(testUserId > 0, "Test bidder should be inserted");
        assertTrue(testSellerId > 0, "Test seller should be inserted");
        assertTrue(testItemId > 0, "Test item should be inserted");
        
        // Insert test auction
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(24);
        testAuctionId = TestDatabaseSetup.insertTestAuction(testConnection, testItemId, testSellerId, 
                                                           100.0, 100.0, startTime, endTime, "RUNNING");
        assertTrue(testAuctionId > 0, "Test auction should be inserted");
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
    @DisplayName("Should insert bid successfully")
    void testInsertBid_Success() throws SQLException {
        // Arrange
        double bidAmount = 150.0;
        LocalDateTime bidTime = LocalDateTime.now();
        
        // Act
        boolean result = bidDAO.insertBid(testUserId, testAuctionId, bidAmount, bidTime);
        
        // Assert
        assertTrue(result, "Bid insertion should succeed");
        
        // Verify bid was actually inserted
        List<TestBidDAO.BidData> bidHistory = bidDAO.findBidHistoryByAuctionId(testAuctionId);
        assertNotNull(bidHistory, "Bid history should not be null");
        assertEquals(1, bidHistory.size(), "Should find 1 bid");
        
        TestBidDAO.BidData bid = bidHistory.get(0);
        assertEquals(testUserId, bid.userId(), "User ID should match");
        assertEquals(testAuctionId, bid.auctionId(), "Auction ID should match");
        assertEquals(bidAmount, bid.amount(), "Bid amount should match");
        // Compare timestamps without nanoseconds due to H2 precision limitations
        LocalDateTime actualTime = bid.time();
        assertEquals(bidTime.truncatedTo(java.time.temporal.ChronoUnit.MILLIS), 
                    actualTime.truncatedTo(java.time.temporal.ChronoUnit.MILLIS), 
                    "Bid time should match (ignoring nanoseconds)");
    }
    
    @Test
    @DisplayName("Should return false when inserting bid for non-existent user")
    void testInsertBid_NonExistentUser() {
        // Arrange
        int nonExistentUserId = 99999;
        double bidAmount = 150.0;
        LocalDateTime bidTime = LocalDateTime.now();
        
        // Act
        boolean result = bidDAO.insertBid(nonExistentUserId, testAuctionId, bidAmount, bidTime);
        
        // Assert
        assertFalse(result, "Bid insertion should fail for non-existent user");
    }
    
    @Test
    @DisplayName("Should return false when inserting bid for non-existent auction")
    void testInsertBid_NonExistentAuction() {
        // Arrange
        int nonExistentAuctionId = 99999;
        double bidAmount = 150.0;
        LocalDateTime bidTime = LocalDateTime.now();
        
        // Act
        boolean result = bidDAO.insertBid(testUserId, nonExistentAuctionId, bidAmount, bidTime);
        
        // Assert
        assertFalse(result, "Bid insertion should fail for non-existent auction");
    }
    
    @Test
    @DisplayName("Should find bid history by auction ID successfully")
    void testFindBidHistoryByAuctionId_Success() throws SQLException {
        // Arrange
        LocalDateTime bidTime1 = LocalDateTime.now().minusMinutes(30);
        LocalDateTime bidTime2 = LocalDateTime.now().minusMinutes(15);
        LocalDateTime bidTime3 = LocalDateTime.now().minusMinutes(5);
        
        double bidAmount1 = 120.0;
        double bidAmount2 = 130.0;
        double bidAmount3 = 140.0;
        
        // Insert multiple bids
        assertTrue(bidDAO.insertBid(testUserId, testAuctionId, bidAmount1, bidTime1), "First bid should be inserted");
        assertTrue(bidDAO.insertBid(testUserId, testAuctionId, bidAmount2, bidTime2), "Second bid should be inserted");
        assertTrue(bidDAO.insertBid(testUserId, testAuctionId, bidAmount3, bidTime3), "Third bid should be inserted");
        
        // Act
        List<TestBidDAO.BidData> bidHistory = bidDAO.findBidHistoryByAuctionId(testAuctionId);
        
        // Assert
        assertNotNull(bidHistory, "Bid history should not be null");
        assertEquals(3, bidHistory.size(), "Should find 3 bids");
        
        // Verify bids are in chronological order (ASC) - compare without nanoseconds
        assertEquals(bidTime1.truncatedTo(java.time.temporal.ChronoUnit.MILLIS), 
                    bidHistory.get(0).time().truncatedTo(java.time.temporal.ChronoUnit.MILLIS), 
                    "First bid should be earliest");
        assertEquals(bidTime2.truncatedTo(java.time.temporal.ChronoUnit.MILLIS), 
                    bidHistory.get(1).time().truncatedTo(java.time.temporal.ChronoUnit.MILLIS), 
                    "Second bid should be in middle");
        assertEquals(bidTime3.truncatedTo(java.time.temporal.ChronoUnit.MILLIS), 
                    bidHistory.get(2).time().truncatedTo(java.time.temporal.ChronoUnit.MILLIS), 
                    "Third bid should be latest");
        
        // Verify bid details
        for (TestBidDAO.BidData bid : bidHistory) {
            assertEquals(testUserId, bid.userId(), "User ID should match");
            assertEquals(testAuctionId, bid.auctionId(), "Auction ID should match");
        }
        
        assertEquals(bidAmount1, bidHistory.get(0).amount(), "First bid amount should match");
        assertEquals(bidAmount2, bidHistory.get(1).amount(), "Second bid amount should match");
        assertEquals(bidAmount3, bidHistory.get(2).amount(), "Third bid amount should match");
    }
    
    @Test
    @DisplayName("Should return empty list when auction has no bid history")
    void testFindBidHistoryByAuctionId_NoBids() {
        // Arrange - Use auction with no bids
        
        // Act
        List<TestBidDAO.BidData> bidHistory = bidDAO.findBidHistoryByAuctionId(testAuctionId);
        
        // Assert
        assertNotNull(bidHistory, "Bid history should not be null");
        assertTrue(bidHistory.isEmpty(), "Should return empty list for auction with no bids");
    }
    
    @Test
    @DisplayName("Should return empty list when finding bid history for non-existent auction")
    void testFindBidHistoryByAuctionId_NonExistentAuction() {
        // Arrange
        int nonExistentAuctionId = 99999;
        
        // Act
        List<TestBidDAO.BidData> bidHistory = bidDAO.findBidHistoryByAuctionId(nonExistentAuctionId);
        
        // Assert
        assertNotNull(bidHistory, "Bid history should not be null");
        assertTrue(bidHistory.isEmpty(), "Should return empty list for non-existent auction");
    }
    
    @Test
    @DisplayName("Should find current price successfully")
    void testFindCurrentPrice_Success() throws SQLException {
        // Arrange
        double bidAmount = 150.0;
        LocalDateTime bidTime = LocalDateTime.now();
        
        // Insert bid to update current price
        assertTrue(bidDAO.insertBid(testUserId, testAuctionId, bidAmount, bidTime), "Bid should be inserted");
        
        // Update auction's current price (since bid insertion doesn't automatically update auction table)
        try (java.sql.PreparedStatement stmt = testConnection.prepareStatement(
                "UPDATE auctions SET cur_price = ? WHERE id = ?")) {
            stmt.setDouble(1, bidAmount);
            stmt.setInt(2, testAuctionId);
            stmt.executeUpdate();
        }
        
        // Act
        Double currentPrice = bidDAO.findCurrentPrice(testAuctionId);
        
        // Assert
        assertNotNull(currentPrice, "Current price should not be null");
        assertEquals(bidAmount, currentPrice, "Current price should match bid amount");
    }
    
    @Test
    @DisplayName("Should return null when finding current price for non-existent auction")
    void testFindCurrentPrice_NonExistentAuction() {
        // Arrange
        int nonExistentAuctionId = 99999;
        
        // Act
        Double currentPrice = bidDAO.findCurrentPrice(nonExistentAuctionId);
        
        // Assert
        assertNull(currentPrice, "Current price should be null for non-existent auction");
    }
    
    @Test
    @DisplayName("Should handle multiple bids from different users")
    void testMultipleBidsFromDifferentUsers() throws SQLException {
        // Arrange
        int bidder2Id = TestDatabaseSetup.insertTestUser(testConnection, "bidder2", "password123", "BIDDER");
        int bidder3Id = TestDatabaseSetup.insertTestUser(testConnection, "bidder3", "password123", "BIDDER");
        
        assertTrue(bidder2Id > 0, "Second bidder should be inserted");
        assertTrue(bidder3Id > 0, "Third bidder should be inserted");
        
        LocalDateTime bidTime1 = LocalDateTime.now().minusMinutes(30);
        LocalDateTime bidTime2 = LocalDateTime.now().minusMinutes(20);
        LocalDateTime bidTime3 = LocalDateTime.now().minusMinutes(10);
        
        // Insert bids from different users
        assertTrue(bidDAO.insertBid(testUserId, testAuctionId, 120.0, bidTime1), "First bidder's bid should be inserted");
        assertTrue(bidDAO.insertBid(bidder2Id, testAuctionId, 130.0, bidTime2), "Second bidder's bid should be inserted");
        assertTrue(bidDAO.insertBid(bidder3Id, testAuctionId, 140.0, bidTime3), "Third bidder's bid should be inserted");
        
        // Act
        List<TestBidDAO.BidData> bidHistory = bidDAO.findBidHistoryByAuctionId(testAuctionId);
        
        // Assert
        assertNotNull(bidHistory, "Bid history should not be null");
        assertEquals(3, bidHistory.size(), "Should find 3 bids");
        
        // Verify all users are represented
        boolean foundBidder1 = bidHistory.stream().anyMatch(bid -> bid.userId() == testUserId);
        boolean foundBidder2 = bidHistory.stream().anyMatch(bid -> bid.userId() == bidder2Id);
        boolean foundBidder3 = bidHistory.stream().anyMatch(bid -> bid.userId() == bidder3Id);
        
        assertTrue(foundBidder1, "First bidder should be found");
        assertTrue(foundBidder2, "Second bidder should be found");
        assertTrue(foundBidder3, "Third bidder should be found");
    }
    
    @Test
    @DisplayName("Should handle bids with same timestamp from different users")
    void testBidsWithSameTimestamp() throws SQLException {
        // Arrange
        int bidder2Id = TestDatabaseSetup.insertTestUser(testConnection, "bidder2", "password123", "BIDDER");
        assertTrue(bidder2Id > 0, "Second bidder should be inserted");
        
        LocalDateTime sameBidTime = LocalDateTime.now();
        
        // Insert bids with same timestamp
        assertTrue(bidDAO.insertBid(testUserId, testAuctionId, 120.0, sameBidTime), "First bidder's bid should be inserted");
        assertTrue(bidDAO.insertBid(bidder2Id, testAuctionId, 130.0, sameBidTime), "Second bidder's bid should be inserted");
        
        // Act
        List<TestBidDAO.BidData> bidHistory = bidDAO.findBidHistoryByAuctionId(testAuctionId);
        
        // Assert
        assertNotNull(bidHistory, "Bid history should not be null");
        assertEquals(2, bidHistory.size(), "Should find 2 bids");
        
        // Both bids should have the same timestamp (ignoring nanoseconds)
        assertEquals(sameBidTime.truncatedTo(java.time.temporal.ChronoUnit.MILLIS), 
                    bidHistory.get(0).time().truncatedTo(java.time.temporal.ChronoUnit.MILLIS), 
                    "First bid timestamp should match");
        assertEquals(sameBidTime.truncatedTo(java.time.temporal.ChronoUnit.MILLIS), 
                    bidHistory.get(1).time().truncatedTo(java.time.temporal.ChronoUnit.MILLIS), 
                    "Second bid timestamp should match");
    }
    
    @Test
    @DisplayName("Should handle concurrent bid insertions")
    void testConcurrentBidInsertions() throws SQLException, InterruptedException {
        // Arrange
        int numThreads = 5;
        Thread[] threads = new Thread[numThreads];
        boolean[] results = new boolean[numThreads];
        
        // Act - Create multiple threads inserting bids concurrently
        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    double bidAmount = 100.0 + (threadIndex * 10);
                    LocalDateTime bidTime = LocalDateTime.now().minusMinutes(numThreads - threadIndex);
                    
                    results[threadIndex] = bidDAO.insertBid(testUserId, testAuctionId, bidAmount, bidTime);
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
            assertTrue(results[i], "Concurrent bid insertion " + i + " should succeed");
        }
        
        // Verify all bids were inserted
        List<TestBidDAO.BidData> bidHistory = bidDAO.findBidHistoryByAuctionId(testAuctionId);
        assertEquals(numThreads, bidHistory.size(), "All concurrent bids should be inserted");
    }
    
    @Test
    @DisplayName("Should handle bid data record correctly")
    void testBidDataRecord() {
        // Arrange
        int userId = 123;
        int auctionId = 456;
        double amount = 789.0;
        LocalDateTime time = LocalDateTime.now();
        
        // Act
        TestBidDAO.BidData bidData = new TestBidDAO.BidData(userId, auctionId, amount, time);
        
        // Assert
        assertEquals(userId, bidData.userId(), "User ID should match");
        assertEquals(auctionId, bidData.auctionId(), "Auction ID should match");
        assertEquals(amount, bidData.amount(), "Amount should match");
        assertEquals(time, bidData.time(), "Time should match");
    }
    
    @Test
    @DisplayName("Should handle bid history ordering correctly")
    void testBidHistoryOrdering() throws SQLException {
        // Arrange
        LocalDateTime oldestTime = LocalDateTime.now().minusHours(2);
        LocalDateTime middleTime = LocalDateTime.now().minusHours(1);
        LocalDateTime newestTime = LocalDateTime.now().minusMinutes(30);
        
        // Insert bids in non-chronological order
        assertTrue(bidDAO.insertBid(testUserId, testAuctionId, 150.0, newestTime), "Newest bid should be inserted");
        assertTrue(bidDAO.insertBid(testUserId, testAuctionId, 120.0, oldestTime), "Oldest bid should be inserted");
        assertTrue(bidDAO.insertBid(testUserId, testAuctionId, 130.0, middleTime), "Middle bid should be inserted");
        
        // Act
        List<TestBidDAO.BidData> bidHistory = bidDAO.findBidHistoryByAuctionId(testAuctionId);
        
        // Assert
        assertNotNull(bidHistory, "Bid history should not be null");
        assertEquals(3, bidHistory.size(), "Should find 3 bids");
        
        // Verify chronological ordering (ASC) - compare without nanoseconds
        assertEquals(oldestTime.truncatedTo(java.time.temporal.ChronoUnit.MILLIS), 
                    bidHistory.get(0).time().truncatedTo(java.time.temporal.ChronoUnit.MILLIS), 
                    "Oldest bid should be first");
        assertEquals(middleTime.truncatedTo(java.time.temporal.ChronoUnit.MILLIS), 
                    bidHistory.get(1).time().truncatedTo(java.time.temporal.ChronoUnit.MILLIS), 
                    "Middle bid should be second");
        assertEquals(newestTime.truncatedTo(java.time.temporal.ChronoUnit.MILLIS), 
                    bidHistory.get(2).time().truncatedTo(java.time.temporal.ChronoUnit.MILLIS), 
                    "Newest bid should be third");
        
        assertEquals(120.0, bidHistory.get(0).amount(), "Oldest bid amount should be 120.0");
        assertEquals(130.0, bidHistory.get(1).amount(), "Middle bid amount should be 130.0");
        assertEquals(150.0, bidHistory.get(2).amount(), "Newest bid amount should be 150.0");
    }
    
    @Test
    @DisplayName("Should handle empty bid history gracefully")
    void testEmptyBidHistory() throws SQLException {
        // Arrange - Create new auction with no bids
        int emptyAuctionId = TestDatabaseSetup.insertTestAuction(testConnection, testItemId, testSellerId, 
                                                               100.0, 100.0, LocalDateTime.now(), 
                                                               LocalDateTime.now().plusHours(24), "RUNNING");
        assertTrue(emptyAuctionId > 0, "Empty auction should be inserted");
        
        // Act
        List<TestBidDAO.BidData> bidHistory = bidDAO.findBidHistoryByAuctionId(emptyAuctionId);
        
        // Assert
        assertNotNull(bidHistory, "Bid history should not be null");
        assertTrue(bidHistory.isEmpty(), "Bid history should be empty");
    }
}
