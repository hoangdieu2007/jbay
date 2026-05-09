package a88.jbay.testutil;

import a88.jbay.model.entity.item.Item;
import a88.jbay.model.entity.user.User;
import a88.jbay.model.event.Auction;
import a88.jbay.model.event.AuctionState;
import a88.jbay.model.event.BidTransaction;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * factory for creating test data objects
 */
public class TestDataFactory {
    
    // Test users
    public static User createTestSeller() {
        return new User(1, "SELLER", "seller123", "session123");
    }
    
    public static User createTestBidder() {
        return new User(2, "BIDDER", "bidder123", "session456");
    }
    
    public static User createTestAdmin() {
        return new User(3, "ADMIN", "admin123", "session789");
    }
    
    public static User createTestBannedUser() {
        return new User(4, "BAN", "banned123", "session000");
    }
    
    // Test items
    public static Item createTestItem() {
        return new Item(0, "Test Laptop", "Electronics", "A high-quality laptop for testing", 999.99, new byte[0]);
    }
    
    public static Item createTestBook() {
        return new Item(1, "Test Book", "Books", "A programming book for testing", 49.99, new byte[0]);
    }
    
    public static Item createTestPhone() {
        return new Item(2, "Test Phone", "Electronics", "A smartphone for testing", 699.99, new byte[0]);
    }
    
    // Test auctions
    public static Auction createTestAuction() {
        LocalDateTime now = LocalDateTime.now();
        return new Auction(1, createTestItem(), "seller123", now, now.plusHours(24));
    }
    
    public static Auction createRunningAuction() {
        Auction auction = createTestAuction();
        auction.setAuctionState(AuctionState.RUNNING);
        return auction;
    }
    
    public static Auction createFinishedAuction() {
        Auction auction = createTestAuction();
        auction.setAuctionState(AuctionState.FINISHED);
        return auction;
    }
    
    public static Auction createAuctionWithBids() {
        Auction auction = createRunningAuction();
        LocalDateTime now = LocalDateTime.now();
        
        BidTransaction bid1 = new BidTransaction(2, "u1", 1100.0, now.minusMinutes(30));
        BidTransaction bid2 = new BidTransaction(3, "u2", 1200.0, now.minusMinutes(15));
        
        auction.updatePrice(1100.0, bid1);
        auction.updatePrice(1200.0, bid2);
        
        return auction;
    }
    
    // Test bid transactions
    public static BidTransaction createTestBid() {
        return new BidTransaction(2, "u99", 1500.0, LocalDateTime.now());
    }
    
    public static List<BidTransaction> createTestBidHistory() {
        LocalDateTime now = LocalDateTime.now();
        return Arrays.asList(
            new BidTransaction(2, "u6", 1100.0, now.minusHours(2)),
            new BidTransaction(3, "u7", 1200.0, now.minusHours(1)),
            new BidTransaction(2, "u8", 1300.0, now.minusMinutes(30)),
            new BidTransaction(4, "u9", 1400.0, now.minusMinutes(15))
        );
    }
    
    // Time-based test data
    public static LocalDateTime getPastTime(int hoursAgo) {
        return LocalDateTime.now().minusHours(hoursAgo);
    }
    
    public static LocalDateTime getFutureTime(int hoursFromNow) {
        return LocalDateTime.now().plusHours(hoursFromNow);
    }
    
    public static LocalDateTime getYesterday() {
        return LocalDateTime.now().minusDays(1);
    }
    
    public static LocalDateTime getTomorrow() {
        return LocalDateTime.now().plusDays(1);
    }
    
    // Test strings
    public static String getTestUsername() {
        return "testuser" + System.currentTimeMillis();
    }
    
    public static String getTestPassword() {
        return "testpass123";
    }
    
    public static String getTestEmail() {
        return "test" + System.currentTimeMillis() + "@example.com";
    }
}
