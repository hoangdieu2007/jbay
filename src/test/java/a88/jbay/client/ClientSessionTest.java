package a88.jbay.client;

import a88.jbay.common.auction.Auction;
import a88.jbay.common.user.User;
import javafx.collections.ObservableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientSessionTest {

    private ClientSession session;

    @BeforeEach
    void setUp() {
        session = ClientSession.getInstance();
        session.resetSession();
        session.getAdminUsers().clear();
        session.getAdminAuctions().clear();
    }

    @Test
    void testSingleton() {
        assertSame(ClientSession.getInstance(), ClientSession.getInstance());
    }

    @Test
    void testDefaultUser() {
        User user = session.getUser();
        assertEquals(-1, user.getId());
        assertEquals("GUEST", user.getRole());
        assertEquals("guest", user.getUsername());
    }

    @Test
    void testSetUser() {
        User user = new User(5, "ADMIN", "admin1", "sess1");
        session.setUser(user);
        assertSame(user, session.getUser());
    }

    @Test
    void testGetSellerAuctions() {
        ObservableMap<Integer, Auction> auctions = session.getSellerAuctions();
        assertNotNull(auctions);
        assertTrue(auctions.isEmpty());
    }

    @Test
    void testGetBidderAuctions() {
        ObservableMap<Integer, Auction> auctions = session.getBidderAuctions();
        assertNotNull(auctions);
        assertTrue(auctions.isEmpty());
    }

    @Test
    void testGetWonAuctions() {
        ObservableMap<Integer, Auction> auctions = session.getWonAuctions();
        assertNotNull(auctions);
        assertTrue(auctions.isEmpty());
    }

    @Test
    void testGetAdminUsers() {
        ObservableMap<Integer, User> users = session.getAdminUsers();
        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    void testGetAdminAuctions() {
        ObservableMap<Integer, Auction> auctions = session.getAdminAuctions();
        assertNotNull(auctions);
        assertTrue(auctions.isEmpty());
    }

    @Test
    void testResetSession() {
        session.setUser(new User(1, "USER", "test", "sess"));
        session.getSellerAuctions().put(1, null);
        session.getBidderAuctions().put(2, null);

        session.resetSession();

        assertEquals(-1, session.getUser().getId());
        assertTrue(session.getSellerAuctions().isEmpty());
        assertTrue(session.getBidderAuctions().isEmpty());
        assertTrue(session.getWonAuctions().isEmpty());
    }

    @Test
    void testAdminUsersInitiallyEmpty() {
        assertTrue(session.getAdminUsers().isEmpty());
    }

    @Test
    void testAdminAuctionsInitiallyEmpty() {
        assertTrue(session.getAdminAuctions().isEmpty());
    }

    @Test
    void testAdminUsersCanBeModified() {
        User adminUser = new User(99, "USER", "admin_test");
        session.getAdminUsers().put(99, adminUser);
        assertSame(adminUser, session.getAdminUsers().get(99));
    }

    @Test
    void testAdminAuctionsCanBeModified() {
        Auction auction = mock(Auction.class);
        when(auction.getId()).thenReturn(1);
        session.getAdminAuctions().put(1, auction);
        assertSame(auction, session.getAdminAuctions().get(1));
    }

    @Test
    void testResetSessionClearsAdminMaps() {
        session.getAdminUsers().put(1, new User(1, "USER", "u1"));
        session.getAdminAuctions().put(1, mock(Auction.class));

        session.resetSession();

        assertTrue(session.getAdminUsers().isEmpty());
        assertTrue(session.getAdminAuctions().isEmpty());
    }

    @Test
    void testGetWonAuctionsInitiallyEmpty() {
        assertTrue(session.getWonAuctions().isEmpty());
    }

    @Test
    void testMultipleGetInstanceReturnsSame() {
        assertSame(session, ClientSession.getInstance());
    }

    @Test
    void testUserDefaults() {
        assertEquals(-1, session.getUser().getId());
        assertEquals("guest", session.getUser().getUsername());
        assertEquals("GUEST", session.getUser().getRole());
    }

    @Test
    void testSingletonConsistency() {
        ClientSession anotherRef = ClientSession.getInstance();
        anotherRef.setUser(new User(5, "ADMIN", "admin2"));
        assertSame(anotherRef.getUser(), session.getUser());
        session.setUser(new User(3, "USER", "user3"));
        assertSame(anotherRef.getUser(), session.getUser());
    }
}
