package a88.jbay.system.update;

import a88.jbay.common.auction.Auction;
import a88.jbay.common.item.Item;
import a88.jbay.common.network.Response;
import a88.jbay.common.user.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Set;

import static org.mockito.Mockito.*;

class UpdateSystemTest {

    @Mock
    private ConnectionSystem connectionSystem;

    private UpdateSystem updateSystem;

    private Auction auction;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        updateSystem = new UpdateSystem(connectionSystem);

        Item item = new Item(1, "Test Item", "ELECTRONICS", "A test item", 100.0);
        UserData seller = new UserData(1, "seller1", "SELLER", "pass");
        auction = new Auction(1, item, seller, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        auction.subscribe(1);
        auction.subscribe(2);
    }

    @Test
    @DisplayName("Should notify auction subscribers")
    void testNotifyAuctionSubscribers() {
        updateSystem.notifyAuctionSubscribers(auction);

        verify(connectionSystem).sendToUsers(eq(Set.of(1, 2)), any(Response.class));
    }

    @Test
    @DisplayName("Should broadcast auction update to all")
    void testBroadcastAuctionUpdate() {
        updateSystem.broadcastAuctionUpdate(auction);

        verify(connectionSystem).broadcast(any(Response.class));
    }

    @Test
    @DisplayName("Should send response to specific user")
    void testSendToUser() {
        Response response = new Response(true, "TEST", "data");

        updateSystem.sendToUser(1, response);

        verify(connectionSystem).sendToUser(1, response);
    }

    @Test
    @DisplayName("Should send response to multiple users")
    void testSendToUsers() {
        Response response = new Response(true, "TEST", "data");
        Set<Integer> userIds = Set.of(1, 2, 3);

        updateSystem.sendToUsers(userIds, response);

        verify(connectionSystem).sendToUsers(userIds, response);
    }

    @Test
    @DisplayName("Should broadcast to all users")
    void testBroadcastToAll() {
        Response response = new Response(true, "ANNOUNCEMENT", "System maintenance");

        updateSystem.broadcastToAll(response);

        verify(connectionSystem).broadcast(response);
    }

    @Test
    @DisplayName("Should handle auction with no subscribers")
    void testNotifyAuctionSubscribers_NoSubscribers() {
        Auction emptyAuction;
        Item item = new Item(2, "No Sub", "TYPE", "Desc", 50.0);
        UserData seller = new UserData(3, "seller3", "SELLER", "pass");
        emptyAuction = new Auction(2, item, seller, LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        updateSystem.notifyAuctionSubscribers(emptyAuction);

        verify(connectionSystem).sendToUsers(eq(Set.of()), any(Response.class));
    }
}
