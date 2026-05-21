package a88.jbay.system;

import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionState;
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
        when(auctionDAO.updateCurrentPrice(anyInt(), anyDouble(), anyInt())).thenReturn(true);
        when(bidDAO.insertBid(anyInt(), anyInt(), anyDouble(), any())).thenReturn(true);
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
        verify(auction).updatePrice(eq(bidAmount), any());
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
        verify(auction, never()).updatePrice(anyDouble(), any());
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
        verify(auction, never()).updatePrice(anyDouble(), any());
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
        verify(auction, never()).updatePrice(anyDouble(), any());
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
        verify(auction, never()).updatePrice(anyDouble(), any());
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
        verify(auction, never()).updatePrice(anyDouble(), any());
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
        when(auctionDAO.updateCurrentPrice(anyInt(), anyDouble(), anyInt())).thenReturn(true);
        when(bidDAO.insertBid(anyInt(), anyInt(), anyDouble(), any())).thenReturn(false);
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
        when(auctionDAO.updateCurrentPrice(anyInt(), anyDouble(), anyInt())).thenReturn(false);
        when(bidDAO.insertBid(anyInt(), anyInt(), anyDouble(), any())).thenReturn(true);
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
}
