package a88.jbay.server;

import a88.jbay.common.auction.Auction;
import a88.jbay.common.item.Item;
import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import a88.jbay.common.network.Response;
import a88.jbay.common.user.User;
import a88.jbay.common.user.role.Role;
import a88.jbay.system.AuctionSystem;
import a88.jbay.system.BidSystem;
import a88.jbay.system.update.ConnectionSystem;
import a88.jbay.system.update.UpdateSystem;
import a88.jbay.system.user.AdminService;
import a88.jbay.system.user.UserSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Extended edge-case tests for RequestHandler.
 * Covers boundary values, missing params, type mismatches, and error flows
 * not tested in the base RequestHandlerTest.
 */
class RequestHandlerExtendedTest {

    @Mock private UserSystem userSystem;
    @Mock private AdminService adminService;
    @Mock private AuctionSystem auctionSystem;
    @Mock private ConnectionSystem connectionSystem;
    @Mock private UpdateSystem updateSystem;
    @Mock private BidSystem bidSystem;

    private RequestHandler handler;
    private User testUser;
    private User adminUser;
    private User bannedUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new RequestHandler(userSystem, adminService, auctionSystem,
                connectionSystem, updateSystem, bidSystem);
        testUser = new User(1, Role.USER, "testuser", "sess1");
        adminUser = new User(2, Role.ADMIN, "admin", "sess2");
        bannedUser = new User(3, Role.BAN, "banned", "sessBan");
    }

    @Nested
    @DisplayName("BID edge cases")
    class BidEdgeCases {

        @Test
        @DisplayName("BID with null auctionId throws NPE (handler unboxing)")
        void bidWithNullAuctionId() {
            when(userSystem.findBySessionId("sess1")).thenReturn(testUser);

            Request req = new Request(RequestType.BID)
                    .put("sessionId", "sess1")
                    .put("amount", 150.0);
            assertThrows(NullPointerException.class, () -> handler.handleRequest(req));
        }

        @Test
        @DisplayName("BID with null amount throws NPE (handler unboxing)")
        void bidWithNullAmount() {
            when(userSystem.findBySessionId("sess1")).thenReturn(testUser);

            Request req = new Request(RequestType.BID)
                    .put("sessionId", "sess1")
                    .put("auctionId", 100);
            assertThrows(NullPointerException.class, () -> handler.handleRequest(req));
        }

        @Test
        @DisplayName("BID with zero amount triggers NPE-safe fail")
        void bidWithZeroAmount() {
            when(userSystem.findBySessionId("sess1")).thenReturn(testUser);
            when(bidSystem.placeBid(1, 100, 0.0)).thenReturn(false);

            Request req = new Request(RequestType.BID)
                    .put("sessionId", "sess1")
                    .put("auctionId", 100)
                    .put("amount", 0.0);
            Response res = handler.handleRequest(req);

            assertFalse(res.isSuccess());
            assertEquals("BID_FAIL", res.getMessage());
        }

        @Test
        @DisplayName("BID with negative amount")
        void bidWithNegativeAmount() {
            when(userSystem.findBySessionId("sess1")).thenReturn(testUser);
            when(bidSystem.placeBid(1, 100, -50.0)).thenReturn(false);

            Request req = new Request(RequestType.BID)
                    .put("sessionId", "sess1")
                    .put("auctionId", 100)
                    .put("amount", -50.0);
            Response res = handler.handleRequest(req);

            assertFalse(res.isSuccess());
            assertEquals("BID_FAIL", res.getMessage());
        }

        @Test
        @DisplayName("BID with NaN amount throws or fails gracefully")
        void bidWithNaN() {
            when(userSystem.findBySessionId("sess1")).thenReturn(testUser);
            when(bidSystem.placeBid(1, 100, Double.NaN)).thenReturn(false);

            Request req = new Request(RequestType.BID)
                    .put("sessionId", "sess1")
                    .put("auctionId", 100)
                    .put("amount", Double.NaN);
            Response res = handler.handleRequest(req);

            assertFalse(res.isSuccess());
            assertEquals("BID_FAIL", res.getMessage());
        }

        @Test
        @DisplayName("BID by banned user returns PERMISSION_DENIED")
        void bidByBannedUser() {
            when(userSystem.findBySessionId("sessBan")).thenReturn(bannedUser);

            Request req = new Request(RequestType.BID)
                    .put("sessionId", "sessBan")
                    .put("auctionId", 100)
                    .put("amount", 150.0);
            Response res = handler.handleRequest(req);

            assertFalse(res.isSuccess());
            assertEquals("PERMISSION_DENIED", res.getMessage());
        }
    }

    @Nested
    @DisplayName("SELL edge cases")
    class SellEdgeCases {

        @Test
        @DisplayName("SELL with null item returns SELL_FAIL")
        void sellWithNullItem() {
            when(userSystem.findBySessionId("sess1")).thenReturn(testUser);

            Request req = new Request(RequestType.SELL)
                    .put("sessionId", "sess1")
                    .put("start", LocalDateTime.now())
                    .put("end", LocalDateTime.now().plusDays(1));
            Response res = handler.handleRequest(req);

            assertFalse(res.isSuccess());
            assertEquals("SELL_FAIL", res.getMessage());
        }

        @Test
        @DisplayName("SELL with null start time returns SELL_FAIL")
        void sellWithNullStart() {
            when(userSystem.findBySessionId("sess1")).thenReturn(testUser);

            Item item = new Item("Laptop", "ELECTRONICS", "desc", 500.0, new byte[]{});
            Request req = new Request(RequestType.SELL)
                    .put("sessionId", "sess1")
                    .put("item", item)
                    .put("end", LocalDateTime.now().plusDays(1));
            Response res = handler.handleRequest(req);

            assertFalse(res.isSuccess());
            assertEquals("SELL_FAIL", res.getMessage());
        }

        @Test
        @DisplayName("SELL with null end time returns SELL_FAIL")
        void sellWithNullEnd() {
            when(userSystem.findBySessionId("sess1")).thenReturn(testUser);

            Item item = new Item("Laptop", "ELECTRONICS", "desc", 500.0, new byte[]{});
            Request req = new Request(RequestType.SELL)
                    .put("sessionId", "sess1")
                    .put("item", item)
                    .put("start", LocalDateTime.now());
            Response res = handler.handleRequest(req);

            assertFalse(res.isSuccess());
            assertEquals("SELL_FAIL", res.getMessage());
        }

        @Test
        @DisplayName("SELL with BAN role returns PERMISSION_DENIED")
        void sellByBannedRole() {
            User banned = new User(4, Role.BAN, "banned", "sessBan");
            when(userSystem.findBySessionId("sessBan")).thenReturn(banned);

            Item item = new Item("Laptop", "ELECTRONICS", "desc", 500.0, new byte[]{});
            Request req = new Request(RequestType.SELL)
                    .put("sessionId", "sessBan")
                    .put("item", item)
                    .put("start", LocalDateTime.now())
                    .put("end", LocalDateTime.now().plusDays(1));
            Response res = handler.handleRequest(req);

            assertFalse(res.isSuccess());
            assertEquals("PERMISSION_DENIED", res.getMessage());
        }

        @Test
        @DisplayName("SELL with end before start produces system-level fail")
        void sellEndBeforeStart() {
            when(userSystem.findBySessionId("sess1")).thenReturn(testUser);
            when(auctionSystem.createAuction(any(), anyInt(), anyDouble(), any(), any())).thenReturn(false);

            Item item = new Item("Laptop", "ELECTRONICS", "desc", 500.0, new byte[]{});
            Request req = new Request(RequestType.SELL)
                    .put("sessionId", "sess1")
                    .put("item", item)
                    .put("start", LocalDateTime.now().plusDays(1))
                    .put("end", LocalDateTime.now());
            Response res = handler.handleRequest(req);

            assertFalse(res.isSuccess());
            assertEquals("SELL_FAIL", res.getMessage());
        }
    }

    @Nested
    @DisplayName("AUTO_BID / CANCEL_AUTO_BID edge cases")
    class AutoBidEdgeCases {

        @Test
        @DisplayName("AUTO_BID with null max_amount throws NPE (handler unboxing)")
        void autoBidNullMaxAmount() {
            when(userSystem.findBySessionId("sess1")).thenReturn(testUser);

            Request req = new Request(RequestType.AUTO_BID)
                    .put("sessionId", "sess1")
                    .put("auctionId", 100);
            assertThrows(NullPointerException.class, () -> handler.handleRequest(req));
        }

        @Test
        @DisplayName("AUTO_BID by banned user returns PERMISSION_DENIED")
        void autoBidByBanned() {
            when(userSystem.findBySessionId("sessBan")).thenReturn(bannedUser);

            Request req = new Request(RequestType.AUTO_BID)
                    .put("sessionId", "sessBan")
                    .put("auctionId", 100)
                    .put("max_amount", 500.0)
                    .put("increment", 10.0);
            Response res = handler.handleRequest(req);

            assertFalse(res.isSuccess());
            assertEquals("PERMISSION_DENIED", res.getMessage());
        }

        @Test
        @DisplayName("CANCEL_AUTO_BID with null auctionId throws NPE (handler unboxing)")
        void cancelAutoBidNullAuction() {
            when(userSystem.findBySessionId("sess1")).thenReturn(testUser);

            Request req = new Request(RequestType.CANCEL_AUTO_BID)
                    .put("sessionId", "sess1");
            assertThrows(NullPointerException.class, () -> handler.handleRequest(req));
        }
    }

    @Nested
    @DisplayName("SUBSCRIBE / UNSUBSCRIBE edge cases")
    class SubscribeEdgeCases {

        @Test
        @DisplayName("SUBSCRIBE to non-existent auction returns AUCTION_NOT_FOUND")
        void subscribeNonExistentAuction() {
            when(userSystem.findBySessionId("sess1")).thenReturn(testUser);
            when(auctionSystem.isAuctionActive(999)).thenReturn(false);

            Request req = new Request(RequestType.SUBSCRIBE_AUCTION)
                    .put("sessionId", "sess1")
                    .put("auctionId", 999);
            Response res = handler.handleRequest(req);

            assertFalse(res.isSuccess());
            assertEquals("AUCTION_NOT_FOUND", res.getMessage());
        }

        @Test
        @DisplayName("SUBSCRIBE with null auctionId returns AUCTION_NOT_FOUND")
        void subscribeNullAuctionId() {
            when(userSystem.findBySessionId("sess1")).thenReturn(testUser);

            Request req = new Request(RequestType.SUBSCRIBE_AUCTION)
                    .put("sessionId", "sess1");
            Response res = handler.handleRequest(req);

            assertFalse(res.isSuccess());
            assertEquals("AUCTION_NOT_FOUND", res.getMessage());
        }

        @Test
        @DisplayName("UNSUBSCRIBE with null auctionId returns INVALID_AUCTION")
        void unsubscribeNullAuctionId() {
            when(userSystem.findBySessionId("sess1")).thenReturn(testUser);

            Request req = new Request(RequestType.UNSUBSCRIBE_AUCTION)
                    .put("sessionId", "sess1");
            Response res = handler.handleRequest(req);

            assertFalse(res.isSuccess());
            assertEquals("INVALID_AUCTION", res.getMessage());
        }
    }

    @Nested
    @DisplayName("PAY / CONFIRM_PAYMENT edge cases")
    class PayEdgeCases {

        @Test
        @DisplayName("PAY with null auctionId throws NPE (handler unboxing)")
        void payNullAuctionId() {
            when(userSystem.findBySessionId("sess1")).thenReturn(testUser);

            Request req = new Request(RequestType.PAY)
                    .put("sessionId", "sess1");
            assertThrows(NullPointerException.class, () -> handler.handleRequest(req));
        }

        @Test
        @DisplayName("PAY for non-existent auction returns PAY_FAIL")
        void payNonExistentAuction() {
            when(userSystem.findBySessionId("sess1")).thenReturn(testUser);
            when(auctionSystem.getAuctionById(999)).thenReturn(null);

            Request req = new Request(RequestType.PAY)
                    .put("sessionId", "sess1")
                    .put("auctionId", 999);
            Response res = handler.handleRequest(req);

            assertFalse(res.isSuccess());
            assertEquals("PAY_FAIL", res.getMessage());
        }

        @Test
        @DisplayName("CONFIRM_PAYMENT for non-existent auction returns INVALID_AUCTION")
        void confirmPaymentInvalidAuction() {
            when(userSystem.findBySessionId("sess1")).thenReturn(testUser);
            when(auctionSystem.getAuctionById(999)).thenReturn(null);

            Request req = new Request(RequestType.CONFIRM_PAYMENT)
                    .put("sessionId", "sess1")
                    .put("auctionId", 999);
            Response res = handler.handleRequest(req);

            assertFalse(res.isSuccess());
            assertEquals("INVALID_AUCTION", res.getMessage());
        }

        @Test
        @DisplayName("CONFIRM_PAYMENT by non-seller returns CONFIRM_PAYMENT_FAIL")
        void confirmPaymentByNonSeller() {
            Auction auction = mock(Auction.class);
            when(auction.getSellerName()).thenReturn("realSeller");
            when(userSystem.findBySessionId("sess1")).thenReturn(testUser);
            when(auctionSystem.getAuctionById(100)).thenReturn(auction);

            Request req = new Request(RequestType.CONFIRM_PAYMENT)
                    .put("sessionId", "sess1")
                    .put("auctionId", 100);
            Response res = handler.handleRequest(req);

            assertFalse(res.isSuccess());
            assertEquals("CONFIRM_PAYMENT_FAIL", res.getMessage());
        }
    }

    @Nested
    @DisplayName("CANCEL edge cases")
    class CancelEdgeCases {

        @Test
        @DisplayName("CANCEL for non-existent auction throws NPE (handler dereference)")
        void cancelNonExistentAuction() {
            when(userSystem.findBySessionId("sess1")).thenReturn(testUser);
            when(auctionSystem.getAuctionById(999)).thenReturn(null);

            Request req = new Request(RequestType.CANCEL)
                    .put("sessionId", "sess1")
                    .put("auctionId", 999);
            assertThrows(NullPointerException.class, () -> handler.handleRequest(req));
        }

        @Test
        @DisplayName("CANCEL by non-seller non-admin returns CANCEL_FAIL")
        void cancelByUnauthorized() {
            Auction auction = mock(Auction.class);
            when(auction.getSellerName()).thenReturn("otherUser");
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
        @DisplayName("CANCEL by admin succeeds even when not seller")
        void cancelByAdmin() {
            Auction auction = mock(Auction.class);
            when(auction.getSellerName()).thenReturn("otherUser");
            when(auction.getId()).thenReturn(100);
            when(auctionSystem.getAuctionById(100)).thenReturn(auction);
            when(auctionSystem.cancelAuction(100)).thenReturn(true);
            when(userSystem.findBySessionId("sess2")).thenReturn(adminUser);

            Request req = new Request(RequestType.CANCEL)
                    .put("sessionId", "sess2")
                    .put("auctionId", 100);
            Response res = handler.handleRequest(req);

            assertNotNull(res);
            assertTrue(res.isSuccess(),
                    "Admin cancel should succeed, got: " + res.getMessage());
            assertEquals("CANCEL_SUCCESS", res.getMessage());
        }
    }

    @Nested
    @DisplayName("BAN edge cases")
    class BanEdgeCases {

        @Test
        @DisplayName("BAN non-existent user returns BAN_FAIL")
        void banNonExistentUser() {
            when(userSystem.findBySessionId("sess2")).thenReturn(adminUser);
            when(adminService.banUser(999)).thenReturn(null);

            Request req = new Request(RequestType.BAN)
                    .put("sessionId", "sess2")
                    .put("userId", 999);
            Response res = handler.handleRequest(req);

            assertFalse(res.isSuccess());
            assertEquals("BAN_FAIL", res.getMessage());
        }

        @Test
        @DisplayName("BAN without action defaults to BAN")
        void banDefaultAction() {
            when(userSystem.findBySessionId("sess2")).thenReturn(adminUser);
            when(adminService.banUser(5)).thenReturn(new User(5, Role.BAN, "target", null));

            Request req = new Request(RequestType.BAN)
                    .put("sessionId", "sess2")
                    .put("userId", 5);
            Response res = handler.handleRequest(req);

            assertTrue(res.isSuccess());
            verify(adminService).banUser(5);
        }

        @Test
        @DisplayName("BAN self returns BAN_FAIL")
        void banSelf() {
            User selfAdmin = new User(2, Role.ADMIN, "admin", "sess2");
            when(userSystem.findBySessionId("sess2")).thenReturn(selfAdmin);
            when(adminService.banUser(2)).thenReturn(null);

            Request req = new Request(RequestType.BAN)
                    .put("sessionId", "sess2")
                    .put("userId", 2);
            Response res = handler.handleRequest(req);

            assertFalse(res.isSuccess());
        }
    }

    @Nested
    @DisplayName("GET_AUCTIONS / GET_USERS edge cases")
    class GetEdgeCases {

        @Test
        @DisplayName("GET_AUCTIONS with invalid session returns INVALID_SESSION")
        void getAuctionsInvalidSession() {
            when(userSystem.findBySessionId("bad")).thenReturn(null);

            Request req = new Request(RequestType.GET_AUCTIONS)
                    .put("sessionId", "bad")
                    .put("userId", 1);
            Response res = handler.handleRequest(req);

            assertFalse(res.isSuccess());
            assertEquals("INVALID_SESSION", res.getMessage());
        }

        @Test
        @DisplayName("GET_USERS by non-admin returns PERMISSION_DENIED")
        void getUsersByNonAdmin() {
            when(userSystem.findBySessionId("sess1")).thenReturn(testUser);

            Request req = new Request(RequestType.GET_USERS)
                    .put("sessionId", "sess1");
            Response res = handler.handleRequest(req);

            assertFalse(res.isSuccess());
            assertEquals("PERMISSION_DENIED", res.getMessage());
        }

        @Test
        @DisplayName("GET_USERS by admin returns GET_USERS_SUCCESS")
        void getUsersByAdmin() {
            when(userSystem.findBySessionId("sess2")).thenReturn(adminUser);
            when(userSystem.getAllNormalUsersForAdmin()).thenReturn(List.of());

            Request req = new Request(RequestType.GET_USERS)
                    .put("sessionId", "sess2");
            Response res = handler.handleRequest(req);

            assertTrue(res.isSuccess());
            assertEquals("GET_USERS_SUCCESS", res.getMessage());
            verify(updateSystem).sendToUser(eq(2), any(Response.class));
        }
    }

    @Nested
    @DisplayName("MISC / general edge cases")
    class MiscEdgeCases {

        @Test
        @DisplayName("MISC disconnect command interrupts thread")
        void miscDisconnect() {
            when(userSystem.findBySessionId("sess1")).thenReturn(testUser);

            Request req = new Request(RequestType.MISC)
                    .put("sessionId", "sess1")
                    .put("command", "disconnect");
            Response res = handler.handleRequest(req);

            assertNull(res, "disconnect should return null and interrupt");
        }

        @Test
        @DisplayName("MISC unknown command returns INVALID_MISC_COMMAND")
        void miscUnknownCommand() {
            when(userSystem.findBySessionId("sess1")).thenReturn(testUser);

            Request req = new Request(RequestType.MISC)
                    .put("sessionId", "sess1")
                    .put("command", "nonexistent");
            Response res = handler.handleRequest(req);

            assertFalse(res.isSuccess());
            assertEquals("INVALID_MISC_COMMAND", res.getMessage());
        }

        @Test
        @DisplayName("MISC empty command returns INVALID_MISC_COMMAND")
        void miscEmptyCommand() {
            when(userSystem.findBySessionId("sess1")).thenReturn(testUser);

            Request req = new Request(RequestType.MISC)
                    .put("sessionId", "sess1")
                    .put("command", "");
            Response res = handler.handleRequest(req);

            assertFalse(res.isSuccess());
            assertEquals("INVALID_MISC_COMMAND", res.getMessage());
        }

        @Test
        @DisplayName("Unknown request type returns PERMISSION_DENIED when unauthenticated")
        void unknownTypeUnauthenticated() {
            Request req = new Request(RequestType.MISC);
            Response res = handler.handleRequest(req);

            assertFalse(res.isSuccess());
            assertEquals("PERMISSION_DENIED", res.getMessage());
        }

        @Test
        @DisplayName("Double session lookup same result")
        void doubleLoginSimulated() {
            when(userSystem.findBySessionId("sess1")).thenReturn(testUser);
            when(bidSystem.placeBid(1, 100, 200.0)).thenReturn(true);

            Request req1 = new Request(RequestType.BID)
                    .put("sessionId", "sess1")
                    .put("auctionId", 100)
                    .put("amount", 200.0);
            Response res1 = handler.handleRequest(req1);
            assertTrue(res1.isSuccess());

            Request req2 = new Request(RequestType.BID)
                    .put("sessionId", "sess1")
                    .put("auctionId", 101)
                    .put("amount", 300.0);
            when(bidSystem.placeBid(1, 101, 300.0)).thenReturn(true);
            Response res2 = handler.handleRequest(req2);
            assertTrue(res2.isSuccess());
        }
    }
}
