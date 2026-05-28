package a88.jbay.common.user;

import a88.jbay.common.auction.Auction;
import a88.jbay.common.network.RequestType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testDefaultConstructor() {
        User user = new User();
        assertEquals(-1, user.getId());
        assertEquals("GUEST", user.getRole());
        assertEquals("guest", user.getUsername());
        assertNull(user.getSessionId());
    }

    @Test
    void testFullConstructor() {
        User user = new User(5, "ADMIN", "admin1", "sess123");
        assertEquals(5, user.getId());
        assertEquals("ADMIN", user.getRole());
        assertEquals("admin1", user.getUsername());
        assertEquals("sess123", user.getSessionId());
    }

    @Test
    void testConstructorWithoutSession() {
        User user = new User(3, "USER", "bidder");
        assertEquals(3, user.getId());
        assertEquals("USER", user.getRole());
        assertEquals("bidder", user.getUsername());
        assertNull(user.getSessionId());
    }

    @Test
    void testCanMethod() {
        User admin = new User(1, "ADMIN", "admin");
        User user = new User(2, "USER", "normal");
        User banned = new User(3, "BAN", "banned");

        assertTrue(admin.can(RequestType.BAN));
        assertTrue(admin.can(RequestType.BID));
        assertTrue(admin.can(RequestType.SELL));

        assertTrue(user.can(RequestType.BID));
        assertTrue(user.can(RequestType.SELL));
        assertFalse(user.can(RequestType.BAN));

        assertFalse(banned.can(RequestType.BID));
        assertFalse(banned.can(RequestType.SELL));
        assertFalse(banned.can(RequestType.BAN));
    }

    @Test
    void testToString() {
        User user = new User(5, "ADMIN", "admin1");
        assertEquals("5 admin1 ADMIN", user.toString());
    }

    @Test
    void testUpdate() {
        User user = new User(1, "USER", "test");
        a88.jbay.common.item.Item item = new a88.jbay.common.item.Item(1, "Test", "TYPE", "desc", 100.0);
        Auction auction = new Auction(1, item, new UserData(2, "seller", "USER", "pass"),
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now().plusDays(1));
        user.update(auction);
    }
}
