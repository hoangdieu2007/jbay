package a88.jbay.dao;

import a88.jbay.common.auction.AuctionData;
import a88.jbay.common.auction.AuctionState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuctionDAOImplTest extends DaoTestBase {

    private final AuctionDAO auctionDAO = new AuctionDAOImpl(dbController);

    @Test
    @DisplayName("Should insert auction and return generated id")
    void testInsertAuction() throws Exception {
        int userId = insertUser("seller", "pass", "SELLER", null);
        int itemId = insertItem("Item", "TYPE", "Desc", 100.0, new byte[]{});

        int auctionId = auctionDAO.insertAuction(itemId, userId, 100.0, 5.0,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        assertTrue(auctionId > 0);
    }

    @Test
    @DisplayName("Should find auction by id")
    void testFindAuctionById() throws Exception {
        int userId = insertUser("seller", "pass", "SELLER", null);
        int itemId = insertItem("Item", "TYPE", "Desc", 100.0, new byte[]{});
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 10, 10, 0);
        int auctionId = auctionDAO.insertAuction(itemId, userId, 100.0, 5.0, start, end);

        AuctionData result = auctionDAO.findAuctionById(auctionId);

        assertNotNull(result);
        assertEquals(itemId, result.itemId());
        assertEquals(userId, result.sellerId());
        assertEquals(100.0, result.startPrice(), 0.001);
        assertEquals(5.0, result.minIncrement(), 0.001);
        assertNotNull(result.startTime());
        assertNotNull(result.endTime());
        assertEquals("OPENING", result.state());
    }

    @Test
    @DisplayName("Should return null when auction not found")
    void testFindAuctionById_NotFound() {
        AuctionData result = auctionDAO.findAuctionById(999);

        assertNull(result);
    }

    @Test
    @DisplayName("Should update current bid on auction")
    void testUpdateCurrentBid() throws Exception {
        int sellerId = insertUser("seller", "pass", "SELLER", null);
        int bidderId = insertUser("bidder", "pass", "BIDDER", null);
        int itemId = insertItem("Item", "TYPE", "Desc", 100.0, new byte[]{});
        int auctionId = insertAuction(itemId, sellerId, 100.0, 5.0,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), "RUNNING");
        int bidId = insertBid(bidderId, auctionId, 150.0, LocalDateTime.now());

        boolean updated = auctionDAO.updateCurrentBid(auctionId, bidId);

        assertTrue(updated);
    }

    @Test
    @DisplayName("Should update end time of auction")
    void testUpdateEndTime() throws Exception {
        int userId = insertUser("seller", "pass", "SELLER", null);
        int itemId = insertItem("Item", "TYPE", "Desc", 100.0, new byte[]{});
        int auctionId = insertAuction(itemId, userId, 100.0, 5.0,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), "RUNNING");
        LocalDateTime newEnd = LocalDateTime.now().plusDays(5);

        boolean updated = auctionDAO.updateEndTime(auctionId, newEnd);

        assertTrue(updated);
        AuctionData data = auctionDAO.findAuctionById(auctionId);
        assertEquals(newEnd.toLocalDate(), data.endTime().toLocalDate());
    }

    @Test
    @DisplayName("Should set auction state")
    void testSetAuctionState() throws Exception {
        int userId = insertUser("seller", "pass", "SELLER", null);
        int itemId = insertItem("Item", "TYPE", "Desc", 100.0, new byte[]{});
        int auctionId = insertAuction(itemId, userId, 100.0, 5.0,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), "OPENING");

        boolean updated = auctionDAO.setAuctionState(auctionId, AuctionState.RUNNING);

        assertTrue(updated);
        AuctionData data = auctionDAO.findAuctionById(auctionId);
        assertEquals("RUNNING", data.state());
    }

    @Test
    @DisplayName("Should find current price of auction")
    void testFindCurrentPrice() throws Exception {
        int sellerId = insertUser("seller", "pass", "SELLER", null);
        int bidderId = insertUser("bidder", "pass", "BIDDER", null);
        int itemId = insertItem("Item", "TYPE", "Desc", 100.0, new byte[]{});
        int auctionId = insertAuction(itemId, sellerId, 100.0, 5.0,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), "RUNNING");
        int bidId = insertBid(bidderId, auctionId, 150.0, LocalDateTime.now());
        auctionDAO.updateCurrentBid(auctionId, bidId);

        Double price = auctionDAO.findCurrentPrice(auctionId);

        assertNotNull(price);
        assertEquals(150.0, price, 0.001);
    }

    @Test
    @DisplayName("Should return start price when no bids")
    void testFindCurrentPrice_NoBids() throws Exception {
        int userId = insertUser("seller", "pass", "SELLER", null);
        int itemId = insertItem("Item", "TYPE", "Desc", 100.0, new byte[]{});
        int auctionId = insertAuction(itemId, userId, 100.0, 5.0,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), "OPENING");

        Double price = auctionDAO.findCurrentPrice(auctionId);

        assertNotNull(price);
        assertEquals(100.0, price, 0.001);
    }

    @Test
    @DisplayName("Should find auctions by seller id")
    void testFindAuctionsBySellerId() throws Exception {
        int userId = insertUser("seller", "pass", "SELLER", null);
        int itemId1 = insertItem("Item1", "TYPE", "Desc", 100.0, new byte[]{});
        int itemId2 = insertItem("Item2", "TYPE", "Desc", 200.0, new byte[]{});
        insertAuction(itemId1, userId, 100.0, 5.0, LocalDateTime.now(), LocalDateTime.now().plusDays(1), "RUNNING");
        insertAuction(itemId2, userId, 200.0, 10.0, LocalDateTime.now(), LocalDateTime.now().plusDays(2), "OPENING");

        List<AuctionData> auctions = auctionDAO.findAuctionsBySellerId(userId);

        assertEquals(2, auctions.size());
    }

    @Test
    @DisplayName("Should find all active auctions")
    void testFindAllActiveAuctions() throws Exception {
        int userId = insertUser("seller", "pass", "SELLER", null);
        int itemId1 = insertItem("Item1", "TYPE", "Desc", 100.0, new byte[]{});
        int itemId2 = insertItem("Item2", "TYPE", "Desc", 200.0, new byte[]{});
        insertAuction(itemId1, userId, 100.0, 5.0, LocalDateTime.now(), LocalDateTime.now().plusDays(1), "RUNNING");
        insertAuction(itemId2, userId, 200.0, 10.0, LocalDateTime.now(), LocalDateTime.now().plusDays(2), "OPENING");

        List<AuctionData> active = auctionDAO.findAllActiveAuctions();

        assertEquals(2, active.size());
    }

    @Test
    @DisplayName("Should exclude non-active auctions")
    void testFindAllActiveAuctions_ExcludesFinished() throws Exception {
        int userId = insertUser("seller", "pass", "SELLER", null);
        int itemId1 = insertItem("Item1", "TYPE", "Desc", 100.0, new byte[]{});
        int itemId2 = insertItem("Item2", "TYPE", "Desc", 200.0, new byte[]{});
        insertAuction(itemId1, userId, 100.0, 5.0, LocalDateTime.now(), LocalDateTime.now().plusDays(1), "RUNNING");
        insertAuction(itemId2, userId, 200.0, 10.0, LocalDateTime.now(), LocalDateTime.now().plusDays(2), "FINISHED");

        List<AuctionData> active = auctionDAO.findAllActiveAuctions();

        assertEquals(1, active.size());
    }

    @Test
    @DisplayName("Should get all auctions for admin")
    void testGetAllAuctionsForAdmin() throws Exception {
        int userId = insertUser("seller", "pass", "SELLER", null);
        int itemId = insertItem("AdminItem", "TYPE", "Desc", 100.0, new byte[]{});
        int auctionId = insertAuction(itemId, userId, 100.0, 5.0,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), "RUNNING");

        List<AuctionData> all = auctionDAO.getAllAuctionsForAdmin();

        assertEquals(1, all.size());
        assertEquals("AdminItem", all.get(0).itemName());
    }
}
