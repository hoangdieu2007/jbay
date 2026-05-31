package a88.jbay.server;

import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import a88.jbay.common.network.Response;
import a88.jbay.common.auction.Auction;
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
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Stress and throughput tests for RequestHandler.
 * Verifies the handler can process many requests sequentially and concurrently
 * without race conditions or exceptions.
 */
class RequestHandlerStressTest {

    @Mock private UserSystem userSystem;
    @Mock private AdminService adminService;
    @Mock private AuctionSystem auctionSystem;
    @Mock private ConnectionSystem connectionSystem;
    @Mock private UpdateSystem updateSystem;
    @Mock private BidSystem bidSystem;

    private RequestHandler handler;
    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new RequestHandler(userSystem, adminService, auctionSystem,
                connectionSystem, updateSystem, bidSystem);
        testUser = new User(1, Role.USER, "testuser", "sess1");

        when(userSystem.findBySessionId("sess1")).thenReturn(testUser);
        when(bidSystem.placeBid(anyInt(), anyInt(), anyDouble())).thenReturn(true);
        when(auctionSystem.createAuction(any(), anyInt(), anyDouble(), any(), any())).thenReturn(true);
        when(auctionSystem.getAuctionById(anyInt())).thenReturn(mock(Auction.class));
    }

    @Test
    @DisplayName("500 sequential BID requests")
    void sequentialBidStress() {
        for (int i = 0; i < 500; i++) {
            Request req = new Request(RequestType.BID)
                    .put("sessionId", "sess1")
                    .put("auctionId", i % 100)
                    .put("amount", 100.0 + i);
            Response res = handler.handleRequest(req);
            assertTrue(res.isSuccess());
            assertEquals("BID_SUCCESS", res.getMessage());
        }
        verify(bidSystem, times(500)).placeBid(anyInt(), anyInt(), anyDouble());
    }

    @Test
    @DisplayName("1000 interleaved requests of mixed types")
    void mixedRequestStress() {
        for (int i = 0; i < 1000; i++) {
            Response res;
            switch (i % 5) {
                case 0 -> {
                    Request req = new Request(RequestType.BID)
                            .put("sessionId", "sess1")
                            .put("auctionId", i)
                            .put("amount", 100.0 + i);
                    res = handler.handleRequest(req);
                    assertTrue(res.isSuccess(), "BID should succeed at iter " + i);
                }
                case 1 -> {
                    Request req = new Request(RequestType.PING);
                    res = handler.handleRequest(req);
                    assertTrue(res.isSuccess());
                    assertEquals("PONG", res.getMessage());
                }
                case 2 -> {
                    Request req = new Request(RequestType.GET_AUCTIONS)
                            .put("sessionId", "sess1")
                            .put("userId", 1);
                    res = handler.handleRequest(req);
                    assertTrue(res.isSuccess());
                }
                case 3 -> {
                    Request req = new Request(RequestType.LOGOUT)
                            .put("sessionId", "sess1");
                    res = handler.handleRequest(req);
                    assertTrue(res.isSuccess());
                }
                case 4 -> {
                    Request req = new Request(RequestType.MISC)
                            .put("sessionId", "sess1")
                            .put("command", "ls-auction");
                    res = handler.handleRequest(req);
                    assertTrue(res.isSuccess());
                }
            }
        }
    }

    @Test
    @DisplayName("50 concurrent sessions sending BID requests")
    void concurrentSessionsBidStress() throws Exception {
        int numSessions = 50;
        int requestsPerSession = 20;
        User[] users = new User[numSessions];
        for (int i = 0; i < numSessions; i++) {
            users[i] = new User(100 + i, Role.USER, "user" + i, "sess" + i);
            when(userSystem.findBySessionId("sess" + i)).thenReturn(users[i]);
        }

        AtomicInteger successCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numSessions; i++) {
            final int sessionIndex = i;
            futures.add(executor.submit(() -> {
                for (int j = 0; j < requestsPerSession; j++) {
                    Request req = new Request(RequestType.BID)
                            .put("sessionId", "sess" + sessionIndex)
                            .put("auctionId", j)
                            .put("amount", 50.0 + j);
                    Response res = handler.handleRequest(req);
                    if (res != null && res.isSuccess()) {
                        successCount.incrementAndGet();
                    }
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertEquals(numSessions * requestsPerSession, successCount.get(),
                "All concurrent BID requests should succeed");
        verify(bidSystem, times(numSessions * requestsPerSession))
                .placeBid(anyInt(), anyInt(), anyDouble());
    }

    @Test
    @DisplayName("100 concurrent mixed request type stress")
    void concurrentMixedStress() throws Exception {
        int numClients = 100;
        int requestsPerClient = 30;
        User[] users = new User[numClients];
        for (int i = 0; i < numClients; i++) {
            Role role = i % 3 == 0 ? Role.ADMIN : Role.USER;
            users[i] = new User(200 + i, role, "user" + i, "sessMix" + i);
            when(userSystem.findBySessionId("sessMix" + i)).thenReturn(users[i]);
        }
        when(userSystem.getAllNormalUsersForAdmin()).thenReturn(List.of());
        when(auctionSystem.listActiveAuctions()).thenReturn("auctions");
        doNothing().when(auctionSystem).updateAllAuctions(anyInt());
        doNothing().when(auctionSystem).updateAdminAuctions(anyInt());
        doNothing().when(updateSystem).sendToUser(anyInt(), any());

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numClients; i++) {
            final int clientIdx = i;
            futures.add(executor.submit(() -> {
                for (int j = 0; j < requestsPerClient; j++) {
                    Response res;
                    try {
                        int variant = (j + clientIdx) % 5;
                        Request req = switch (variant) {
                            case 0 -> new Request(RequestType.PING);
                            case 1 -> new Request(RequestType.BID)
                                    .put("sessionId", "sessMix" + clientIdx)
                                    .put("auctionId", j % 10)
                                    .put("amount", 100.0 + j);
                            case 2 -> new Request(RequestType.LOGOUT)
                                    .put("sessionId", "sessMix" + clientIdx);
                            case 3 -> new Request(RequestType.GET_AUCTIONS)
                                    .put("sessionId", "sessMix" + clientIdx)
                                    .put("userId", 200 + clientIdx);
                            case 4 -> new Request(RequestType.MISC)
                                    .put("sessionId", "sessMix" + clientIdx)
                                    .put("command", "ls-auction");
                            default -> new Request(RequestType.PING);
                        };
                        res = handler.handleRequest(req);
                        if (res != null && res.isSuccess()) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    }
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertTrue(successCount.get() > 0, "Should have successes");
        assertEquals(0, errorCount.get(), "Should have zero exceptions");
    }

    @Test
    @DisplayName("Rapid-fire BID requests on same auction from different sessions")
    void rapidFireSameAuction() throws Exception {
        int numBidders = 30;
        int bidsPerBidder = 10;
        User[] bidders = new User[numBidders];
        for (int i = 0; i < numBidders; i++) {
            bidders[i] = new User(300 + i, Role.USER, "bidder" + i, "sessFast" + i);
            when(userSystem.findBySessionId("sessFast" + i)).thenReturn(bidders[i]);
        }

        AtomicInteger successCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numBidders; i++) {
            final int bidderIdx = i;
            futures.add(executor.submit(() -> {
                for (int j = 0; j < bidsPerBidder; j++) {
                    Request req = new Request(RequestType.BID)
                            .put("sessionId", "sessFast" + bidderIdx)
                            .put("auctionId", 42)
                            .put("amount", 100.0 + bidderIdx * 10 + j);
                    Response res = handler.handleRequest(req);
                    if (res != null && res.isSuccess()) {
                        successCount.incrementAndGet();
                    }
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertTrue(successCount.get() > 0, "Should have successful bids");
        verify(bidSystem, atLeastOnce()).placeBid(anyInt(), eq(42), anyDouble());
    }
}
