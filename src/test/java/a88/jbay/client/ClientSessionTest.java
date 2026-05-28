package a88.jbay.client;

import a88.jbay.common.auction.Auction;
import a88.jbay.common.user.User;
import javafx.collections.ObservableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientSessionTest {

    private ClientSession session;

    @BeforeEach
    void setUp() {
        session = ClientSession.getInstance();
        session.resetSession();
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
}
