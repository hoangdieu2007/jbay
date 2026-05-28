package a88.jbay.server;

import a88.jbay.common.auction.Auction;
import a88.jbay.common.item.Item;
import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import a88.jbay.common.network.Response;
import a88.jbay.common.user.User;
import a88.jbay.system.AuctionSystem;
import a88.jbay.system.BidSystem;
import a88.jbay.system.update.ConnectionSystem;
import a88.jbay.system.update.UpdateSystem;
import a88.jbay.system.user.AdminService;
import a88.jbay.system.user.UserSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RequestHandlerTest {

    @Mock private UserSystem userSystem;
    @Mock private AdminService adminService;
    @Mock private AuctionSystem auctionSystem;
    @Mock private ConnectionSystem connectionSystem;
    @Mock private UpdateSystem updateSystem;
    @Mock private BidSystem bidSystem;

    private RequestHandler handler;
    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new RequestHandler(userSystem, adminService, auctionSystem,
                connectionSystem, updateSystem, bidSystem);
        testUser = new User(1, "USER", "testuser", "sess1");
        adminUser = new User(2, "ADMIN", "admin", "sess2");
    }

    @Test
    @DisplayName("Unauthenticated request without session returns PERMISSION_DENIED")
    void testUnauthenticatedRequest() {
        Request req = new Request(RequestType.BID);
        Response res = handler.handleRequest(req);
        assertFalse(res.isSuccess());
        assertEquals("PERMISSION_DENIED", res.getMessage());
    }

    @Test
    @DisplayName("PING is allowed without session")
    void testPing() {
        Request req = new Request(RequestType.PING);
        Response res = handler.handleRequest(req);
        assertTrue(res.isSuccess());
        assertEquals("PONG", res.getMessage());
    }

    @Test
    @DisplayName("LOGIN success returns user")
    void testLoginSuccess() {
        when(userSystem.login("alice", "pass")).thenReturn(testUser);

        Request req = new Request(RequestType.LOGIN)
                .put("username", "alice")
                .put("password", "pass");
        Response res = handler.handleRequest(req);

        assertTrue(res.isSuccess());
        assertEquals("LOGIN_SUCCESS", res.getMessage());
        assertSame(testUser, res.getPayload());
    }

    @Test
    @DisplayName("LOGIN with banned user returns BAN_USER")
    void testLoginBanned() {
        User banned = new User(3, "BAN", "banned", null);
        when(userSystem.login("banned", "pass")).thenReturn(banned);

        Request req = new Request(RequestType.LOGIN)
                .put("username", "banned")
                .put("password", "pass");
        Response res = handler.handleRequest(req);

        assertTrue(res.isSuccess());
        assertEquals("BAN_USER", res.getMessage());
    }

    @Test
    @DisplayName("LOGIN failure returns LOGIN_FAIL")
    void testLoginFail() {
        when(userSystem.login("alice", "wrong")).thenReturn(null);

        Request req = new Request(RequestType.LOGIN)
                .put("username", "alice")
                .put("password", "wrong");
        Response res = handler.handleRequest(req);

        assertFalse(res.isSuccess());
        assertEquals("LOGIN_FAIL", res.getMessage());
    }

    @Test
    @DisplayName("REGISTER broadcasts new user")
    void testRegister() {
        when(userSystem.register("newuser", "pass", "USER", null)).thenReturn(true);
        when(userSystem.getUserByName("newuser")).thenReturn(testUser);

        Request req = new Request(RequestType.REGISTER)
                .put("username", "newuser")
                .put("password", "pass");
        Response res = handler.handleRequest(req);

        assertTrue(res.isSuccess());
        assertEquals("REGISTER_SUCCESS", res.getMessage());
        verify(updateSystem).broadcastToAll(any(Response.class));
    }

    @Test
    @DisplayName("REGISTER failure returns REGISTER_FAIL")
    void testRegisterFail() {
        when(userSystem.register("newuser", "pass", "USER", null)).thenReturn(false);

        Request req = new Request(RequestType.REGISTER)
                .put("username", "newuser")
                .put("password", "pass");
        Response res = handler.handleRequest(req);

        assertFalse(res.isSuccess());
        assertEquals("REGISTER_FAIL", res.getMessage());
    }

    @Test
    @DisplayName("LOGOUT deletes session")
    void testLogout() {
        when(userSystem.findBySessionId("sess1")).thenReturn(testUser);

        Request req = new Request(RequestType.LOGOUT)
                .put("sessionId", "sess1");
        Response res = handler.handleRequest(req);

        assertTrue(res.isSuccess());
        assertEquals("LOGOUT_SUCCESS", res.getMessage());
        verify(userSystem).logout("sess1");
    }

    @Test
    @DisplayName("BID success")
    void testBidSuccess() {
        when(userSystem.findBySessionId("sess1")).thenReturn(testUser);
        when(bidSystem.placeBid(1, 100, 150.0)).thenReturn(true);

        Request req = new Request(RequestType.BID)
                .put("sessionId", "sess1")
                .put("auctionId", 100)
                .put("amount", 150.0);
        Response res = handler.handleRequest(req);

        assertTrue(res.isSuccess());
        assertEquals("BID_SUCCESS", res.getMessage());
    }

    @Test
    @DisplayName("BID failure")
    void testBidFail() {
        when(userSystem.findBySessionId("sess1")).thenReturn(testUser);
        when(bidSystem.placeBid(1, 100, 150.0)).thenReturn(false);

        Request req = new Request(RequestType.BID)
                .put("sessionId", "sess1")
                .put("auctionId", 100)
                .put("amount", 150.0);
        Response res = handler.handleRequest(req);

        assertFalse(res.isSuccess());
        assertEquals("BID_FAIL", res.getMessage());
    }

    @Test
    @DisplayName("BID by banned user returns PERMISSION_DENIED")
    void testBidByBannedUser() {
        User banned = new User(3, "BAN", "banned", "sessBan");
        when(userSystem.findBySessionId("sessBan")).thenReturn(banned);

        Request req = new Request(RequestType.BID)
                .put("sessionId", "sessBan")
                .put("auctionId", 100)
                .put("amount", 150.0);
        Response res = handler.handleRequest(req);

        assertFalse(res.isSuccess());
        assertEquals("PERMISSION_DENIED", res.getMessage());
    }

    @Test
    @DisplayName("SELL success")
    void testSellSuccess() {
        Item item = new Item(1, "Test", "ELECTRONICS", "desc", 100.0);
        when(userSystem.findBySessionId("sess1")).thenReturn(testUser);
        when(auctionSystem.createAuction(any(), anyInt(), anyDouble(), any(), any())).thenReturn(true);

        Request req = new Request(RequestType.SELL)
                .put("sessionId", "sess1")
                .put("item", item)
                .put("start", LocalDateTime.now())
                .put("end", LocalDateTime.now().plusDays(1));
        Response res = handler.handleRequest(req);

        assertTrue(res.isSuccess());
        assertEquals("SELL_SUCCESS", res.getMessage());
    }

    @Test
    @DisplayName("SELL with missing params returns SELL_FAIL")
    void testSellMissingParams() {
        when(userSystem.findBySessionId("sess1")).thenReturn(testUser);

        Request req = new Request(RequestType.SELL)
                .put("sessionId", "sess1");
        Response res = handler.handleRequest(req);

        assertFalse(res.isSuccess());
        assertEquals("SELL_FAIL", res.getMessage());
    }

    @Test
    @DisplayName("PAY returns QR")
    void testPay() {
        Auction auction = mock(Auction.class);
        when(auction.getSellerId()).thenReturn(2);
        when(userSystem.findBySessionId("sess1")).thenReturn(testUser);
        when(auctionSystem.getAuctionById(100)).thenReturn(auction);
        when(userSystem.getQr(2)).thenReturn(new byte[]{1, 2, 3});

        Request req = new Request(RequestType.PAY)
                .put("sessionId", "sess1")
                .put("auctionId", 100);
        Response res = handler.handleRequest(req);

        assertTrue(res.isSuccess());
        assertEquals("PAY_QR", res.getMessage());
    }

    @Test
    @DisplayName("CANCEL by seller succeeds")
    void testCancelBySeller() {
        Auction auction = mock(Auction.class);
        when(auction.getSellerName()).thenReturn("testuser");
        when(auction.getId()).thenReturn(100);
        when(userSystem.findBySessionId("sess1")).thenReturn(testUser);
        when(auctionSystem.getAuctionById(100)).thenReturn(auction);
        when(auctionSystem.cancelAuction(100)).thenReturn(true);

        Request req = new Request(RequestType.CANCEL)
                .put("sessionId", "sess1")
                .put("auctionId", 100);
        Response res = handler.handleRequest(req);

        assertTrue(res.isSuccess());
        assertEquals("CANCEL_SUCCESS", res.getMessage());
    }

    @Test
    @DisplayName("CANCEL by non-seller non-admin fails")
    void testCancelByUnauthorized() {
        Auction auction = mock(Auction.class);
        when(auction.getSellerName()).thenReturn("other");
        when(userSystem.findBySessionId("sess1")).thenReturn(testUser);
        when(auctionSystem.getAuctionById(100)).thenReturn(auction);

        Request req = new Request(RequestType.CANCEL)
                .put("sessionId", "sess1")
                .put("auctionId", 100);
        Response res = handler.handleRequest(req);

        assertFalse(res.isSuccess());
        assertEquals("CANCEL_FAIL", res.getMessage());
    }

    @Test
    @DisplayName("GET_AUCTIONS triggers updateAllAuctions for non-admin")
    void testGetAuctionsForUser() {
        when(userSystem.findBySessionId("sess1")).thenReturn(testUser);

        Request req = new Request(RequestType.GET_AUCTIONS)
                .put("sessionId", "sess1")
                .put("userId", 1);
        Response res = handler.handleRequest(req);

        assertTrue(res.isSuccess());
        verify(auctionSystem).updateAllAuctions(1);
    }

    @Test
    @DisplayName("GET_AUCTIONS triggers updateAdminAuctions for admin")
    void testGetAuctionsForAdmin() {
        when(userSystem.findBySessionId("sess2")).thenReturn(adminUser);

        Request req = new Request(RequestType.GET_AUCTIONS)
                .put("sessionId", "sess2")
                .put("userId", 2);
        Response res = handler.handleRequest(req);

        assertTrue(res.isSuccess());
        verify(auctionSystem).updateAdminAuctions(2);
    }

    @Test
    @DisplayName("GET_USERS returns user list for admin")
    void testGetUsersForAdmin() {
        List<User> users = List.of(testUser);
        when(userSystem.findBySessionId("sess2")).thenReturn(adminUser);
        when(userSystem.getAllNormalUsersForAdmin()).thenReturn(users);

        Request req = new Request(RequestType.GET_USERS)
                .put("sessionId", "sess2");
        Response res = handler.handleRequest(req);

        assertTrue(res.isSuccess());
        assertEquals("GET_USERS_SUCCESS", res.getMessage());
        verify(updateSystem).sendToUser(eq(2), any(Response.class));
    }

    @Test
    @DisplayName("GET_USERS returns PERMISSION_DENIED for non-admin")
    void testGetUsersForbidden() {
        when(userSystem.findBySessionId("sess1")).thenReturn(testUser);

        Request req = new Request(RequestType.GET_USERS)
                .put("sessionId", "sess1");
        Response res = handler.handleRequest(req);

        assertFalse(res.isSuccess());
        assertEquals("PERMISSION_DENIED", res.getMessage());
    }

    @Test
    @DisplayName("BAN user by admin")
    void testBanUser() {
        when(userSystem.findBySessionId("sess2")).thenReturn(adminUser);
        when(adminService.banUser(5)).thenReturn(new User(5, "BAN", "target", null));

        Request req = new Request(RequestType.BAN)
                .put("sessionId", "sess2")
                .put("userId", 5);
        Response res = handler.handleRequest(req);

        assertTrue(res.isSuccess());
        assertEquals("USER_STATE_CHANGED", res.getMessage());
        verify(updateSystem).broadcastToAll(any(Response.class));
    }

    @Test
    @DisplayName("UNBAN user by admin")
    void testUnbanUser() {
        when(userSystem.findBySessionId("sess2")).thenReturn(adminUser);
        when(adminService.unbanUser(5)).thenReturn(new User(5, "USER", "target", null));

        Request req = new Request(RequestType.BAN)
                .put("sessionId", "sess2")
                .put("userId", 5)
                .put("action", "UNBAN");
        Response res = handler.handleRequest(req);

        assertTrue(res.isSuccess());
        assertEquals("USER_STATE_CHANGED", res.getMessage());
        verify(adminService).unbanUser(5);
    }

    @Test
    @DisplayName("BAN by non-admin returns PERMISSION_DENIED")
    void testBanByNonAdmin() {
        when(userSystem.findBySessionId("sess1")).thenReturn(testUser);

        Request req = new Request(RequestType.BAN)
                .put("sessionId", "sess1")
                .put("userId", 5);
        Response res = handler.handleRequest(req);

        assertFalse(res.isSuccess());
        assertEquals("PERMISSION_DENIED", res.getMessage());
    }

    @Test
    @DisplayName("MISC ls-auction returns auction list")
    void testMiscListAuctions() {
        when(userSystem.findBySessionId("sess1")).thenReturn(testUser);
        when(auctionSystem.listActiveAuctions()).thenReturn("auction list");

        Request req = new Request(RequestType.MISC)
                .put("sessionId", "sess1")
                .put("command", "ls-auction");
        Response res = handler.handleRequest(req);

        assertTrue(res.isSuccess());
        assertEquals("LIST_AUCTION_SUCCESS", res.getMessage());
    }

    @Test
    @DisplayName("MISC invalid command returns error")
    void testMiscInvalidCommand() {
        when(userSystem.findBySessionId("sess1")).thenReturn(testUser);

        Request req = new Request(RequestType.MISC)
                .put("sessionId", "sess1")
                .put("command", "");
        Response res = handler.handleRequest(req);

        assertFalse(res.isSuccess());
        assertEquals("INVALID_MISC_COMMAND", res.getMessage());
    }

    @Test
    @DisplayName("Invalid session returns INVALID_SESSION")
    void testInvalidSession() {
        when(userSystem.findBySessionId("bad")).thenReturn(null);

        Request req = new Request(RequestType.BID)
                .put("sessionId", "bad")
                .put("auctionId", 100)
                .put("amount", 150.0);
        Response res = handler.handleRequest(req);

        assertFalse(res.isSuccess());
        assertEquals("INVALID_SESSION", res.getMessage());
    }

    @Test
    @DisplayName("AUTO_BID triggers auto bid")
    void testAutoBid() {
        when(userSystem.findBySessionId("sess1")).thenReturn(testUser);

        Request req = new Request(RequestType.AUTO_BID)
                .put("sessionId", "sess1")
                .put("auctionId", 100)
                .put("max_amount", 500.0)
                .put("increment", 10.0);
        Response res = handler.handleRequest(req);

        assertTrue(res.isSuccess());
        assertEquals("AUTO_BID_SUCCESS", res.getMessage());
        verify(bidSystem).placeBidAutomated(1, 100, 500.0, 10.0);
    }

    @Test
    @DisplayName("CANCEL_AUTO_BID cancels auto bid")
    void testCancelAutoBid() {
        when(userSystem.findBySessionId("sess1")).thenReturn(testUser);

        Request req = new Request(RequestType.CANCEL_AUTO_BID)
                .put("sessionId", "sess1")
                .put("auctionId", 100);
        Response res = handler.handleRequest(req);

        assertTrue(res.isSuccess());
        assertEquals("CANCEL_AUTO_BID_SUCCESS", res.getMessage());
        verify(bidSystem).cancelAutoBid(1, 100);
    }

    @Test
    @DisplayName("SUBSCRIBE_AUCTION works")
    void testSubscribeAuction() {
        Auction auction = mock(Auction.class);
        when(userSystem.findBySessionId("sess1")).thenReturn(testUser);
        when(auctionSystem.isAuctionActive(100)).thenReturn(true);
        when(auctionSystem.getAuctionById(100)).thenReturn(auction);

        Request req = new Request(RequestType.SUBSCRIBE_AUCTION)
                .put("sessionId", "sess1")
                .put("auctionId", 100);
        Response res = handler.handleRequest(req);

        assertTrue(res.isSuccess());
        assertEquals("SUBSCRIBE_AUCTION_SUCCESS", res.getMessage());
        verify(auction).subscribe(1);
    }

    @Test
    @DisplayName("UNSUBSCRIBE_AUCTION works")
    void testUnsubscribeAuction() {
        Auction auction = mock(Auction.class);
        when(userSystem.findBySessionId("sess1")).thenReturn(testUser);
        when(auctionSystem.getAuctionById(100)).thenReturn(auction);

        Request req = new Request(RequestType.UNSUBSCRIBE_AUCTION)
                .put("sessionId", "sess1")
                .put("auctionId", 100);
        Response res = handler.handleRequest(req);

        assertTrue(res.isSuccess());
        assertEquals("UNSUBSCRIBE_AUCTION_SUCCESS", res.getMessage());
        verify(auction).unsubscribe(1);
    }

    @Test
    @DisplayName("CONFIRM_PAYMENT by seller succeeds")
    void testConfirmPayment() {
        Auction auction = mock(Auction.class);
        when(auction.getSellerName()).thenReturn("testuser");
        when(auction.getId()).thenReturn(100);
        when(userSystem.findBySessionId("sess1")).thenReturn(testUser);
        when(auctionSystem.getAuctionById(100)).thenReturn(auction);
        when(auctionSystem.confirmPayment(100)).thenReturn(true);

        Request req = new Request(RequestType.CONFIRM_PAYMENT)
                .put("sessionId", "sess1")
                .put("auctionId", 100);
        Response res = handler.handleRequest(req);

        assertTrue(res.isSuccess());
        assertEquals("CONFIRM_PAYMENT_SUCCESS", res.getMessage());
    }
}
