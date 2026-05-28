package a88.jbay.data;

import a88.jbay.common.auction.BidTransaction;
import a88.jbay.dao.AuctionDAO;
import a88.jbay.dao.BidDAO;
import a88.jbay.server.DatabaseController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BidRepositoryTest {

    @Mock
    private DatabaseController dbController;

    @Mock
    private AuctionDAO auctionDAO;

    @Mock
    private BidDAO bidDAO;

    @Mock
    private Connection connection;

    private BidRepository bidRepository;

    private BidTransaction bidTx;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        bidRepository = new BidRepository(dbController, auctionDAO, bidDAO);

        when(dbController.getConnection()).thenReturn(connection);

        bidTx = new BidTransaction(1, "testuser", 150.0, LocalDateTime.now());
    }

    @Test
    @DisplayName("Should save bid successfully")
    void testSaveBid_Success() throws SQLException {
        when(bidDAO.insertBid(eq(connection), eq(1), eq(100), eq(150.0), any())).thenReturn(1);
        when(auctionDAO.updateCurrentBid(eq(connection), eq(100), eq(1))).thenReturn(true);

        boolean result = bidRepository.saveBid(100, bidTx);

        assertTrue(result);
        verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(connection).setAutoCommit(true);
    }

    @Test
    @DisplayName("Should rollback on bid insert failure")
    void testSaveBid_InsertFails() throws SQLException {
        when(bidDAO.insertBid(eq(connection), eq(1), eq(100), eq(150.0), any()))
                .thenThrow(new SQLException("Insert failed"));

        boolean result = bidRepository.saveBid(100, bidTx);

        assertFalse(result);
        verify(connection).rollback();
        verify(auctionDAO, never()).updateCurrentBid(any(Connection.class), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Should rollback on auction update failure")
    void testSaveBid_UpdateFails() throws SQLException {
        when(bidDAO.insertBid(eq(connection), eq(1), eq(100), eq(150.0), any())).thenReturn(1);
        when(auctionDAO.updateCurrentBid(eq(connection), eq(100), eq(1))).thenThrow(new RuntimeException("DB error"));

        boolean result = bidRepository.saveBid(100, bidTx);

        assertFalse(result);
        verify(connection).rollback();
    }

    @Test
    @DisplayName("Should return false when getConnection throws SQLException")
    void testSaveBid_DBConnectionFails() throws SQLException {
        when(dbController.getConnection()).thenThrow(new SQLException("Connection failed"));

        boolean result = bidRepository.saveBid(100, bidTx);

        assertFalse(result);
    }
}
