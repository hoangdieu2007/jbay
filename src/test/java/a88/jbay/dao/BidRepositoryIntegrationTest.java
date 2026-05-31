package a88.jbay.dao;

import a88.jbay.common.auction.BidTransaction;
import a88.jbay.data.BidRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BidRepositoryIntegrationTest extends DaoTestBase {

    private BidRepository bidRepository;

    private int sellerId;
    private int bidderId;
    private int itemId;
    private int auctionId;

    @BeforeEach
    void setUp() throws Exception {
        bidRepository = new BidRepository(dbController,
                new AuctionDAOImpl(dbController),
                new BidDAOImpl(dbController));

        sellerId = insertUser("seller", "pass", "SELLER", null);
        bidderId = insertUser("bidder", "pass", "USER", null);
        itemId = insertItem("Item", "TYPE", "Desc", 100.0, new byte[]{});
        auctionId = insertAuction(itemId, sellerId, 100.0, 5.0,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), "RUNNING");
    }

    @Test
    @DisplayName("Should save bid with transaction")
    void testSaveBid_Success() {
        BidTransaction tx = new BidTransaction(bidderId, "bidder", 150.0, LocalDateTime.now());

        boolean result = bidRepository.saveBid(auctionId, tx);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false when auction does not exist")
    void testSaveBid_NonExistentAuction() {
        BidTransaction tx = new BidTransaction(bidderId, "bidder", 150.0, LocalDateTime.now());

        boolean result = bidRepository.saveBid(999, tx);

        assertFalse(result);
    }
}
