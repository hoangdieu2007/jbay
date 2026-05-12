package a88.jbay.common.auction;

import a88.jbay.common.item.Item;
import a88.jbay.system.AuctionSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Auction auto-bidding functionality
 */
class AuctionAutoBidTest {

    @Mock
    private AuctionSystem auctionSystem;

    @Mock
    private Item item;

    private Auction auction;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(item.getInitPrice()).thenReturn(100.0);
        when(item.getName()).thenReturn("Test Item");
        when(item.getType()).thenReturn("Electronics");
        when(item.getDescription()).thenReturn("Test description");

        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now().plusHours(24);
        auction = new Auction(1, item, "seller", startTime, endTime);
        auction.setAuctionState(AuctionState.RUNNING);
    }

    @Test
    @DisplayName("Should set auto-bid configuration for user")
    void testSetAutoBidConfig() {
        // Arrange
        int userId = 2;
        double maxAmount = 200.0;
        double increment = 10.0;

        // Act
        auction.setAutoBidConfig(userId, maxAmount, increment);

        // Assert
        assertTrue(auction.hasAutoBidConfig(userId));
        assertEquals(maxAmount, auction.getAutoBidConfigs().get(userId).getMaxAmount());
        assertEquals(increment, auction.getAutoBidConfigs().get(userId).getIncrement());
    }

    @Test
    @DisplayName("Should update auto-bid configuration for existing user")
    void testUpdateAutoBidConfig() {
        // Arrange
        int userId = 2;
        auction.setAutoBidConfig(userId, 200.0, 10.0);

        // Act
        auction.setAutoBidConfig(userId, 300.0, 15.0);

        // Assert
        assertEquals(300.0, auction.getAutoBidConfigs().get(userId).getMaxAmount());
        assertEquals(15.0, auction.getAutoBidConfigs().get(userId).getIncrement());
        assertEquals(1, auction.getAutoBidConfigs().size());
    }

    @Test
    @DisplayName("Should clear auto-bid configuration for user")
    void testClearAutoBidConfig() {
        // Arrange
        int userId = 2;
        auction.setAutoBidConfig(userId, 200.0, 10.0);
        assertTrue(auction.hasAutoBidConfig(userId));

        // Act
        auction.clearAutoBidConfig(userId);

        // Assert
        assertFalse(auction.hasAutoBidConfig(userId));
    }

    @Test
    @DisplayName("Should clear all auto-bid configurations")
    void testClearAllAutoBidConfigs() {
        // Arrange
        auction.setAutoBidConfig(2, 200.0, 10.0);
        auction.setAutoBidConfig(3, 300.0, 15.0);
        auction.setAutoBidConfig(4, 400.0, 20.0);
        assertEquals(3, auction.getAutoBidConfigs().size());

        // Act
        auction.clearAllAutoBidConfigs();

        // Assert
        assertEquals(0, auction.getAutoBidConfigs().size());
        assertFalse(auction.hasAutoBidConfig(2));
        assertFalse(auction.hasAutoBidConfig(3));
        assertFalse(auction.hasAutoBidConfig(4));
    }

    @Test
    @DisplayName("Should check if user has auto-bid configuration")
    void testHasAutoBidConfig() {
        // Arrange
        int userId = 2;
        auction.setAutoBidConfig(userId, 200.0, 10.0);

        // Act & Assert
        assertTrue(auction.hasAutoBidConfig(userId));
        assertFalse(auction.hasAutoBidConfig(999));
    }

    @Test
    @DisplayName("Should get auto-bid configurations")
    void testGetAutoBidConfigs() {
        // Arrange
        auction.setAutoBidConfig(2, 200.0, 10.0);
        auction.setAutoBidConfig(3, 300.0, 15.0);

        // Act
        Map<Integer, AutoBidConfig> configs = auction.getAutoBidConfigs();

        // Assert
        assertNotNull(configs);
        assertEquals(2, configs.size());
        assertTrue(configs.containsKey(2));
        assertTrue(configs.containsKey(3));
    }

    @Test
    @DisplayName("Should return copy of auto-bid configurations")
    void testGetAutoBidConfigs_ReturnsCopy() {
        // Arrange
        auction.setAutoBidConfig(2, 200.0, 10.0);
        Map<Integer, AutoBidConfig> configs1 = auction.getAutoBidConfigs();

        // Act
        auction.setAutoBidConfig(3, 300.0, 15.0);
        Map<Integer, AutoBidConfig> configs2 = auction.getAutoBidConfigs();

        // Assert
        assertNotSame(configs1, configs2, "Should return a new map instance");
        assertEquals(1, configs1.size());
        assertEquals(2, configs2.size());
    }

    @Test
    @DisplayName("Should handle multiple auto-bid configurations")
    void testMultipleAutoBidConfigs() {
        // Arrange
        auction.setAutoBidConfig(2, 200.0, 10.0);
        auction.setAutoBidConfig(3, 300.0, 15.0);
        auction.setAutoBidConfig(4, 400.0, 20.0);

        // Act & Assert
        assertEquals(3, auction.getAutoBidConfigs().size());
        assertTrue(auction.hasAutoBidConfig(2));
        assertTrue(auction.hasAutoBidConfig(3));
        assertTrue(auction.hasAutoBidConfig(4));
    }

    @Test
    @DisplayName("Should cancel auto-bid for specific user")
    void testCancelAutoBid_SpecificUser() {
        // Arrange
        int userId1 = 2;
        int userId2 = 3;
        auction.setAutoBidConfig(userId1, 200.0, 10.0);
        auction.setAutoBidConfig(userId2, 300.0, 15.0);

        // Act
        auction.clearAutoBidConfig(userId1);

        // Assert
        assertFalse(auction.hasAutoBidConfig(userId1));
        assertTrue(auction.hasAutoBidConfig(userId2));
        assertEquals(1, auction.getAutoBidConfigs().size());
    }

    @Test
    @DisplayName("Should handle auto-bid with small increment")
    void testAutoBid_SmallIncrement() {
        // Arrange
        int userId = 2;
        double maxAmount = 200.0;
        double smallIncrement = 0.01;

        // Act
        auction.setAutoBidConfig(userId, maxAmount, smallIncrement);

        // Assert
        assertEquals(smallIncrement, auction.getAutoBidConfigs().get(userId).getIncrement());
        assertTrue(smallIncrement > 0, "Increment should be positive");
    }

    @Test
    @DisplayName("Should handle auto-bid with large increment")
    void testAutoBid_LargeIncrement() {
        // Arrange
        int userId = 2;
        double maxAmount = 1000.0;
        double largeIncrement = 100.0;

        // Act
        auction.setAutoBidConfig(userId, maxAmount, largeIncrement);

        // Assert
        assertEquals(largeIncrement, auction.getAutoBidConfigs().get(userId).getIncrement());
    }

    @Test
    @DisplayName("Should handle auto-bid configuration persistence")
    void testAutoBidConfig_Persistence() {
        // Arrange
        int userId = 2;
        double maxAmount = 200.0;
        double increment = 10.0;

        // Act
        auction.setAutoBidConfig(userId, maxAmount, increment);
        Map<Integer, AutoBidConfig> configs = auction.getAutoBidConfigs();

        // Assert
        assertNotNull(configs.get(userId));
        assertEquals(maxAmount, configs.get(userId).getMaxAmount());
        assertEquals(increment, configs.get(userId).getIncrement());
    }

    @Test
    @DisplayName("Should handle AutoBidConfig serialization")
    void testAutoBidConfig_Serialization() {
        // Arrange
        AutoBidConfig config = new AutoBidConfig(200.0, 10.0);

        // Act & Assert
        assertEquals(200.0, config.getMaxAmount());
        assertEquals(10.0, config.getIncrement());
    }

    @Test
    @DisplayName("Should handle auto-bid with zero increment edge case")
    void testAutoBid_ZeroIncrement() {
        // Arrange
        int userId = 2;
        double maxAmount = 200.0;
        double zeroIncrement = 0.0;

        // Act
        auction.setAutoBidConfig(userId, maxAmount, zeroIncrement);

        // Assert
        assertEquals(zeroIncrement, auction.getAutoBidConfigs().get(userId).getIncrement());
    }

    @Test
    @DisplayName("Should handle auto-bid with negative max amount edge case")
    void testAutoBid_NegativeMaxAmount() {
        // Arrange
        int userId = 2;
        double negativeMaxAmount = -100.0;
        double increment = 10.0;

        // Act
        auction.setAutoBidConfig(userId, negativeMaxAmount, increment);

        // Assert
        assertEquals(negativeMaxAmount, auction.getAutoBidConfigs().get(userId).getMaxAmount());
    }

    @Test
    @DisplayName("Should not trigger auto-bid when no configurations exist")
    void testTriggerAutoBid_NoConfigs() {
        // Arrange
        double currentPrice = 100.0;
        BidTransaction tx = new BidTransaction(1, "user1", currentPrice, LocalDateTime.now());

        // Act
        auction.updatePrice(currentPrice, tx);

        // Assert
        assertEquals(0, auction.getAutoBidConfigs().size());
    }

    @Test
    @DisplayName("Should filter current winner from auto-bid candidates")
    void testAutoBid_FilterCurrentWinner() {
        // Arrange
        int winnerId = 2;
        int otherUserId = 3;
        auction.setAutoBidConfig(winnerId, 200.0, 10.0);
        auction.setAutoBidConfig(otherUserId, 250.0, 15.0);

        // Act
        BidTransaction tx = new BidTransaction(winnerId, "user2", 150.0, LocalDateTime.now());
        auction.updatePrice(150.0, tx);

        // Assert
        assertEquals(winnerId, auction.getWinnerId());
        assertTrue(auction.hasAutoBidConfig(winnerId));
        assertTrue(auction.hasAutoBidConfig(otherUserId));
    }

    @Test
    @DisplayName("Should handle auto-bid when auction is not running")
    void testAutoBid_AuctionNotRunning() {
        // Arrange
        auction.setAuctionState(AuctionState.OPENING);
        int userId = 2;
        auction.setAutoBidConfig(userId, 200.0, 10.0);

        // Act & Assert
        assertEquals(AuctionState.OPENING, auction.getAuctionState());
        assertTrue(auction.hasAutoBidConfig(userId));
    }

    @Test
    @DisplayName("Should maintain auto-bid config after bid update")
    void testAutoBidConfig_AfterBidUpdate() {
        // Arrange
        int userId = 2;
        double maxAmount = 200.0;
        double increment = 10.0;
        auction.setAutoBidConfig(userId, maxAmount, increment);

        // Act
        BidTransaction tx = new BidTransaction(1, "user1", 150.0, LocalDateTime.now());
        auction.updatePrice(150.0, tx);

        // Assert
        assertTrue(auction.hasAutoBidConfig(userId));
        assertEquals(maxAmount, auction.getAutoBidConfigs().get(userId).getMaxAmount());
        assertEquals(increment, auction.getAutoBidConfigs().get(userId).getIncrement());
    }
}
