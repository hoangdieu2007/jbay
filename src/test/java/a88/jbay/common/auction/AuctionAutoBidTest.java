package a88.jbay.common.auction;

import a88.jbay.common.item.Item;
import a88.jbay.common.user.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the auction's current auto-bid configuration.
 */
class AuctionAutoBidTest {

    private Auction auction;

    @BeforeEach
    void setUp() {
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 15, 10, 0);
        Item item = new Item(1, "Test Item", "Electronics", "Test description", 100.0);
        UserData seller = new UserData(1, "seller", "USER", "password");

        auction = new Auction(1, item, seller, startTime, startTime.plusDays(1));
        auction.setAuctionState(AuctionState.RUNNING);
    }

    @Test
    @DisplayName("Should start without current auto-bid configuration")
    void testInitialCurrentAutoBidConfig() {
        assertNull(auction.getCurrAutoBidConfig());
        assertFalse(auction.hasAutoBidConfig(2));
        assertNull(auction.getAutoBidConfig(2));
    }

    @Test
    @DisplayName("Should set current auto-bid configuration")
    void testSetCurrentAutoBidConfig() {
        AutoBidConfig config = new AutoBidConfig(2, 200.0, 10.0);

        auction.setCurrAutoBidConfig(config);

        assertSame(config, auction.getCurrAutoBidConfig());
        assertTrue(auction.hasAutoBidConfig(2));
        assertSame(config, auction.getAutoBidConfig(2));
    }

    @Test
    @DisplayName("Should replace current auto-bid configuration")
    void testReplaceCurrentAutoBidConfig() {
        AutoBidConfig firstConfig = new AutoBidConfig(2, 200.0, 10.0);
        AutoBidConfig secondConfig = new AutoBidConfig(3, 300.0, 15.0);

        auction.setCurrAutoBidConfig(firstConfig);
        auction.setCurrAutoBidConfig(secondConfig);

        assertSame(secondConfig, auction.getCurrAutoBidConfig());
        assertFalse(auction.hasAutoBidConfig(2));
        assertTrue(auction.hasAutoBidConfig(3));
    }

    @Test
    @DisplayName("Should clear current auto-bid configuration")
    void testClearCurrentAutoBidConfig() {
        auction.setCurrAutoBidConfig(new AutoBidConfig(2, 200.0, 10.0));

        auction.setCurrAutoBidConfig(null);

        assertNull(auction.getCurrAutoBidConfig());
        assertFalse(auction.hasAutoBidConfig(2));
        assertNull(auction.getAutoBidConfig(2));
    }

    @Test
    @DisplayName("Should return config only for matching user")
    void testGetAutoBidConfigByUser() {
        AutoBidConfig config = new AutoBidConfig(2, 200.0, 10.0);

        auction.setCurrAutoBidConfig(config);

        assertSame(config, auction.getAutoBidConfig(2));
        assertNull(auction.getAutoBidConfig(999));
    }

    @Test
    @DisplayName("Should preserve max amount and increment")
    void testCurrentAutoBidConfigValues() {
        AutoBidConfig config = new AutoBidConfig(2, 250.0, 12.5);

        auction.setCurrAutoBidConfig(config);

        AutoBidConfig currentConfig = auction.getCurrAutoBidConfig();
        assertEquals(2, currentConfig.getUserId());
        assertEquals(250.0, currentConfig.getMaxAmount());
        assertEquals(12.5, currentConfig.getIncrement());
    }

    @Test
    @DisplayName("Should support zero increment edge case")
    void testCurrentAutoBidConfigWithZeroIncrement() {
        AutoBidConfig config = new AutoBidConfig(2, 200.0, 0.0);

        auction.setCurrAutoBidConfig(config);

        assertEquals(0.0, auction.getCurrAutoBidConfig().getIncrement());
    }

    @Test
    @DisplayName("Should support negative max amount edge case")
    void testCurrentAutoBidConfigWithNegativeMaxAmount() {
        AutoBidConfig config = new AutoBidConfig(2, -100.0, 10.0);

        auction.setCurrAutoBidConfig(config);

        assertEquals(-100.0, auction.getCurrAutoBidConfig().getMaxAmount());
    }
}
