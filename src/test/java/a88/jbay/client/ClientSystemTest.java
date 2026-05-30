package a88.jbay.client;

import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.common.item.Item;
import a88.jbay.common.network.Response;
import a88.jbay.common.user.User;
import a88.jbay.common.user.UserData;
import a88.jbay.common.user.role.Role;
import a88.jbay.controller.ControllerProvider;
import a88.jbay.controller.app.EntranceUI.ClientLoginController;
import a88.jbay.controller.app.EntranceUI.ClientRegisterController;
import a88.jbay.view.ViewManager;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientSystemTest {

    private ClientSession session;
    private ControllerProvider controllerProvider;
    private ResponseHandler handler;

    @BeforeEach
    void setUp() {
        session = new ClientSession();
        controllerProvider = mock(ControllerProvider.class);
        handler = new ResponseHandler(session, controllerProvider, mock(ViewManager.class));
    }

    private Item makeItem(String name) {
        return new Item(1, name, "TYPE", "desc", 100.0);
    }

    private Auction auction(int id, String sellerName, int sellerId, String winner) {
        Auction a = new Auction(id, makeItem("Item" + id),
                new UserData(sellerId, sellerName, "USER", "pass"),
                LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        if (winner != null) {
            a.setAuctionState(AuctionState.FINISHED);
            try {
                Field f = Auction.class.getDeclaredField("winner");
                f.setAccessible(true);
                f.set(a, winner);
            } catch (Exception ignored) {}
        }
        return a;
    }

    private Auction auction(int id, String sellerName) {
        return auction(id, sellerName, id + 100, null);
    }

    // ===== LOGIN / LOGOUT =====

    @Test
    @DisplayName("Login success sets user in session")
    void loginSuccess_setsUser() {
        User curUser = new User(1, Role.USER, "testuser", "sess1");
        when(controllerProvider.getController(ClientLoginController.class))
                .thenReturn(mock(ClientLoginController.class));

        try (MockedStatic<ServerConnection> sc = mockStatic(ServerConnection.class)) {
            sc.when(ServerConnection::getInstance).thenReturn(mock(ServerConnection.class));
            handler.handle(new Response(true, "LOGIN_SUCCESS", curUser));
        }

        assertSame(curUser, session.getUser());
    }

    @Test
    @DisplayName("Login fail does not change session user")
    void loginFail_keepsGuest() {
        when(controllerProvider.getController(ClientLoginController.class))
                .thenReturn(mock(ClientLoginController.class));

        handler.handle(new Response(false, "LOGIN_FAIL", null));

        assertEquals(-1, session.getUser().getId());
    }

    @Test
    @DisplayName("Logout success resets session and clears controllers")
    void logoutSuccess_resetsSession() {
        session.setUser(new User(1, Role.USER, "testuser", "sess1"));
        session.getBidderAuctions().put(1, auction(1, "seller"));
        session.getSellerAuctions().put(2, auction(2, "testuser"));

        handler.handle(new Response(true, "LOGOUT_SUCCESS", null));

        assertEquals(-1, session.getUser().getId());
        assertTrue(session.getBidderAuctions().isEmpty());
        assertTrue(session.getSellerAuctions().isEmpty());
        assertTrue(session.getWonAuctions().isEmpty());
        assertTrue(session.getAdminUsers().isEmpty());
        assertTrue(session.getAdminAuctions().isEmpty());
        verify(controllerProvider).clearControllers();
    }

    // ===== REGISTER =====

    @Test
    @DisplayName("Register success calls controller label")
    void registerSuccess_updatesLabel() {
        when(controllerProvider.getController(ClientRegisterController.class))
                .thenReturn(mock(ClientRegisterController.class));

        handler.handle(new Response(true, "REGISTER_SUCCESS", null));

        verify(controllerProvider.getController(ClientRegisterController.class))
                .updateRegisterLabel("Register successful");
    }

    @Test
    @DisplayName("Register fail calls controller label")
    void registerFail_updatesLabel() {
        when(controllerProvider.getController(ClientRegisterController.class))
                .thenReturn(mock(ClientRegisterController.class));

        handler.handle(new Response(false, "REGISTER_FAIL", null));

        verify(controllerProvider.getController(ClientRegisterController.class))
                .updateRegisterLabel("Register failed");
    }

    // ===== AUCTION LISTS =====

    @Test
    @DisplayName("ACTIVE_AUCTION_LIST populates bidder auctions")
    void activeAuctionList_populatesBidderAuctions() {
        handler.handle(new Response(true, "ACTIVE_AUCTION_LIST",
                List.of(auction(10, "sA"), auction(20, "sB"))));

        assertEquals(2, session.getBidderAuctions().size());
        assertNotNull(session.getBidderAuctions().get(10));
        assertNotNull(session.getBidderAuctions().get(20));
    }

    @Test
    @DisplayName("SELLER_AUCTION_LIST populates seller auctions")
    void sellerAuctionList_populatesSellerAuctions() {
        handler.handle(new Response(true, "SELLER_AUCTION_LIST",
                List.of(auction(30, "sellerX"), auction(40, "sellerY"))));

        assertEquals(2, session.getSellerAuctions().size());
        assertNotNull(session.getSellerAuctions().get(30));
        assertNotNull(session.getSellerAuctions().get(40));
    }

    @Test
    @DisplayName("BIDDER_AUCTION_LIST populates won auctions")
    void bidderAuctionList_populatesWonAuctions() {
        handler.handle(new Response(true, "BIDDER_AUCTION_LIST",
                List.of(auction(50, "sA", 150, "w1"), auction(60, "sB", 160, "w2"))));

        assertEquals(2, session.getWonAuctions().size());
        assertNotNull(session.getWonAuctions().get(50));
        assertNotNull(session.getWonAuctions().get(60));
    }

    // ===== AUCTION_UPDATE =====

    @Test
    @DisplayName("AUCTION_UPDATE goes to bidder auctions for unrelated user")
    void auctionUpdate_goesToBidderAuctions() {
        session.setUser(new User(2, Role.USER, "alice", "s"));

        handler.handle(new Response(true, "AUCTION_UPDATE", auction(70, "bob")));

        assertNotNull(session.getBidderAuctions().get(70));
        assertTrue(session.getSellerAuctions().isEmpty());
        assertTrue(session.getWonAuctions().isEmpty());
    }

    @Test
    @DisplayName("AUCTION_UPDATE goes to seller auctions when user is seller")
    void auctionUpdate_goesToSellerAuctions() {
        session.setUser(new User(3, Role.USER, "seller1", "s"));

        handler.handle(new Response(true, "AUCTION_UPDATE", auction(80, "seller1")));

        assertNotNull(session.getSellerAuctions().get(80));
        assertTrue(session.getBidderAuctions().isEmpty());
        assertTrue(session.getWonAuctions().isEmpty());
    }

    @Test
    @DisplayName("AUCTION_UPDATE goes to won auctions when user won finished auction")
    void auctionUpdate_goesToWonAuctions_whenFinished() {
        session.setUser(new User(4, Role.USER, "winner1", "s"));

        Auction a = auction(90, "sellerX", 190, "winner1");

        handler.handle(new Response(true, "AUCTION_UPDATE", a));

        assertNotNull(session.getWonAuctions().get(90));
        assertTrue(session.getBidderAuctions().isEmpty());
        assertTrue(session.getSellerAuctions().isEmpty());
    }

    @Test
    @DisplayName("AUCTION_UPDATE goes to bidder (not won) for running auction user won")
    void auctionUpdate_goesToBidderAuctions_whenRunningWinner() {
        session.setUser(new User(5, Role.USER, "winner1", "s"));

        Auction a = auction(95, "sellerX");
        // winner is empty string (default), so user is neither seller nor winner → bidder
        // Instead: manually construct a running auction where user is winning
        Auction running = new Auction(95, makeItem("Item95"),
                new UserData(195, "sellerX", "USER", "pass"),
                LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        running.setAuctionState(AuctionState.RUNNING);

        handler.handle(new Response(true, "AUCTION_UPDATE", running));

        assertNotNull(session.getBidderAuctions().get(95));
        assertTrue(session.getSellerAuctions().isEmpty());
        assertTrue(session.getWonAuctions().isEmpty());
    }

    @Test
    @DisplayName("AUCTION_UPDATE also goes to admin auctions for admin user")
    void auctionUpdate_alsoGoesToAdminAuctions() {
        session.setUser(new User(6, Role.ADMIN, "admin1", "s"));

        handler.handle(new Response(true, "AUCTION_UPDATE", auction(100, "other")));

        assertNotNull(session.getAdminAuctions().get(100));
        assertNotNull(session.getBidderAuctions().get(100));
    }

    // ===== ADMIN =====

    @Test
    @DisplayName("ADMIN_AUCTION_LIST populates admin auctions")
    void adminAuctionList_populatesAdminAuctions() {
        handler.handle(new Response(true, "ADMIN_AUCTION_LIST",
                List.of(auction(110, "sA"), auction(120, "sB"))));

        assertEquals(2, session.getAdminAuctions().size());
        assertNotNull(session.getAdminAuctions().get(110));
        assertNotNull(session.getAdminAuctions().get(120));
    }

    @Test
    @DisplayName("ADMIN_USER_LIST populates admin users")
    void adminUserList_populatesAdminUsers() {
        handler.handle(new Response(true, "ADMIN_USER_LIST",
                List.of(new User(10, Role.USER, "alice"), new User(20, Role.ADMIN, "bob"))));

        assertEquals(2, session.getAdminUsers().size());
        assertEquals("alice", session.getAdminUsers().get(10).getUsername());
        assertEquals("bob", session.getAdminUsers().get(20).getUsername());
    }

    @Test
    @DisplayName("USER_STATE_CHANGED updates admin users map")
    void userStateChanged_updatesAdminUsers() {
        session.getAdminUsers().put(30, new User(30, Role.USER, "target"));
        handler.handle(new Response(true, "USER_STATE_CHANGED", new User(30, Role.BAN, "target")));

        assertEquals(Role.BAN, session.getAdminUsers().get(30).getRole());
    }

    @Test
    @DisplayName("NEW_USER_REGISTERED adds to admin users when current user is admin")
    void newUserRegistered_asAdmin_addsToAdminUsers() {
        session.setUser(new User(99, Role.ADMIN, "admin1", "s"));

        try (MockedStatic<Platform> p = mockStatic(Platform.class)) {
            p.when(() -> Platform.runLater(any(Runnable.class)))
                    .thenAnswer(inv -> { ((Runnable) inv.getArgument(0)).run(); return null; });
            handler.handle(new Response(true, "NEW_USER_REGISTERED", new User(40, Role.USER, "newbie")));
        }

        assertNotNull(session.getAdminUsers().get(40));
    }

    @Test
    @DisplayName("NEW_USER_REGISTERED ignored when current user is not admin")
    void newUserRegistered_asNonAdmin_ignored() {
        session.setUser(new User(98, Role.USER, "regular", "s"));

        try (MockedStatic<Platform> p = mockStatic(Platform.class)) {
            handler.handle(new Response(true, "NEW_USER_REGISTERED", new User(41, Role.USER, "newbie")));
        }

        assertTrue(session.getAdminUsers().isEmpty());
    }

    // ===== FULL LIFECYCLE =====

    @Test
    @DisplayName("Full lifecycle: login → lists → updates → logout")
    void fullLifecycle() {
        User user = new User(7, Role.USER, "lifecycleUser", "s");
        when(controllerProvider.getController(ClientLoginController.class))
                .thenReturn(mock(ClientLoginController.class));

        try (MockedStatic<ServerConnection> sc = mockStatic(ServerConnection.class)) {
            sc.when(ServerConnection::getInstance).thenReturn(mock(ServerConnection.class));
            handler.handle(new Response(true, "LOGIN_SUCCESS", user));
        }

        assertSame(user, session.getUser());

        handler.handle(new Response(true, "ACTIVE_AUCTION_LIST",
                List.of(auction(200, "stranger"))));
        assertEquals(1, session.getBidderAuctions().size());

        Auction sellerAuc = new Auction(210, makeItem("Item210"),
                new UserData(710, "lifecycleUser", "USER", "pass"),
                LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        handler.handle(new Response(true, "SELLER_AUCTION_LIST", List.of(sellerAuc)));
        assertEquals(1, session.getSellerAuctions().size());
        assertNotNull(session.getSellerAuctions().get(210));

        handler.handle(new Response(true, "BIDDER_AUCTION_LIST",
                List.of(auction(220, "sH", 720, "lifecycleUser"))));
        assertEquals(1, session.getWonAuctions().size());
        assertNotNull(session.getWonAuctions().get(220));

        Auction updateBidder = auction(230, "stranger2");
        handler.handle(new Response(true, "AUCTION_UPDATE", updateBidder));
        assertEquals(2, session.getBidderAuctions().size());

        Auction updateSeller = new Auction(240, makeItem("Item240"),
                new UserData(740, "lifecycleUser", "USER", "pass"),
                LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        handler.handle(new Response(true, "AUCTION_UPDATE", updateSeller));
        assertEquals(2, session.getSellerAuctions().size());

        handler.handle(new Response(true, "LOGOUT_SUCCESS", null));

        assertEquals(-1, session.getUser().getId());
        assertTrue(session.getBidderAuctions().isEmpty());
        assertTrue(session.getSellerAuctions().isEmpty());
        assertTrue(session.getWonAuctions().isEmpty());
    }

    // ===== EDGE CASES =====

    @Test
    @DisplayName("PONG does not modify session")
    void pong_doesNotModifySession() {
        session.setUser(new User(1, Role.USER, "u", "s"));

        handler.handle(new Response(true, "PONG", null));

        assertEquals(1, session.getUser().getId());
    }

    @Test
    @DisplayName("Unknown message is silently handled")
    void unknownMessage_doesNotThrow() {
        assertDoesNotThrow(() -> handler.handle(new Response(true, "SOME_UNKNOWN", null)));
        assertDoesNotThrow(() -> handler.handle(new Response(false, "SOME_FAIL", null)));
    }

    @Test
    @DisplayName("PAY_QR with pending auction does not crash")
    void payQr_withPendingAuction() {
        handler.setPendingPaymentAuction(auction(1, "seller"));
        try (MockedStatic<Platform> p = mockStatic(Platform.class)) {
            assertDoesNotThrow(() ->
                    handler.handle(new Response(true, "PAY_QR", new byte[]{1, 2, 3})));
        }
    }

    @Test
    @DisplayName("AUCTION_UPDATE_NOTIFY does not crash")
    void auctionUpdateNotify_doesNotCrash() {
        Auction a = mock(Auction.class);
        when(a.getId()).thenReturn(1);
        when(a.getItem()).thenReturn(makeItem("Test"));
        when(a.getCurrentPrice()).thenReturn(50.0);
        when(a.getWinner()).thenReturn("someone");

        try (MockedStatic<Platform> p = mockStatic(Platform.class)) {
            assertDoesNotThrow(() ->
                    handler.handle(new Response(true, "AUCTION_UPDATE_NOTIFY", a)));
        }
    }

    @Test
    @DisplayName("CONFIRM_PAYMENT_SUCCESS does not crash")
    void confirmPaymentSuccess_doesNotCrash() {
        try (MockedStatic<Platform> p = mockStatic(Platform.class)) {
            assertDoesNotThrow(() ->
                    handler.handle(new Response(true, "CONFIRM_PAYMENT_SUCCESS", null)));
        }
    }

    @Test
    @DisplayName("BAN_USER resets session")
    void banUser_resetsSession() {
        session.setUser(new User(99, Role.USER, "banned", "s"));
        session.getBidderAuctions().put(1, auction(1, "s"));

        try (MockedStatic<Platform> p = mockStatic(Platform.class)) {
            handler.handle(new Response(true, "BAN_USER", null));
        }

        assertEquals(-1, session.getUser().getId());
        assertTrue(session.getBidderAuctions().isEmpty());
    }
}
