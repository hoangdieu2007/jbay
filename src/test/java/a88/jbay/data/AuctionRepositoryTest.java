package a88.jbay.data;

import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionData;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.common.item.Item;
import a88.jbay.common.user.UserData;
import a88.jbay.dao.AuctionDAO;
import a88.jbay.dao.BidDAO;
import a88.jbay.dao.ItemDAO;
import a88.jbay.dao.UserDAO;
import a88.jbay.server.DatabaseController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuctionRepositoryTest {

    @Mock
    private DatabaseController dbController;

    @Mock
    private AuctionDAO auctionDAO;

    @Mock
    private ItemDAO itemDAO;

    @Mock
    private UserDAO userDAO;

    @Mock
    private BidDAO bidDAO;

    @Mock
    private Connection connection;

    private AuctionRepository auctionRepository;

    private Auction auction;
    private Item item;
    private UserData seller;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        auctionRepository = new AuctionRepository(dbController, auctionDAO, itemDAO, userDAO, bidDAO);

        item = new Item(1, "Test Item", "ELECTRONICS", "A test item", 100.0, new byte[]{});
        seller = new UserData(1, "seller1", "SELLER", "pass");
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);

        auction = new Auction(1, item, seller, start, end);
        auction.setMinIncrement(5.0);

        when(dbController.getConnection()).thenReturn(connection);
    }

    @Test
    @DisplayName("Should store and retrieve active auction from cache")
    void testStoreAndGetActiveAuction() {
        auctionRepository.storeActiveAuction(auction);

        Auction result = auctionRepository.getActiveAuctionById(1);

        assertSame(auction, result);
    }

    @Test
    @DisplayName("Should remove active auction from cache")
    void testRemoveActiveAuction() {
        auctionRepository.storeActiveAuction(auction);
        auctionRepository.removeActiveAuction(1);

        assertNull(auctionRepository.getActiveAuctionById(1));
    }

    @Test
    @DisplayName("Should check if auction is active")
    void testIsAuctionActive() {
        auctionRepository.storeActiveAuction(auction);

        assertTrue(auctionRepository.isAuctionActive(1));
        assertFalse(auctionRepository.isAuctionActive(999));
    }

    @Test
    @DisplayName("Should get all active auctions")
    void testGetAllActiveAuctions() {
        auctionRepository.storeActiveAuction(auction);

        Collection<Auction> all = auctionRepository.getAllActiveAuctions();

        assertEquals(1, all.size());
    }

    @Test
    @DisplayName("Should get active auction list")
    void testGetActiveAuctionList() {
        auctionRepository.storeActiveAuction(auction);

        List<Auction> list = auctionRepository.getActiveAuctionList();

        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("Should get active auctions except seller's own")
    void testGetActiveAuctionListExceptForSeller() {
        UserData seller2 = new UserData(2, "seller2", "SELLER", "pass");
        Item item2 = new Item(2, "Item 2", "TYPE", "Desc", 50.0, new byte[]{});
        Auction auction2 = new Auction(2, item2, seller2, LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        auctionRepository.storeActiveAuction(auction);
        auctionRepository.storeActiveAuction(auction2);

        when(userDAO.findByUserId(1)).thenReturn(seller);

        List<Auction> result = auctionRepository.getActiveAuctionListExceptForSeller(1);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getId());
    }

    @Test
    @DisplayName("Should get auction by id from cache first")
    void testGetAuctionById_FromCache() {
        auctionRepository.storeActiveAuction(auction);

        Auction result = auctionRepository.getAuctionById(1);

        assertSame(auction, result);
        verify(auctionDAO, never()).findAuctionById(anyInt());
    }

    @Test
    @DisplayName("Should fall back to DAO when auction not in cache")
    void testGetAuctionById_FromDAO() {
        AuctionData data = new AuctionData(
                1, 1, 1, 100.0, 100.0, 5.0, null,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                "OPENING", ""
        );
        when(auctionDAO.findAuctionById(1)).thenReturn(data);
        when(itemDAO.findItemById(1)).thenReturn(item);
        when(userDAO.findByUserId(1)).thenReturn(seller);
        when(bidDAO.findBidHistoryByAuctionId(1)).thenReturn(List.of());

        Auction result = auctionRepository.getAuctionById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(auctionDAO).findAuctionById(1);
    }

    @Test
    @DisplayName("Should return null when auction not found in DAO either")
    void testGetAuctionById_NotFound() {
        when(auctionDAO.findAuctionById(999)).thenReturn(null);

        Auction result = auctionRepository.getAuctionById(999);

        assertNull(result);
    }

    @Test
    @DisplayName("Should set auction state in DB and update cache")
    void testSetAuctionState() {
        auctionRepository.storeActiveAuction(auction);
        when(auctionDAO.setAuctionState(1, AuctionState.RUNNING)).thenReturn(true);

        boolean result = auctionRepository.setAuctionState(1, AuctionState.RUNNING);

        assertTrue(result);
        assertEquals(AuctionState.RUNNING, auction.getAuctionState());
    }

    @Test
    @DisplayName("Should return false when setAuctionState in DAO fails")
    void testSetAuctionState_DAOFails() {
        when(auctionDAO.setAuctionState(1, AuctionState.CANCELED)).thenReturn(false);

        boolean result = auctionRepository.setAuctionState(1, AuctionState.CANCELED);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should update end time in DB and cache")
    void testUpdateEndTime() {
        auctionRepository.storeActiveAuction(auction);
        LocalDateTime newEnd = LocalDateTime.now().plusDays(2);
        when(auctionDAO.updateEndTime(1, newEnd)).thenReturn(true);

        boolean result = auctionRepository.updateEndTime(1, newEnd);

        assertTrue(result);
        assertEquals(newEnd, auction.getEndTime());
    }

    @Test
    @DisplayName("Should insert item and auction in transaction")
    void testInsertItemAndAuction() throws SQLException {
        when(itemDAO.insertItem(eq(connection), eq(item))).thenReturn(1);
        when(auctionDAO.insertAuction(eq(connection), eq(1), eq(1), eq(100.0),
                eq(5.0), any(), any())).thenReturn(1);

        int auctionId = auctionRepository.insertItemAndAuction(item, 1, 5.0,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        assertEquals(1, auctionId);
        verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(connection).setAutoCommit(true);
    }

    @Test
    @DisplayName("Should rollback transaction when item insert fails")
    void testInsertItemAndAuction_RollbackOnFailure() throws SQLException {
        when(itemDAO.insertItem(eq(connection), eq(item))).thenThrow(new RuntimeException("DB error"));

        int auctionId = auctionRepository.insertItemAndAuction(item, 1, 5.0,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        assertEquals(-1, auctionId);
        verify(connection).rollback();
    }

    @Test
    @DisplayName("Should rollback transaction when auction insert fails")
    void testInsertItemAndAuction_RollbackOnAuctionFailure() throws SQLException {
        when(itemDAO.insertItem(eq(connection), eq(item))).thenReturn(1);
        when(auctionDAO.insertAuction(eq(connection), eq(1), eq(1), eq(100.0),
                eq(5.0), any(), any())).thenThrow(new RuntimeException("DB error"));

        int auctionId = auctionRepository.insertItemAndAuction(item, 1, 5.0,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        assertEquals(-1, auctionId);
        verify(connection).rollback();
    }

    @Test
    @DisplayName("Should load active auctions from DAO into cache")
    void testLoadActiveAuctions() {
        AuctionData activeData = new AuctionData(
                1, 1, 1, 100.0, 100.0, 5.0, null,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                "OPENING", ""
        );
        when(auctionDAO.findAllActiveAuctions()).thenReturn(List.of(activeData));
        when(itemDAO.findItemById(1)).thenReturn(item);
        when(userDAO.findByUserId(1)).thenReturn(seller);
        when(bidDAO.findBidHistoryByAuctionId(1)).thenReturn(List.of());

        auctionRepository.loadActiveAuctions();

        assertTrue(auctionRepository.isAuctionActive(1));
        verify(auctionDAO).findAllActiveAuctions();
    }

    @Test
    @DisplayName("Should get username by user id")
    void testGetUsernameByUserId() {
        when(userDAO.findByUserId(1)).thenReturn(seller);

        String username = auctionRepository.getUsernameByUserId(1);

        assertEquals("seller1", username);
    }

    @Test
    @DisplayName("Should get all auctions for admin")
    void testGetAllAuctionsForAdmin() {
        AuctionData data = new AuctionData(
                1, 1, 1, 100.0, 100.0, 5.0, null,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                "FINISHED", "Test Item"
        );
        when(auctionDAO.getAllAuctionsForAdmin()).thenReturn(List.of(data));
        when(itemDAO.findItemById(1)).thenReturn(item);
        when(userDAO.findByUserId(1)).thenReturn(seller);
        when(bidDAO.findBidHistoryByAuctionId(1)).thenReturn(List.of());

        List<Auction> result = auctionRepository.getAllAuctionsForAdmin();

        assertEquals(1, result.size());
    }
}
