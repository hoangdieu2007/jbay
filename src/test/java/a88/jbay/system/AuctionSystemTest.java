package a88.jbay.system;

import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.common.auction.BidTransaction;
import a88.jbay.common.item.Item;
import a88.jbay.common.network.Response;
import a88.jbay.common.user.UserData;
import a88.jbay.data.AuctionRepository;
import a88.jbay.data.UserRepository;
import a88.jbay.system.update.UpdateSystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuctionSystemTest {

    private UpdateSystem updateSystem;
    private AuctionRepository auctionRepository;
    private UserRepository userRepository;

    private AuctionSystem auctionSystem;

    private Auction auction;
    private Item item;
    private UserData seller;

    @BeforeEach
    void setUp() {
        updateSystem = mock(UpdateSystem.class);
        auctionRepository = mock(AuctionRepository.class);
        userRepository = mock(UserRepository.class);

        lenient().when(auctionRepository.getAllActiveAuctions()).thenReturn(List.of());
        lenient().when(auctionRepository.setAuctionState(anyInt(), any())).thenReturn(true);

        auctionSystem = new AuctionSystem(updateSystem, auctionRepository, userRepository);

        verify(auctionRepository).loadActiveAuctions();

        auctionSystem.stopSystem();

        item = new Item(1, "Test Item", "ELECTRONICS", "A test item", 100.0);
        seller = new UserData(1, "seller1", "SELLER", "pass");
        auction = new Auction(1, item, seller, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        auction.setMinIncrement(5.0);
    }

    @AfterEach
    void tearDown() {
        auctionSystem.stopSystem();
    }

    @Test
    @DisplayName("Should create auction successfully")
    void testCreateAuction() {
        when(auctionRepository.insertItemAndAuction(eq(item), eq(1), eq(5.0), any(), any())).thenReturn(1);
        when(userRepository.findByUserId(1)).thenReturn(seller);

        boolean result = auctionSystem.createAuction(item, 1, 5.0,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        assertTrue(result);
        verify(auctionRepository).insertItemAndAuction(eq(item), eq(1), eq(5.0), any(), any());
        verify(auctionRepository).storeActiveAuction(any(Auction.class));
        verify(updateSystem).broadcastToAll(any(Response.class));
    }

    @Test
    @DisplayName("Should return false when auction creation fails")
    void testCreateAuction_Failure() {
        when(auctionRepository.insertItemAndAuction(eq(item), eq(1), eq(5.0), any(), any())).thenReturn(-1);

        boolean result = auctionSystem.createAuction(item, 1, 5.0,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        assertFalse(result);
        verify(auctionRepository, never()).storeActiveAuction(any());
        verify(updateSystem, never()).broadcastToAll(any());
    }

    @Test
    @DisplayName("Should place bid via BidSystem")
    void testPlaceBid() {
        try (MockedStatic<BidSystem> bidSystemMock = mockStatic(BidSystem.class)) {
            BidSystem mockBidSystem = mock(BidSystem.class);
            bidSystemMock.when(BidSystem::getInstance).thenReturn(mockBidSystem);
            when(mockBidSystem.placeBid(1, 100, 150.0)).thenReturn(true);

            boolean result = auctionSystem.placeBid(1, 100, 150.0);

            assertTrue(result);
            verify(mockBidSystem).placeBid(1, 100, 150.0);
        }
    }

    @Test
    @DisplayName("Should confirm payment and remove auction from cache")
    void testConfirmPayment() {
        auction.setAuctionState(AuctionState.FINISHED);
        auction.addBid(150.0, new BidTransaction(2, "bidder1", 150.0, LocalDateTime.now()));
        when(auctionRepository.getAuctionById(1)).thenReturn(auction);
        auction.subscribe(1);

        boolean result = auctionSystem.confirmPayment(1);

        assertTrue(result);
        verify(auctionRepository).setAuctionState(1, AuctionState.PAID);
        verify(auctionRepository).removeActiveAuction(1);
    }

    @Test
    @DisplayName("Should return false when confirming payment for unknown auction")
    void testConfirmPayment_UnknownAuction() {
        when(auctionRepository.getAuctionById(999)).thenReturn(null);

        boolean result = auctionSystem.confirmPayment(999);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should cancel auction in OPENING state")
    void testCancelAuction_Opening() {
        when(auctionRepository.getAuctionById(1)).thenReturn(auction);

        boolean result = auctionSystem.cancelAuction(1);

        assertTrue(result);
        verify(auctionRepository).removeActiveAuction(1);
    }

    @Test
    @DisplayName("Should cancel auction in RUNNING state")
    void testCancelAuction_Running() {
        auction.setAuctionState(AuctionState.RUNNING);
        when(auctionRepository.getAuctionById(1)).thenReturn(auction);

        boolean result = auctionSystem.cancelAuction(1);

        assertTrue(result);
        verify(auctionRepository).removeActiveAuction(1);
    }

    @Test
    @DisplayName("Should not cancel a FINISHED auction")
    void testCancelAuction_Finished() {
        auction.setAuctionState(AuctionState.FINISHED);
        when(auctionRepository.getAuctionById(1)).thenReturn(auction);

        boolean result = auctionSystem.cancelAuction(1);

        assertFalse(result);
        verify(auctionRepository, never()).setAuctionState(anyInt(), any());
    }

    @Test
    @DisplayName("Should not cancel a PAID auction")
    void testCancelAuction_Paid() {
        auction.setAuctionState(AuctionState.PAID);
        when(auctionRepository.getAuctionById(1)).thenReturn(auction);

        boolean result = auctionSystem.cancelAuction(1);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false when canceling unknown auction")
    void testCancelAuction_Unknown() {
        when(auctionRepository.getAuctionById(999)).thenReturn(null);

        boolean result = auctionSystem.cancelAuction(999);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should cancel all auctions for a seller")
    void testCancelAuctionsBySellerId() {
        when(auctionRepository.getAuctionsBySellerId(1)).thenReturn(List.of(auction));
        when(auctionRepository.getAuctionById(1)).thenReturn(auction);

        boolean result = auctionSystem.cancelAuctionsBySellerId(1);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should check if auction is active")
    void testIsAuctionActive() {
        when(auctionRepository.isAuctionActive(1)).thenReturn(true);

        assertTrue(auctionSystem.isAuctionActive(1));
    }

    @Test
    @DisplayName("Should get active auction list")
    void testGetActiveAuctionList() {
        when(auctionRepository.getActiveAuctionList()).thenReturn(List.of(auction));

        List<Auction> result = auctionSystem.getActiveAuctionList();

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should get active auctions except seller")
    void testGetActiveAuctionListExceptForSeller() {
        when(auctionRepository.getActiveAuctionListExceptForSeller(1)).thenReturn(List.of());

        List<Auction> result = auctionSystem.getActiveAuctionListExceptForSeller(1);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should update seller auctions for user")
    void testUpdateSellerAuctions() {
        when(auctionRepository.getAuctionsBySellerId(1)).thenReturn(List.of(auction));

        auctionSystem.updateSellerAuctions(1);

        verify(updateSystem).sendToUser(eq(1), any(Response.class));
    }

    @Test
    @DisplayName("Should update bidder auctions for user")
    void testUpdateBidderAuctions() {
        when(auctionRepository.getAuctionsByWinnerId(1)).thenReturn(List.of(auction));

        auctionSystem.updateBidderAuctions(1);

        verify(updateSystem).sendToUser(eq(1), any(Response.class));
    }

    @Test
    @DisplayName("Should update active auctions for user")
    void testUpdateActiveAuctions() {
        when(auctionRepository.getActiveAuctionListExceptForSeller(1)).thenReturn(List.of(auction));

        auctionSystem.updateActiveAuctions(1);

        verify(updateSystem).sendToUser(eq(1), any(Response.class));
    }

    @Test
    @DisplayName("Should update all auctions for user")
    void testUpdateAllAuctions() {
        when(auctionRepository.getActiveAuctionListExceptForSeller(1)).thenReturn(List.of(auction));
        when(auctionRepository.getAuctionsByWinnerId(1)).thenReturn(List.of());
        when(auctionRepository.getAuctionsBySellerId(1)).thenReturn(List.of());

        auctionSystem.updateAllAuctions(1);

        verify(updateSystem, times(3)).sendToUser(eq(1), any(Response.class));
    }

    @Test
    @DisplayName("Should update admin auctions")
    void testUpdateAdminAuctions() {
        when(auctionRepository.getAllAuctionsForAdmin()).thenReturn(List.of(auction));

        auctionSystem.updateAdminAuctions(1);

        verify(updateSystem).sendToUser(eq(1), any(Response.class));
    }

    @Test
    @DisplayName("Should reload system")
    void testReloadSystem() {
        when(auctionRepository.getAllActiveAuctions()).thenReturn(List.of(auction));

        auctionSystem.reloadSystem();

        verify(auctionRepository, times(2)).loadActiveAuctions();
        verify(updateSystem).broadcastAuctionUpdate(auction);
    }

    @Test
    @DisplayName("Should unsubscribe user from all active auctions")
    void testUnsubscribeUserFromAllAuctions() {
        when(auctionRepository.getAllActiveAuctions()).thenReturn(List.of(auction));

        auctionSystem.unsubscribeUserFromAllAuctions(1);

        assertFalse(auction.getSubscribers().contains(1));
    }

    @Test
    @DisplayName("Should get active auction by id")
    void testGetActiveAuctionById() {
        when(auctionRepository.getActiveAuctionById(1)).thenReturn(auction);

        Auction result = auctionSystem.getActiveAuctionById(1);

        assertSame(auction, result);
    }

    @Test
    @DisplayName("Should get auction by id")
    void testGetAuctionById() {
        when(auctionRepository.getAuctionById(1)).thenReturn(auction);

        Auction result = auctionSystem.getAuctionById(1);

        assertSame(auction, result);
    }

    @Test
    @DisplayName("Should get auctions by seller id")
    void testGetAuctionsBySellerId() {
        when(auctionRepository.getAuctionsBySellerId(1)).thenReturn(List.of(auction));

        List<Auction> result = auctionSystem.getAuctionsBySellerId(1);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should get auctions by winner id")
    void testGetAuctionsByWinnerId() {
        when(auctionRepository.getAuctionsByWinnerId(1)).thenReturn(List.of());

        List<Auction> result = auctionSystem.getAuctionsByWinnerId(1);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should list active auctions as string")
    void testListActiveAuctions() {
        when(auctionRepository.listActiveAuctions()).thenReturn("auction list");

        String result = auctionSystem.listActiveAuctions();

        assertEquals("auction list", result);
    }

    @Test
    @DisplayName("Should get all auctions for admin")
    void testGetAllAuctionsForAdmin() {
        when(auctionRepository.getAllAuctionsForAdmin()).thenReturn(List.of(auction));

        List<Auction> result = auctionSystem.getAllAuctionsForAdmin();

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should load active auctions on construction")
    void testConstructorLoadsActiveAuctions() {
        verify(auctionRepository, times(1)).loadActiveAuctions();
    }
}
