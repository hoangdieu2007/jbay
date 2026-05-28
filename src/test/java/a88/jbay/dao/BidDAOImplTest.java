package a88.jbay.dao;

import a88.jbay.common.auction.BidData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BidDAOImplTest extends DaoTestBase {

    private final BidDAO bidDAO = new BidDAOImpl(dbController);

    @Test
    @DisplayName("Should insert bid and return generated id")
    void testInsertBid() throws Exception {
        int userId = insertUser("bidder", "pass", "BIDDER", null);
        int itemId = insertItem("Item", "TYPE", "Desc", 100.0, new byte[]{});
        int auctionId = insertAuction(itemId, userId, 100.0, 5.0,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), "RUNNING");

        int bidId = bidDAO.insertBid(userId, auctionId, 150.0, LocalDateTime.now());

        assertTrue(bidId > 0);
    }

    @Test
    @DisplayName("Should find bid history by auction id")
    void testFindBidHistoryByAuctionId() throws Exception {
        int sellerId = insertUser("seller", "pass", "SELLER", null);
        int bidderId = insertUser("bidder", "pass", "BIDDER", null);
        int itemId = insertItem("Item", "TYPE", "Desc", 100.0, new byte[]{});
        int auctionId = insertAuction(itemId, sellerId, 100.0, 5.0,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), "RUNNING");

        LocalDateTime t1 = LocalDateTime.of(2026, 5, 28, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 5, 28, 10, 5);
        int bid1Id = bidDAO.insertBid(bidderId, auctionId, 150.0, t1);
        int bid2Id = bidDAO.insertBid(bidderId, auctionId, 200.0, t2);

        List<BidData> history = bidDAO.findBidHistoryByAuctionId(auctionId);

        assertEquals(2, history.size());
        assertEquals(150.0, history.get(0).amount(), 0.001);
        assertEquals(200.0, history.get(1).amount(), 0.001);
    }

    @Test
    @DisplayName("Should return empty list when no bids for auction")
    void testFindBidHistoryByAuctionId_Empty() throws Exception {
        int userId = insertUser("user", "pass", "BIDDER", null);
        int itemId = insertItem("Item", "TYPE", "Desc", 100.0, new byte[]{});
        int auctionId = insertAuction(itemId, userId, 100.0, 5.0,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), "OPENING");

        List<BidData> history = bidDAO.findBidHistoryByAuctionId(auctionId);

        assertTrue(history.isEmpty());
    }

    @Test
    @DisplayName("Should exclude bids from banned users")
    void testFindBidHistoryByAuctionId_ExcludesBanned() throws Exception {
        int sellerId = insertUser("seller", "pass", "SELLER", null);
        int bidderId = insertUser("bidder", "pass", "BIDDER", null);
        int bannedId = insertUser("banned", "pass", "BAN", null);
        int itemId = insertItem("Item", "TYPE", "Desc", 100.0, new byte[]{});
        int auctionId = insertAuction(itemId, sellerId, 100.0, 5.0,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), "RUNNING");

        bidDAO.insertBid(bidderId, auctionId, 150.0, LocalDateTime.now());
        bidDAO.insertBid(bannedId, auctionId, 200.0, LocalDateTime.now());

        List<BidData> history = bidDAO.findBidHistoryByAuctionId(auctionId);

        assertEquals(1, history.size());
        assertEquals(150.0, history.get(0).amount(), 0.001);
    }

    @Test
    @DisplayName("Should insert bid via transactional overload")
    void testInsertBid_Transactional() throws Exception {
        int userId = insertUser("user", "pass", "BIDDER", null);
        int itemId = insertItem("Item", "TYPE", "Desc", 100.0, new byte[]{});
        int auctionId = insertAuction(itemId, userId, 100.0, 5.0,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), "RUNNING");

        try (java.sql.Connection conn = dbController.getConnection()) {
            int bidId = bidDAO.insertBid(conn, userId, auctionId, 150.0, LocalDateTime.now());
            assertTrue(bidId > 0);
        }

        List<BidData> history = bidDAO.findBidHistoryByAuctionId(auctionId);
        assertEquals(1, history.size());
    }
}
