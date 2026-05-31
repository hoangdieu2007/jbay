package a88.jbay.data;

import a88.jbay.common.auction.*;
import a88.jbay.common.item.Item;
import a88.jbay.common.user.UserData;
import a88.jbay.dao.BidDAO;
import a88.jbay.dao.ItemDAO;
import a88.jbay.dao.UserDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuctionFactoryTest {

    @Mock
    private ItemDAO itemDAO;

    @Mock
    private UserDAO userDAO;

    @Mock
    private BidDAO bidDAO;

    private AuctionFactory factory;

    private AuctionData auctionData;
    private Item item;
    private UserData seller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        factory = new AuctionFactory(itemDAO, userDAO, bidDAO);

        item = new Item(1, "Test Item", "ELECTRONICS", "A test item", 100.0, new byte[]{});
        seller = new UserData(1, "seller1", "SELLER", "pass");
        auctionData = new AuctionData(
                1, 1, 1, 100.0, 100.0, 5.0,
                null,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                "OPENING", ""
        );
    }

    @Test
    @DisplayName("Should reconstruct full Auction from data")
    void testReconstruct() {
        when(itemDAO.findItemById(1)).thenReturn(item);
        when(userDAO.findByUserId(1)).thenReturn(seller);
        when(bidDAO.findBidHistoryByAuctionId(1)).thenReturn(List.of());

        Auction result = factory.reconstruct(auctionData);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Test Item", result.getItem().getName());
        assertEquals("seller1", result.getSellerName());
        assertEquals(100.0, result.getCurrentPrice());
        assertEquals(5.0, result.getMinIncrement());
        assertEquals(AuctionState.OPENING, result.getAuctionState());
        assertTrue(result.getSubscribers().contains(1));
        assertTrue(result.getBidHistory().isEmpty());
        verify(itemDAO).findItemById(1);
        verify(userDAO).findByUserId(1);
        verify(bidDAO).findBidHistoryByAuctionId(1);
    }

    @Test
    @DisplayName("Should return null when item not found")
    void testReconstruct_ItemNotFound() {
        when(itemDAO.findItemById(1)).thenReturn(null);

        Auction result = factory.reconstruct(auctionData);

        assertNull(result);
    }

    @Test
    @DisplayName("Should restore bid history during reconstruction")
    void testReconstruct_WithBidHistory() {
        AuctionData runningAuctionData = new AuctionData(
                1, 1, 1, 100.0, 200.0, 5.0, 2,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                "RUNNING", ""
        );
        when(itemDAO.findItemById(1)).thenReturn(item);
        when(userDAO.findByUserId(1)).thenReturn(seller);
        when(userDAO.findByUserId(2)).thenReturn(new UserData(2, "bidder1", "USER", "pass"));

        BidData bid1 = new BidData(2, 1, 150.0, LocalDateTime.now());
        BidData bid2 = new BidData(2, 1, 200.0, LocalDateTime.now().plusMinutes(1));
        when(bidDAO.findBidHistoryByAuctionId(1)).thenReturn(List.of(bid1, bid2));

        Auction result = factory.reconstruct(runningAuctionData);

        assertNotNull(result);
        assertEquals(2, result.getBidHistory().size());
        assertTrue(result.getSubscribers().contains(2));
    }

    @Test
    @DisplayName("Should reconstruct lightweight Auction for admin")
    void testReconstructForAdmin() {
        AuctionData adminData = new AuctionData(
                1, 1, 1, 100.0, 100.0, 5.0,
                null,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                "RUNNING", "Test Item"
        );
        when(userDAO.findByUserId(1)).thenReturn(seller);

        Auction result = factory.reconstructForAdmin(adminData);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Test Item", result.getItem().getName());
        assertEquals(5.0, result.getMinIncrement());
        assertEquals(AuctionState.RUNNING, result.getAuctionState());
        assertTrue(result.getBidHistory().isEmpty());
        assertFalse(result.hasSubscribers());
    }

    @Test
    @DisplayName("Should default to OPENING state when admin data has invalid state")
    void testReconstructForAdmin_InvalidState() {
        AuctionData invalidData = new AuctionData(
                1, 1, 1, 100.0, 100.0, 5.0,
                null,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                "INVALID_STATE", "Test Item"
        );
        when(userDAO.findByUserId(1)).thenReturn(seller);

        Auction result = factory.reconstructForAdmin(invalidData);

        assertNotNull(result);
        assertEquals(AuctionState.OPENING, result.getAuctionState());
    }
}
