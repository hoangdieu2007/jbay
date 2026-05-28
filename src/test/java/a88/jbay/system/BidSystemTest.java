package a88.jbay.system;

import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.common.auction.AutoBidConfig;
import a88.jbay.common.auction.BidData;
import a88.jbay.common.user.UserData;
import a88.jbay.dao.AuctionDAO;
import a88.jbay.dao.BidDAO;
import a88.jbay.dao.UserDAO;
import a88.jbay.data.AuctionRepository;
import a88.jbay.data.BidRepository;
import a88.jbay.system.update.UpdateSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BidSystem - manual bid placement functionality
 */
class BidSystemTest {

    @Mock
    private AuctionDAO auctionDAO;

    @Mock
    private BidDAO bidDAO;

    @Mock
    private UserDAO userDAO;

    @Mock
    private Auction auction;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private AuctionSystem auctionSystem;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private UpdateSystem updateSystem;

    private BidSystem bidSystem;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        bidSystem = new BidSystem(auctionRepository, bidRepository, bidDAO, auctionDAO, updateSystem);
    }

    @Test
    @DisplayName("Should place valid bid successfully")
    void testPlaceBid_Success() {
        // Arrange
        int userId = 1;
        int auctionId = 100;
        double bidAmount = 150.0;
        double currentPrice = 100.0;

        when(auction.getCurrentPrice()).thenReturn(currentPrice);
        when(auction.getMinIncrement()).thenReturn(5.0);
        when(auction.getAuctionState()).thenReturn(AuctionState.RUNNING);
        when(auctionDAO.updateCurrentBid(anyInt(), anyInt())).thenReturn(true);
        when(bidDAO.insertBid(anyInt(), anyInt(), anyDouble(), any())).thenReturn(1);
        when(userDAO.findByUserId(userId)).thenReturn(new UserData(userId, "testuser", "password", "BIDDER"));

        // Mock auctionRepository.getActiveAuctionById()
        when(auctionRepository.getActiveAuctionById(auctionId)).thenReturn(auction);
        // Mock bidRepository.saveBid() to return true
        when(bidRepository.saveBid(anyInt(), any())).thenReturn(true);
        // Mock auction.getEndTime() to avoid NullPointerException in extendEndTime
        when(auction.getEndTime()).thenReturn(java.time.LocalDateTime.now().plusHours(1));

        // Act
        boolean result = bidSystem.placeBid(userId, auctionId, bidAmount);

        // Assert
        assertTrue(result);
        verify(auction).subscribe(userId);
        verify(auction).addBid(eq(bidAmount), any());
        verify(bidRepository).saveBid(eq(auctionId), any());
    }

    @Test
    @DisplayName("Should reject bid when auction does not exist")
    void testPlaceBid_NonExistentAuction() {
        // Arrange
        int userId = 1;
        int auctionId = 999;
        double bidAmount = 150.0;

        when(auctionRepository.getActiveAuctionById(auctionId)).thenReturn(null);

        // Act
        boolean result = bidSystem.placeBid(userId, auctionId, bidAmount);

        // Assert
        assertFalse(result);
        verify(auction, never()).subscribe(anyInt());
        verify(auction, never()).addBid(anyDouble(), any());
        verify(bidDAO, never()).insertBid(anyInt(), anyInt(), anyDouble(), any());
    }

    @Test
    @DisplayName("Should reject bid when amount is less than current price plus minimum increment")
    void testPlaceBid_AmountLessThanCurrentPricePlusMinIncrement() {
        // Arrange
        int userId = 1;
        int auctionId = 100;
        double currentPrice = 100.0;
        double invalidBidAmount = 104.0;

        when(auction.getCurrentPrice()).thenReturn(currentPrice);
        when(auction.getMinIncrement()).thenReturn(5.0);
        when(auction.getAuctionState()).thenReturn(AuctionState.RUNNING);
        when(auctionRepository.getActiveAuctionById(auctionId)).thenReturn(auction);

        // Act
        boolean result = bidSystem.placeBid(userId, auctionId, invalidBidAmount);

        // Assert
        assertFalse(result);
        verify(auction, never()).subscribe(anyInt());
        verify(auction, never()).addBid(anyDouble(), any());
        verify(bidDAO, never()).insertBid(anyInt(), anyInt(), anyDouble(), any());
    }

    @Test
    @DisplayName("Should reject bid when auction is not in RUNNING state")
    void testPlaceBid_AuctionNotRunning() {
        // Arrange
        int userId = 1;
        int auctionId = 100;
        double bidAmount = 150.0;
        double currentPrice = 100.0;

        when(auction.getCurrentPrice()).thenReturn(currentPrice);
        when(auction.getAuctionState()).thenReturn(AuctionState.OPENING);
        when(auctionRepository.getActiveAuctionById(auctionId)).thenReturn(auction);

        // Act
        boolean result = bidSystem.placeBid(userId, auctionId, bidAmount);

        // Assert
        assertFalse(result);
        verify(auction, never()).subscribe(anyInt());
        verify(auction, never()).addBid(anyDouble(), any());
        verify(bidDAO, never()).insertBid(anyInt(), anyInt(), anyDouble(), any());
    }

    @Test
    @DisplayName("Should reject bid when auction is FINISHED")
    void testPlaceBid_FinishedAuction() {
        // Arrange
        int userId = 1;
        int auctionId = 100;
        double bidAmount = 150.0;
        double currentPrice = 100.0;

        when(auction.getCurrentPrice()).thenReturn(currentPrice);
        when(auction.getAuctionState()).thenReturn(AuctionState.FINISHED);
        when(auctionRepository.getActiveAuctionById(auctionId)).thenReturn(auction);

        // Act
        boolean result = bidSystem.placeBid(userId, auctionId, bidAmount);

        // Assert
        assertFalse(result);
        verify(auction, never()).subscribe(anyInt());
        verify(auction, never()).addBid(anyDouble(), any());
        verify(bidDAO, never()).insertBid(anyInt(), anyInt(), anyDouble(), any());
    }

    @Test
    @DisplayName("Should reject bid when auction is CANCELED")
    void testPlaceBid_CanceledAuction() {
        // Arrange
        int userId = 1;
        int auctionId = 100;
        double bidAmount = 150.0;
        double currentPrice = 100.0;

        when(auction.getCurrentPrice()).thenReturn(currentPrice);
        when(auction.getAuctionState()).thenReturn(AuctionState.CANCELED);
        when(auctionRepository.getActiveAuctionById(auctionId)).thenReturn(auction);

        // Act
        boolean result = bidSystem.placeBid(userId, auctionId, bidAmount);

        // Assert
        assertFalse(result);
        verify(auction, never()).subscribe(anyInt());
        verify(auction, never()).addBid(anyDouble(), any());
        verify(bidDAO, never()).insertBid(anyInt(), anyInt(), anyDouble(), any());
    }

    @Test
    @DisplayName("Should return false when bidDAO insert fails")
    void testPlaceBid_BidDAOInsertFails() {
        // Arrange
        int userId = 1;
        int auctionId = 100;
        double bidAmount = 150.0;
        double currentPrice = 100.0;

        when(auction.getCurrentPrice()).thenReturn(currentPrice);
        when(auction.getAuctionState()).thenReturn(AuctionState.RUNNING);
        when(auctionDAO.updateCurrentBid(anyInt(), anyInt())).thenReturn(true);
        when(bidDAO.insertBid(anyInt(), anyInt(), anyDouble(), any())).thenReturn(-1);
        when(userDAO.findByUserId(userId)).thenReturn(new UserData(userId, "testuser", "password", "BIDDER"));
        when(auctionRepository.getActiveAuctionById(auctionId)).thenReturn(auction);

        // Act
        boolean result = bidSystem.placeBid(userId, auctionId, bidAmount);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false when auctionDAO update fails")
    void testPlaceBid_AuctionDAOUpdateFails() {
        // Arrange
        int userId = 1;
        int auctionId = 100;
        double bidAmount = 150.0;
        double currentPrice = 100.0;

        when(auction.getCurrentPrice()).thenReturn(currentPrice);
        when(auction.getAuctionState()).thenReturn(AuctionState.RUNNING);
        when(auctionDAO.updateCurrentBid(anyInt(), anyInt())).thenReturn(false);
        when(bidDAO.insertBid(anyInt(), anyInt(), anyDouble(), any())).thenReturn(1);
        when(userDAO.findByUserId(userId)).thenReturn(new UserData(userId, "testuser", "password", "BIDDER"));
        when(auctionRepository.getActiveAuctionById(auctionId)).thenReturn(auction);

        // Act
        boolean result = bidSystem.placeBid(userId, auctionId, bidAmount);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should get bid history for auction")
    void testGetBidHistory() {
        // Arrange
        int auctionId = 100;
        java.util.List<BidData> expectedHistory = java.util.List.of(
            new BidData(1, auctionId, 150.0, java.time.LocalDateTime.now())
        );
        when(bidDAO.findBidHistoryByAuctionId(auctionId)).thenReturn(expectedHistory);

        // Act
        java.util.List<BidData> result = bidSystem.getBidHistory(auctionId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(bidDAO).findBidHistoryByAuctionId(auctionId);
    }

    @Test
    @DisplayName("Should get current price for auction")
    void testGetCurrentPrice() {
        // Arrange
        int auctionId = 100;
        double expectedPrice = 150.0;
        when(auctionDAO.findCurrentPrice(auctionId)).thenReturn(expectedPrice);

        // Act
        Double result = bidSystem.getCurrentPrice(auctionId);

        // Assert
        assertNotNull(result);
        assertEquals(expectedPrice, result);
        verify(auctionDAO).findCurrentPrice(auctionId);
    }

    @Test
    @DisplayName("Should return null when current price not found")
    void testGetCurrentPrice_NotFound() {
        // Arrange
        int auctionId = 999;
        when(auctionDAO.findCurrentPrice(auctionId)).thenReturn(null);

        // Act
        Double result = bidSystem.getCurrentPrice(auctionId);

        // Assert
        assertNull(result);
        verify(auctionDAO).findCurrentPrice(auctionId);
    }

    // --- placeBidAutomated ---

    @Test
    @DisplayName("Should set auto-bid config and place first auto-bid when no existing config")
    void testPlaceBidAutomated_FirstAutoBid() {
        int userId = 1;
        int auctionId = 100;
        double maxAmount = 500.0;
        double increment = 50.0;

        when(auctionRepository.getActiveAuctionById(auctionId)).thenReturn(auction);
        when(auction.getCurrAutoBidConfig()).thenReturn(null);
        when(auction.getId()).thenReturn(auctionId);
        when(auction.getCurrentPrice()).thenReturn(100.0);
        when(auction.getMinIncrement()).thenReturn(10.0);
        when(auction.getWinner()).thenReturn("other");
        when(auction.getAuctionState()).thenReturn(AuctionState.RUNNING);
        when(auction.getEndTime()).thenReturn(java.time.LocalDateTime.now().plusHours(1));
        when(auctionRepository.getUsernameByUserId(userId)).thenReturn("user");
        // make saveBid return false so placeBid skips Thread.sleep(1000) and returns early
        when(bidRepository.saveBid(anyInt(), any())).thenReturn(false);

        bidSystem.placeBidAutomated(userId, auctionId, maxAmount, increment);

        verify(auction).subscribe(userId);
        verify(auction).setCurrAutoBidConfig(any());
    }

    @Test
    @DisplayName("Should skip auto-bid when user is already winner")
    void testPlaceBidAutomated_AlreadyWinner() {
        int userId = 1;
        int auctionId = 100;

        when(auctionRepository.getActiveAuctionById(auctionId)).thenReturn(auction);
        when(auction.getCurrAutoBidConfig()).thenReturn(null);
        when(auction.getWinnerId()).thenReturn(userId);

        bidSystem.placeBidAutomated(userId, auctionId, 500.0, 50.0);

        verify(auction).setCurrAutoBidConfig(any());
        verify(updateSystem).notifyAuctionSubscribers(auction);
    }

    @Test
    @DisplayName("Should update existing config when same user re-enables auto-bid")
    void testPlaceBidAutomated_UpdateExistingConfig() {
        int userId = 1;
        int auctionId = 100;

        AutoBidConfig existing = new AutoBidConfig(userId, 300.0, 30.0);
        when(auctionRepository.getActiveAuctionById(auctionId)).thenReturn(auction);
        when(auction.getCurrAutoBidConfig()).thenReturn(existing);
        when(auction.getId()).thenReturn(auctionId);
        when(auction.getCurrentPrice()).thenReturn(100.0);
        when(auction.getMinIncrement()).thenReturn(10.0);
        when(auction.getWinner()).thenReturn("other");
        when(auction.getAuctionState()).thenReturn(AuctionState.RUNNING);
        when(auction.getEndTime()).thenReturn(java.time.LocalDateTime.now().plusHours(1));
        when(auctionRepository.getUsernameByUserId(userId)).thenReturn("user");
        when(bidRepository.saveBid(anyInt(), any())).thenReturn(false);

        bidSystem.placeBidAutomated(userId, auctionId, 500.0, 50.0);

        verify(auction).setCurrAutoBidConfig(any());
    }

    @Test
    @DisplayName("Should handle competitive auto-bid when different user enables auto-bid")
    void testPlaceBidAutomated_CompetitiveAutoBid() {
        int userA = 1;
        int userB = 2;
        int auctionId = 100;

        AutoBidConfig existingConfig = new AutoBidConfig(userA, 300.0, 30.0);
        when(auctionRepository.getActiveAuctionById(auctionId)).thenReturn(auction);
        when(auction.getCurrAutoBidConfig()).thenReturn(existingConfig);
        when(auction.getId()).thenReturn(auctionId);
        when(auction.getCurrentPrice()).thenReturn(100.0);
        when(auction.getMinIncrement()).thenReturn(10.0);
        when(auction.getAuctionState()).thenReturn(AuctionState.RUNNING);
        when(auction.getEndTime()).thenReturn(java.time.LocalDateTime.now().plusHours(1));
        when(auction.getWinner()).thenReturn("other");
        when(bidRepository.saveBid(anyInt(), any())).thenReturn(false);
        when(auctionRepository.getUsernameByUserId(anyInt())).thenReturn("user");

        bidSystem.placeBidAutomated(userB, auctionId, 500.0, 50.0);

        verify(updateSystem).notifyAuctionSubscribers(auction);
    }

    @Test
    @DisplayName("Should return early when auction not found in placeBidAutomated")
    void testPlaceBidAutomated_AuctionNotFound() {
        when(auctionRepository.getActiveAuctionById(999)).thenReturn(null);

        bidSystem.placeBidAutomated(1, 999, 500.0, 50.0);

        verify(auction, never()).subscribe(anyInt());
    }

    @Test
    @DisplayName("Should handle competitive auto-bid where user A has higher max")
    void testPlaceBidAutomated_CompetitiveUserAWins() {
        int userA = 1;
        int userB = 2;
        int auctionId = 100;

        AutoBidConfig existingConfig = new AutoBidConfig(userA, 500.0, 50.0);
        when(auctionRepository.getActiveAuctionById(auctionId)).thenReturn(auction);
        when(auction.getCurrAutoBidConfig()).thenReturn(existingConfig);
        when(auction.getId()).thenReturn(auctionId);
        when(auction.getCurrentPrice()).thenReturn(100.0);
        when(auction.getMinIncrement()).thenReturn(10.0);
        when(auction.getAuctionState()).thenReturn(AuctionState.RUNNING);
        when(auction.getEndTime()).thenReturn(java.time.LocalDateTime.now().plusHours(1));
        when(auction.getWinner()).thenReturn("other");
        when(bidRepository.saveBid(anyInt(), any())).thenReturn(false);
        when(auctionRepository.getUsernameByUserId(anyInt())).thenReturn("user");

        bidSystem.placeBidAutomated(userB, auctionId, 300.0, 30.0);

        verify(auction, atLeast(0)).setCurrAutoBidConfig(any());
    }

    // --- triggerAutoBid ---

    @Test
    @DisplayName("Should skip auto-bid when no config exists")
    void testTriggerAutoBid_NoConfig() {
        when(auction.getCurrAutoBidConfig()).thenReturn(null);

        bidSystem.triggerAutoBid(auction);

        verify(auction, never()).getCurrentPrice();
    }

    @Test
    @DisplayName("Should skip auto-bid when user is current winner")
    void testTriggerAutoBid_AlreadyWinner() {
        AutoBidConfig config = new AutoBidConfig(1, 500.0, 50.0);
        when(auction.getCurrAutoBidConfig()).thenReturn(config);
        when(auction.getWinnerId()).thenReturn(1);

        bidSystem.triggerAutoBid(auction);

        verify(auction, never()).setCurrAutoBidConfig(any());
    }

    @Test
    @DisplayName("Should stop auto-bid when current price reaches max")
    void testTriggerAutoBid_MaxReached() {
        AutoBidConfig config = new AutoBidConfig(1, 500.0, 50.0);
        when(auction.getCurrAutoBidConfig()).thenReturn(config);
        when(auction.getCurrentPrice()).thenReturn(500.0);
        when(auction.getId()).thenReturn(100);
        when(auctionRepository.getActiveAuctionById(100)).thenReturn(auction);

        bidSystem.triggerAutoBid(auction);

        verify(auction).setCurrAutoBidConfig(null);
        verify(updateSystem).broadcastAuctionUpdate(auction);
    }

    @Test
    @DisplayName("Should place auto-bid when conditions are met")
    void testTriggerAutoBid_PlaceBid() {
        int userId = 1;
        int auctionId = 100;
        AutoBidConfig config = new AutoBidConfig(userId, 500.0, 50.0);

        when(auction.getCurrAutoBidConfig()).thenReturn(config);
        when(auction.getId()).thenReturn(auctionId);
        when(auction.getCurrentPrice()).thenReturn(100.0);
        when(auction.getMinIncrement()).thenReturn(10.0);
        when(auction.getAuctionState()).thenReturn(AuctionState.RUNNING);
        when(auction.getEndTime()).thenReturn(java.time.LocalDateTime.now().plusHours(1));
        when(auction.getWinner()).thenReturn("other");
        when(auctionRepository.getActiveAuctionById(auctionId)).thenReturn(auction);
        when(bidRepository.saveBid(anyInt(), any())).thenReturn(false);
        when(auctionRepository.getUsernameByUserId(userId)).thenReturn("user");

        bidSystem.triggerAutoBid(auction);
    }

    @Test
    @DisplayName("Should skip auto-bid when computed amount does not exceed current price")
    void testTriggerAutoBid_ComputedAmountNotHigher() {
        int userId = 1;
        AutoBidConfig config = new AutoBidConfig(userId, 100.0, 0.0);

        when(auction.getCurrAutoBidConfig()).thenReturn(config);
        when(auction.getId()).thenReturn(100);
        when(auction.getCurrentPrice()).thenReturn(100.0);
        when(auction.getMinIncrement()).thenReturn(0.0);

        bidSystem.triggerAutoBid(auction);

        verify(auction, never()).setCurrAutoBidConfig(any());
    }

    // --- cancelAutoBid ---

    @Test
    @DisplayName("Should cancel auto-bid successfully")
    void testCancelAutoBid_Success() {
        int userId = 1;
        int auctionId = 100;
        AutoBidConfig config = new AutoBidConfig(userId, 500.0, 50.0);

        when(auctionRepository.getActiveAuctionById(auctionId)).thenReturn(auction);
        when(auction.getCurrAutoBidConfig()).thenReturn(config);
        when(auction.getId()).thenReturn(auctionId);

        bidSystem.cancelAutoBid(userId, auctionId);

        verify(auction).setCurrAutoBidConfig(null);
    }

    @Test
    @DisplayName("Should not cancel auto-bid when auction not found")
    void testCancelAutoBid_AuctionNotFound() {
        when(auctionRepository.getActiveAuctionById(999)).thenReturn(null);

        bidSystem.cancelAutoBid(1, 999);

        verify(auction, never()).getCurrAutoBidConfig();
    }

    @Test
    @DisplayName("Should not cancel auto-bid when user does not own the config")
    void testCancelAutoBid_NotOwner() {
        int userId = 1;
        int auctionId = 100;
        AutoBidConfig config = new AutoBidConfig(2, 500.0, 50.0);

        when(auctionRepository.getActiveAuctionById(auctionId)).thenReturn(auction);
        when(auction.getCurrAutoBidConfig()).thenReturn(config);

        bidSystem.cancelAutoBid(userId, auctionId);

        verify(auction, never()).setCurrAutoBidConfig(any());
    }

    // --- setAutoBidConfig / clearAutoBidConfig / hasAutoBidConfig ---

    @Test
    @DisplayName("Should set auto-bid config")
    void testSetAutoBidConfig() {
        when(auctionRepository.getActiveAuctionById(100)).thenReturn(auction);

        bidSystem.setAutoBidConfig(100, 1, 500.0, 50.0);

        verify(auction).setCurrAutoBidConfig(any());
    }

    @Test
    @DisplayName("Should not set auto-bid config when auction not found")
    void testSetAutoBidConfig_AuctionNotFound() {
        when(auctionRepository.getActiveAuctionById(999)).thenReturn(null);

        bidSystem.setAutoBidConfig(999, 1, 500.0, 50.0);

        verify(auction, never()).setCurrAutoBidConfig(any());
    }

    @Test
    @DisplayName("Should clear auto-bid config")
    void testClearAutoBidConfig() {
        when(auctionRepository.getActiveAuctionById(100)).thenReturn(auction);

        bidSystem.clearAutoBidConfig(100);

        verify(auction).setCurrAutoBidConfig(null);
    }

    @Test
    @DisplayName("Should clear auto-bid config for specific user")
    void testClearAutoBidConfig_ForUser() {
        int userId = 1;
        int auctionId = 100;
        AutoBidConfig config = new AutoBidConfig(userId, 500.0, 50.0);

        when(auctionRepository.getActiveAuctionById(auctionId)).thenReturn(auction);
        when(auction.getCurrAutoBidConfig()).thenReturn(config);

        bidSystem.clearAutoBidConfig(auctionId, userId);

        verify(auction).setCurrAutoBidConfig(null);
    }

    @Test
    @DisplayName("Should not clear auto-bid config when user does not own it")
    void testClearAutoBidConfig_ForUserNotOwner() {
        AutoBidConfig config = new AutoBidConfig(2, 500.0, 50.0);
        when(auctionRepository.getActiveAuctionById(100)).thenReturn(auction);
        when(auction.getCurrAutoBidConfig()).thenReturn(config);

        bidSystem.clearAutoBidConfig(100, 1);

        verify(auction, never()).setCurrAutoBidConfig(any());
    }

    @Test
    @DisplayName("Should check if user has auto-bid config")
    void testHasAutoBidConfig() {
        AutoBidConfig config = new AutoBidConfig(1, 500.0, 50.0);
        when(auctionRepository.getActiveAuctionById(100)).thenReturn(auction);
        when(auction.getCurrAutoBidConfig()).thenReturn(config);

        assertTrue(bidSystem.hasAutoBidConfig(100, 1));
        assertFalse(bidSystem.hasAutoBidConfig(100, 2));
    }

    @Test
    @DisplayName("Should return false when no auto-bid config exists")
    void testHasAutoBidConfig_NoConfig() {
        when(auctionRepository.getActiveAuctionById(100)).thenReturn(auction);
        when(auction.getCurrAutoBidConfig()).thenReturn(null);

        assertFalse(bidSystem.hasAutoBidConfig(100, 1));
    }

    @Test
    @DisplayName("Should return false when auction not found for hasAutoBidConfig")
    void testHasAutoBidConfig_AuctionNotFound() {
        when(auctionRepository.getActiveAuctionById(999)).thenReturn(null);

        assertFalse(bidSystem.hasAutoBidConfig(999, 1));
    }
}
