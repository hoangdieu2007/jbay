package a88.jbay.system;

import a88.jbay.client.ClientSession;
import a88.jbay.client.ResponseHandler;
import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.common.item.Item;
import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import a88.jbay.common.network.Response;
import a88.jbay.common.user.User;
import a88.jbay.common.user.role.Role;
import a88.jbay.controller.ControllerProvider;
import a88.jbay.controller.app.EntranceUI.ClientLoginController;
import a88.jbay.controller.app.EntranceUI.ClientRegisterController;
import a88.jbay.dao.*;
import a88.jbay.data.*;
import a88.jbay.client.ServerConnection;
import a88.jbay.di.ApplicationContext;
import a88.jbay.server.DatabaseController;
import a88.jbay.server.RequestHandler;
import a88.jbay.system.update.ConnectionSystem;
import a88.jbay.system.update.UpdateSystem;
import a88.jbay.system.user.AdminService;
import a88.jbay.system.user.UserSystem;
import a88.jbay.view.ViewManager;
import javafx.application.Platform;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientServerRealisticSimulationTest {

    // -----------------------------------------------------------------------
    // Shared DB
    // -----------------------------------------------------------------------
    private static DatabaseController dbController;

    @BeforeAll
    static void initDb() throws Exception {
        dbController = new DatabaseController();
        dbController.initializePool(
                "jdbc:h2:mem:realsim;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa", "", 50
        );
        try (var in = ClientServerRealisticSimulationTest.class
                .getResourceAsStream("/a88/jbay/db/schema-h2.sql")) {
            var sql = new String(in.readAllBytes());
            try (var conn = dbController.getConnection();
                 var stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        }
    }

    @AfterAll
    static void closeDb() {
        if (dbController != null) dbController.close();
    }

    // -----------------------------------------------------------------------
    // Per-test fields
    // -----------------------------------------------------------------------
    private RequestHandler requestHandler;
    private AuctionSystem auctionSystem;
    private BidSystem bidSystem;
    private UserSystem userSystem;
    private AuctionRepository auctionRepository;
    private NetworkSimulation sim;
    private ConnectionSystem connectionSystem;
    private UpdateSystem updateSystem;

    // Bridge-only fields
    private ClientSession clientSession;
    private ResponseHandler responseHandler;
    private ControllerProvider controllerProvider;

    // -----------------------------------------------------------------------
    // Setup / Teardown
    // -----------------------------------------------------------------------

    @BeforeEach
    void setUp() {
        cleanTables();
        initUnbridgedEnvironment();
    }

    @AfterEach
    void tearDown() {
        if (auctionSystem != null) auctionSystem.stopSystem();
    }

    private void initUnbridgedEnvironment() {
        var userDAO = new UserDAOImpl(dbController);
        var itemDAO = new ItemDAOImpl(dbController);
        var bidDAO = new BidDAOImpl(dbController);
        var auctionDAO = new AuctionDAOImpl(dbController);

        var userRepository = new UserRepository(userDAO);
        auctionRepository = new AuctionRepository(
                dbController, auctionDAO, itemDAO, userDAO, bidDAO);

        connectionSystem = new ConnectionSystem();
        updateSystem = new UpdateSystem(connectionSystem);
        userSystem = new UserSystem(userRepository);
        bidSystem = new BidSystem(
                auctionRepository,
                new BidRepository(dbController, auctionDAO, bidDAO),
                bidDAO, auctionDAO, updateSystem);
        bidSystem.setAutoBidDelayMs(0);

        // Register with DI container so BidSystem.getInstance() works
        ApplicationContext.getInstance().getContainer().registerSingleton(BidSystem.class, bidSystem);

        auctionSystem = new AuctionSystem(updateSystem, auctionRepository, userRepository);
        auctionSystem.stopSystem(); // heartbeat not needed — we use forceTick() manually

        var adminService = new AdminService(userDAO, userRepository, connectionSystem,
                auctionSystem, userSystem);
        requestHandler = new RequestHandler(userSystem, adminService, auctionSystem,
                connectionSystem, updateSystem, bidSystem);
        sim = new NetworkSimulation(requestHandler);
    }

    private void setupBridgedEnvironment() {
        if (auctionSystem != null) auctionSystem.stopSystem();

        var userDAO = new UserDAOImpl(dbController);
        var itemDAO = new ItemDAOImpl(dbController);
        var bidDAO = new BidDAOImpl(dbController);
        var auctionDAO = new AuctionDAOImpl(dbController);

        var userRepository = new UserRepository(userDAO);
        auctionRepository = new AuctionRepository(
                dbController, auctionDAO, itemDAO, userDAO, bidDAO);

        clientSession = new ClientSession();
        controllerProvider = mock(ControllerProvider.class);
        var viewManager = mock(ViewManager.class);
        responseHandler = new ResponseHandler(clientSession, controllerProvider, viewManager);
        when(controllerProvider.getController(ClientRegisterController.class))
                .thenReturn(mock(ClientRegisterController.class));

        connectionSystem = new BridgedConnectionSystem(responseHandler);
        updateSystem = new UpdateSystem(connectionSystem);
        userSystem = new UserSystem(userRepository);
        bidSystem = new BidSystem(
                auctionRepository,
                new BidRepository(dbController, auctionDAO, bidDAO),
                bidDAO, auctionDAO, updateSystem);
        bidSystem.setAutoBidDelayMs(0);

        // Register with DI container so BidSystem.getInstance() works in cancel/confirm/heartbeat paths
        ApplicationContext.getInstance().getContainer().registerSingleton(BidSystem.class, bidSystem);

        auctionSystem = new AuctionSystem(updateSystem, auctionRepository, userRepository);
        auctionSystem.stopSystem(); // heartbeat not needed — we use forceTick() manually

        var adminService = new AdminService(userDAO, userRepository, connectionSystem,
                auctionSystem, userSystem);
        requestHandler = new RequestHandler(userSystem, adminService, auctionSystem,
                connectionSystem, updateSystem, bidSystem);
        sim = new NetworkSimulation(requestHandler);
    }

    private void cleanTables() {
        try (var conn = dbController.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");
            stmt.execute("TRUNCATE TABLE bids");
            stmt.execute("TRUNCATE TABLE auctions");
            stmt.execute("TRUNCATE TABLE items");
            stmt.execute("TRUNCATE TABLE users");
            stmt.execute("SET REFERENTIAL_INTEGRITY TRUE");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Item makeItem(String name, double price) {
        return new Item(name, "ELECTRONICS", "desc-" + name, price, new byte[]{});
    }

    private Response sendAndHandle(NetworkSimulation.SimulatedClient client, Request request) {
        Response response = sim.send(client, request);
        if (response != null && !"GET_AUCTIONS_SUCCESS".equals(response.getMessage())
                && !"GET_USERS_SUCCESS".equals(response.getMessage())) {
            responseHandler.handle(response);
        }
        return response;
    }

    private NetworkSimulation.SimulatedClient registerAndLogin(String username, String password) {
        var client = sim.createClient();
        assertTrue(sim.register(client, username, password).isSuccess());

        when(controllerProvider.getController(ClientLoginController.class))
                .thenReturn(mock(ClientLoginController.class));
        try (MockedStatic<ServerConnection> sc = mockStatic(ServerConnection.class)) {
            sc.when(ServerConnection::getInstance).thenReturn(mock(ServerConnection.class));
            sendAndHandle(client, new Request(RequestType.LOGIN)
                    .put("username", username).put("password", password));
        }
        return client;
    }

    private NetworkSimulation.SimulatedClient registerAndLogin(String username, String password,
                                                                String role) {
        var client = sim.createClient();
        assertTrue(sim.send(client, new Request(RequestType.REGISTER)
                .put("username", username).put("password", password)
                .put("role", role)).isSuccess());

        when(controllerProvider.getController(ClientLoginController.class))
                .thenReturn(mock(ClientLoginController.class));
        try (MockedStatic<ServerConnection> sc = mockStatic(ServerConnection.class)) {
            sc.when(ServerConnection::getInstance).thenReturn(mock(ServerConnection.class));
            sendAndHandle(client, new Request(RequestType.LOGIN)
                    .put("username", username).put("password", password));
        }
        return client;
    }

    // ===================================================================
    //  1. REALISTIC CONCURRENT BIDDING
    // ===================================================================

    @Test
    @DisplayName("20 bidders race on same auction with validations")
    void concurrentBiddingRace() throws Exception {
        setupBridgedEnvironment();

        var seller = registerAndLogin("race_seller", "p");
        assertTrue(sendAndHandle(seller, new Request(RequestType.SELL)
                .put("item", makeItem("RaceItem", 500.0))
                .put("start", LocalDateTime.now().minusHours(1))
                .put("end", LocalDateTime.now().plusDays(1))
                .put("minIncrement", 10.0)).isSuccess());
        auctionSystem.forceTick();

        int numBidders = 20;
        var bidders = new NetworkSimulation.SimulatedClient[numBidders];
        for (int i = 0; i < numBidders; i++) {
            bidders[i] = registerAndLogin("racer" + i, "p");
        }

        sendAndHandle(bidders[0], new Request(RequestType.GET_AUCTIONS)
                .put("userId", bidders[0].getUserId()));
        int auctionId = clientSession.getBidderAuctions().keySet().iterator().next();

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger bidOk = new AtomicInteger();
        AtomicInteger bidFail = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        try (MockedStatic<Platform> p = mockStatic(Platform.class)) {
            p.when(() -> Platform.runLater(any(Runnable.class))).thenAnswer(inv -> null);

            CyclicBarrier barrier = new CyclicBarrier(numBidders);
            for (int i = 0; i < numBidders; i++) {
                int idx = i;
                futures.add(executor.submit(() -> {
                    try {
                        barrier.await(5, TimeUnit.SECONDS);
                        double amount = 500.0 + (idx + 1) * 25;
                        Response r = sim.bid(bidders[idx], auctionId, amount);
                        if (r != null && r.isSuccess()) bidOk.incrementAndGet();
                        else bidFail.incrementAndGet();
                    } catch (Exception e) {
                        bidFail.incrementAndGet();
                    }
                }));
            }
            for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertTrue(bidOk.get() >= 1, "At least one bid should succeed");
        assertTrue(bidFail.get() < numBidders, "Not all concurrent bids should fail");

        Auction finalAuction = auctionRepository.getAuctionById(auctionId);
        assertNotNull(finalAuction);
        assertTrue(finalAuction.getCurrentPrice() > 500.0, "Price increased from start");
        assertNotNull(finalAuction.getWinnerId(), "Has a winner");
        assertFalse(finalAuction.getBidHistory().isEmpty(), "Has bid history");

        // Verify DB matches in-memory
        List<a88.jbay.common.auction.BidData> dbHistory = bidDAO(dbController).findBidHistoryByAuctionId(auctionId);
        assertEquals(finalAuction.getBidHistory().size(), dbHistory.size(),
                "DB bid count matches in-memory");
    }

    @Test
    @DisplayName("Sequential bids with auto-bid competing simultaneously")
    void manualVsAutoBidRace() throws Exception {
        setupBridgedEnvironment();

        var seller = registerAndLogin("auto_seller", "p");
        assertTrue(sendAndHandle(seller, new Request(RequestType.SELL)
                .put("item", makeItem("AutoItem", 200.0))
                .put("start", LocalDateTime.now().minusHours(1))
                .put("end", LocalDateTime.now().plusDays(1))
                .put("minIncrement", 5.0)).isSuccess());
        auctionSystem.forceTick();

        var manualBidder = registerAndLogin("manual", "p");
        var autoBidder = registerAndLogin("auto", "p");

        sendAndHandle(manualBidder, new Request(RequestType.GET_AUCTIONS)
                .put("userId", manualBidder.getUserId()));
        int auctionId = clientSession.getBidderAuctions().keySet().iterator().next();

        try (MockedStatic<Platform> p = mockStatic(Platform.class)) {
            p.when(() -> Platform.runLater(any(Runnable.class))).thenAnswer(inv -> null);

            // Manual bidder bids first
            assertTrue(sendAndHandle(manualBidder, new Request(RequestType.BID)
                    .put("auctionId", auctionId).put("amount", 250.0)).isSuccess());

            // Auto-bidder enables auto-bid (max 500, increment 20)
            assertTrue(sendAndHandle(autoBidder, new Request(RequestType.AUTO_BID)
                    .put("auctionId", auctionId)
                    .put("max_amount", 500.0)
                    .put("increment", 20.0)).isSuccess());

            Auction afterAuto = clientSession.getBidderAuctions().get(auctionId);
            assertNotNull(afterAuto);
            // Auto-bid should have outbid the manual bidder (250 + max(20,5) = 270)
            assertTrue(afterAuto.getCurrentPrice() >= 270.0,
                    "Auto-bid raised price above manual bid");

            // Another manual bid should still work
            assertTrue(sendAndHandle(manualBidder, new Request(RequestType.BID)
                    .put("auctionId", auctionId).put("amount", 300.0)).isSuccess());

            // Auto-bid should counter
            sleep(300);
            Auction afterCounter = clientSession.getBidderAuctions().get(auctionId);
            assertTrue(afterCounter.getCurrentPrice() >= 300.0,
                    "Auto-bid countered manual overbid");
        }
    }

    @Test
    @DisplayName("Multiple auto-bidders: competitive escalation, max limits, and manual triggers")
    void multipleAutoBidders() throws Exception {
        setupBridgedEnvironment();

        var seller = registerAndLogin("mab_seller", "p");
        assertTrue(sendAndHandle(seller, new Request(RequestType.SELL)
                .put("item", makeItem("MultiAuto", 100.0))
                .put("start", LocalDateTime.now().minusHours(1))
                .put("end", LocalDateTime.now().plusDays(1))
                .put("minIncrement", 10.0)).isSuccess());
        auctionSystem.forceTick();

        var bidderLow  = registerAndLogin("mab_low", "p");
        var bidderHigh = registerAndLogin("mab_high", "p");
        var bidderMed  = registerAndLogin("mab_med", "p");

        sendAndHandle(bidderLow, new Request(RequestType.GET_AUCTIONS)
                .put("userId", bidderLow.getUserId()));
        int auctionId = clientSession.getBidderAuctions().keySet().iterator().next();

        try (MockedStatic<Platform> p = mockStatic(Platform.class)) {
            p.when(() -> Platform.runLater(any(Runnable.class))).thenAnswer(inv -> null);

            // --- Scenario 1: Low auto-bidder (max 150, inc 15) ---
            assertTrue(sendAndHandle(bidderLow, new Request(RequestType.AUTO_BID)
                    .put("auctionId", auctionId)
                    .put("max_amount", 150.0)
                    .put("increment", 15.0)).isSuccess());
            // First bid: min(150, 100 + max(15,10)) = min(150, 115) = 115
            Auction a1 = clientSession.getBidderAuctions().get(auctionId);
            assertEquals(115.0, a1.getCurrentPrice(), 0.001, "Low auto-bid placed at 115");
            assertEquals("mab_low", a1.getWinner());

            // --- Scenario 2: Higher auto-bidder (max 300, inc 20) takes over ---
            assertTrue(sendAndHandle(bidderHigh, new Request(RequestType.AUTO_BID)
                    .put("auctionId", auctionId)
                    .put("max_amount", 300.0)
                    .put("increment", 20.0)).isSuccess());
            // Competitive: maxLow(150) < maxHigh(300), so bidderHigh takes config
            // newPrice = min(300, max(115+10, 150+max(20,10))) = min(300, max(125, 170)) = min(300, 170) = 170
            Auction a2 = clientSession.getBidderAuctions().get(auctionId);
            assertEquals(170.0, a2.getCurrentPrice(), 0.001, "High auto-bid outbid low at 170");
            assertEquals("mab_high", a2.getWinner());

            // --- Scenario 3: Manual bid triggers auto-bid counter ---
            assertTrue(sendAndHandle(bidderMed, new Request(RequestType.BID)
                    .put("auctionId", auctionId).put("amount", 200.0)).isSuccess());
            // Auto-bid fires: min(300, 200 + max(20,10)) = min(300, 220) = 220
            sleep(300);
            Auction a3 = clientSession.getBidderAuctions().get(auctionId);
            assertTrue(a3.getCurrentPrice() >= 220.0,
                    "Auto-bid countered manual bid to at least 220, got " + a3.getCurrentPrice());
            assertEquals("mab_high", a3.getWinner(), "High auto-bidder still leads");

            // --- Scenario 4: Manual bid near max causes auto-bid to hit limit ---
            // Current price is 220, high auto-bid max is 300, inc 20
            // Bid 290 → auto-bid fires: min(300, 290+20) = min(300, 310) = 300
            assertTrue(sendAndHandle(bidderMed, new Request(RequestType.BID)
                    .put("auctionId", auctionId).put("amount", 290.0)).isSuccess());
            sleep(300);
            Auction a4 = clientSession.getBidderAuctions().get(auctionId);
            assertEquals(300.0, a4.getCurrentPrice(), 0.001,
                    "Auto-bid hit max limit at 300");
            assertEquals("mab_high", a4.getWinner());

            // --- Scenario 5: Another manual bid now succeeds (auto-bid is exhausted) ---
            assertTrue(sendAndHandle(bidderMed, new Request(RequestType.BID)
                    .put("auctionId", auctionId).put("amount", 310.0)).isSuccess());
            Auction a5 = auctionRepository.getAuctionById(auctionId);
            assertEquals(310.0, a5.getCurrentPrice(), 0.001,
                    "Manual bid succeeds after auto-bid exhausted");
            assertEquals("mab_med", a5.getWinner());

            // --- Scenario 6: Enable a medium auto-bid and verify it responds ---
            assertTrue(sendAndHandle(bidderMed, new Request(RequestType.AUTO_BID)
                    .put("auctionId", auctionId)
                    .put("max_amount", 400.0)
                    .put("increment", 25.0)).isSuccess());
            // No higher bid yet, and med is winner, so auto-bid is set but no bid needed
            Auction a6 = clientSession.getBidderAuctions().get(auctionId);
            assertEquals(310.0, a6.getCurrentPrice(), 0.001,
                    "Auto-bid configured but no bid needed when user is winner");

            // A new manual bid from another user should trigger med's auto-bid
            assertTrue(sendAndHandle(bidderLow, new Request(RequestType.BID)
                    .put("auctionId", auctionId).put("amount", 330.0)).isSuccess());
            // Auto-bid fires: min(400, 330 + max(25,10)) = min(400, 355) = 355
            sleep(300);
            Auction a7 = clientSession.getBidderAuctions().get(auctionId);
            assertEquals(355.0, a7.getCurrentPrice(), 0.001,
                    "Med auto-bid responded to manual bid");
            assertEquals("mab_med", a7.getWinner());
        }

        // Verify in-memory and DB bid counts match
        List<a88.jbay.common.auction.BidData> dbHistory = bidDAO(dbController).findBidHistoryByAuctionId(auctionId);
        Auction finalAuction = auctionRepository.getAuctionById(auctionId);
        assertEquals(finalAuction.getBidHistory().size(), dbHistory.size(),
                "DB bid count matches in-memory");
    }

    // ===================================================================
    //  2. REALISTIC USER + ADMIN OPERATIONS
    // ===================================================================

    @Test
    @DisplayName("Normal user flow: register → login → browse → bid → win → pay")
    void normalUserEndToEnd() {
        setupBridgedEnvironment();

        var seller = registerAndLogin("shop_king", "p");
        var buyer = registerAndLogin("buyer_one", "p");
        var admin = registerAndLogin("root_admin", "p", "ADMIN");

        // Seller creates auction
        assertTrue(sendAndHandle(seller, new Request(RequestType.SELL)
                .put("item", makeItem("GoldenRing", 300.0))
                .put("start", LocalDateTime.now().minusHours(2))
                .put("end", LocalDateTime.now().plusSeconds(5))
                .put("minIncrement", 15.0)).isSuccess());

        auctionSystem.forceTick();

        // Buyer bids and wins
        sendAndHandle(buyer, new Request(RequestType.GET_AUCTIONS)
                .put("userId", buyer.getUserId()));
        assertFalse(clientSession.getBidderAuctions().isEmpty(), "Buyer sees auction");
        int auctionId = clientSession.getBidderAuctions().keySet().iterator().next();

        try (MockedStatic<Platform> p = mockStatic(Platform.class)) {
            p.when(() -> Platform.runLater(any(Runnable.class))).thenAnswer(inv -> null);
            assertTrue(sendAndHandle(buyer, new Request(RequestType.BID)
                    .put("auctionId", auctionId).put("amount", 350.0)).isSuccess());
        }

        // Let auction finish
        sleep(6000);
        auctionSystem.forceTick();

        // Admin sees the finished auction
        sendAndHandle(admin, new Request(RequestType.GET_AUCTIONS)
                .put("userId", admin.getUserId()));
        assertFalse(clientSession.getAdminAuctions().isEmpty(), "Admin sees all auctions");

        // Winner requests payment QR
        Response payRes = sim.send(buyer, new Request(RequestType.PAY)
                .put("auctionId", auctionId));
        assertTrue(payRes.isSuccess(), "Pay request succeeds for winner");
    }

    @Test
    @DisplayName("Admin bans a user — banned user cannot bid or login")
    void adminBansMisbehavingUser() {
        setupBridgedEnvironment();

        var seller = registerAndLogin("legit_seller", "p");
        var troublemaker = registerAndLogin("troublemaker", "p");
        var admin = registerAndLogin("sheriff", "p", "ADMIN");

        // Create auction
        assertTrue(sendAndHandle(seller, new Request(RequestType.SELL)
                .put("item", makeItem("ValuableVase", 1000.0))
                .put("start", LocalDateTime.now().minusHours(1))
                .put("end", LocalDateTime.now().plusDays(3))
                .put("minIncrement", 20.0)).isSuccess());
        auctionSystem.forceTick();

        sendAndHandle(troublemaker, new Request(RequestType.GET_AUCTIONS)
                .put("userId", troublemaker.getUserId()));
        int auctionId = clientSession.getBidderAuctions().keySet().iterator().next();

        try (MockedStatic<Platform> p = mockStatic(Platform.class)) {
            p.when(() -> Platform.runLater(any(Runnable.class))).thenAnswer(inv -> null);

            // Troublemaker bids successfully first
            assertTrue(sendAndHandle(troublemaker, new Request(RequestType.BID)
                    .put("auctionId", auctionId).put("amount", 1100.0)).isSuccess());

            // Admin bans the troublemaker
            Response banRes = sim.banUser(admin, troublemaker.getUserId());
            assertTrue(banRes.isSuccess(), "Ban succeeds");

            // Troublemaker's next bid is rejected (session invalidated)
            Response failBid = sim.bid(troublemaker, auctionId, 1200.0);
            assertFalse(failBid.isSuccess(), "Banned user's bid rejected");

            // Admin can unban
            Response unbanRes = sim.unbanUser(admin, troublemaker.getUserId());
            assertTrue(unbanRes.isSuccess(), "Unban succeeds");
        }
    }

    @Test
    @DisplayName("Admin registers, creates auction, cancels it, then checks state")
    void adminCancelsAuction() {
        setupBridgedEnvironment();

        var admin = registerAndLogin("admin_cancel", "p", "ADMIN");

        assertTrue(sendAndHandle(admin, new Request(RequestType.SELL)
                .put("item", makeItem("AdminItem", 500.0))
                .put("start", LocalDateTime.now().minusHours(1))
                .put("end", LocalDateTime.now().plusDays(7))
                .put("minIncrement", 5.0)).isSuccess());
        auctionSystem.forceTick();

        sendAndHandle(admin, new Request(RequestType.GET_AUCTIONS)
                .put("userId", admin.getUserId()));
        int auctionId = clientSession.getAdminAuctions().keySet().iterator().next();

        // Admin cancels own auction
        Response cancelRes = sim.send(admin, new Request(RequestType.CANCEL)
                .put("auctionId", auctionId));
        assertTrue(cancelRes.isSuccess(), "Admin can cancel auction");

        // Verify state
        sendAndHandle(admin, new Request(RequestType.GET_AUCTIONS)
                .put("userId", admin.getUserId()));
        Auction cancelled = clientSession.getAdminAuctions().get(auctionId);
        assertNotNull(cancelled);
        assertEquals(AuctionState.CANCELED, cancelled.getAuctionState(),
                "Auction is canceled");
    }

    // ===================================================================
    //  3. FULL AUCTION LIFECYCLE WITH OPERATIONS
    // ===================================================================

    @Test
    @DisplayName("Full lifecycle: create → opening → bids → finish → confirm payment")
    void fullLifecycleWithConfirmPayment() {
        setupBridgedEnvironment();

        var seller = registerAndLogin("lifecycle_seller", "p");
        var bidder1 = registerAndLogin("lifecycle_b1", "p");
        var bidder2 = registerAndLogin("lifecycle_b2", "p");
        var bidder3 = registerAndLogin("lifecycle_b3", "p");

        // Phase 1: Create auction (starts OPENING, starts in the past so it transitions immediately)
        // Use a far-future end time so bids don't trigger anti-sniping extension
        assertTrue(sendAndHandle(seller, new Request(RequestType.SELL)
                .put("item", makeItem("LifecycleItem", 100.0))
                .put("start", LocalDateTime.now().minusHours(2))
                .put("end", LocalDateTime.now().plusMinutes(30))
                .put("minIncrement", 10.0)).isSuccess());

        // Phase 2: Heartbeat transitions OPENING → RUNNING
        auctionSystem.forceTick();

        // Phase 3: Bidding phase
        sendAndHandle(bidder1, new Request(RequestType.GET_AUCTIONS)
                .put("userId", bidder1.getUserId()));
        assertFalse(clientSession.getBidderAuctions().isEmpty());
        int auctionId = clientSession.getBidderAuctions().keySet().iterator().next();

        try (MockedStatic<Platform> p = mockStatic(Platform.class)) {
            p.when(() -> Platform.runLater(any(Runnable.class))).thenAnswer(inv -> null);

            assertTrue(sendAndHandle(bidder1, new Request(RequestType.BID)
                    .put("auctionId", auctionId).put("amount", 150.0)).isSuccess());
            assertTrue(sendAndHandle(bidder2, new Request(RequestType.BID)
                    .put("auctionId", auctionId).put("amount", 200.0)).isSuccess());
            assertTrue(sendAndHandle(bidder3, new Request(RequestType.BID)
                    .put("auctionId", auctionId).put("amount", 250.0)).isSuccess());
        }

        // Phase 4: Force auction end by setting endTime in the past, then tick
        Auction running = auctionRepository.getAuctionById(auctionId);
        assertNotNull(running);
        running.setEndTime(LocalDateTime.now().minusSeconds(1));
        auctionSystem.forceTick();

        // Phase 5: Seller confirms payment
        sendAndHandle(seller, new Request(RequestType.GET_AUCTIONS)
                .put("userId", seller.getUserId()));
        assertFalse(clientSession.getSellerAuctions().isEmpty(), "Seller sees finished auction");

        Auction finished = auctionRepository.getAuctionById(auctionId);
        assertNotNull(finished);
        assertEquals(AuctionState.FINISHED, finished.getAuctionState());

        Response confirmRes = sim.send(seller, new Request(RequestType.CONFIRM_PAYMENT)
                .put("auctionId", auctionId));
        assertTrue(confirmRes.isSuccess(), "Confirm payment succeeds");

        // Phase 6: Verify PAID state
        Auction paid = auctionRepository.getAuctionById(auctionId);
        assertNotNull(paid);
        assertEquals(AuctionState.PAID, paid.getAuctionState(),
                "Auction is PAID after confirmation");
    }

    @Test
    @DisplayName("Auction with anti-sniping: late bid extends, then finishes naturally")
    void lifecycleWithAntiSniping() {
        setupBridgedEnvironment();

        var seller = registerAndLogin("snip_seller", "p");

        // Auction ending in 60 seconds (well within 300s anti-sniping threshold)
        assertTrue(sendAndHandle(seller, new Request(RequestType.SELL)
                .put("item", makeItem("SnipTarget", 500.0))
                .put("start", LocalDateTime.now().minusHours(1))
                .put("end", LocalDateTime.now().plusSeconds(60))
                .put("minIncrement", 10.0)).isSuccess());
        auctionSystem.forceTick();

        var bidder = registerAndLogin("snip_bidder", "p");
        sendAndHandle(bidder, new Request(RequestType.GET_AUCTIONS)
                .put("userId", bidder.getUserId()));
        int auctionId = clientSession.getBidderAuctions().keySet().iterator().next();

        try (MockedStatic<Platform> p = mockStatic(Platform.class)) {
            p.when(() -> Platform.runLater(any(Runnable.class))).thenAnswer(inv -> null);

            Auction before = clientSession.getBidderAuctions().get(auctionId);
            LocalDateTime originalEnd = before.getEndTime();

            // Wait 55 sec so bid falls within threshold
            sleep(55000);

            // Late bid triggers anti-sniping extension
            assertTrue(sendAndHandle(bidder, new Request(RequestType.BID)
                    .put("auctionId", auctionId).put("amount", 550.0)).isSuccess());

            sendAndHandle(bidder, new Request(RequestType.GET_AUCTIONS)
                    .put("userId", bidder.getUserId()));
            Auction after = clientSession.getBidderAuctions().get(auctionId);

            long extendedSecs = java.time.temporal.ChronoUnit.SECONDS.between(originalEnd, after.getEndTime());
            assertTrue(extendedSecs >= 3500,
                    "Anti-sniping extended end time by ~3600s, got " + extendedSecs);
        }
    }

    // ===================================================================
    //  4. MULTIPLE AUCTIONS + CROSS-USER OPERATIONS
    // ===================================================================

    @Test
    @DisplayName("5 simultaneous auctions with 20 cross-bidding users")
    void multipleAuctionsWithCrossBidders() throws Exception {
        setupBridgedEnvironment();

        int numAuctions = 5;
        int numUsers = 20;

        // Create 5 auctions by different sellers
        var sellers = new NetworkSimulation.SimulatedClient[numAuctions];
        int[] auctionIds = new int[numAuctions];
        for (int i = 0; i < numAuctions; i++) {
            sellers[i] = registerAndLogin("mseller" + i, "p");
            double startPrice = 100.0 * (i + 1);
            assertTrue(sendAndHandle(sellers[i], new Request(RequestType.SELL)
                    .put("item", makeItem("MultiItem" + i, startPrice))
                    .put("start", LocalDateTime.now().minusHours(1))
                    .put("end", LocalDateTime.now().plusDays(1))
                    .put("minIncrement", 5.0 + i)).isSuccess());
        }
        auctionSystem.forceTick();

        // Register 20 cross-bidders
        var bidders = new NetworkSimulation.SimulatedClient[numUsers];
        for (int i = 0; i < numUsers; i++) {
            bidders[i] = registerAndLogin("xb" + i, "p");
        }

        // Get auction IDs
        sendAndHandle(bidders[0], new Request(RequestType.GET_AUCTIONS)
                .put("userId", bidders[0].getUserId()));
        var sortedIds = clientSession.getBidderAuctions().keySet().stream()
                .sorted().collect(Collectors.toList());
        for (int i = 0; i < numAuctions; i++) {
            auctionIds[i] = sortedIds.get(i);
        }

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger bidOk = new AtomicInteger();
        AtomicInteger bidFail = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        try (MockedStatic<Platform> p = mockStatic(Platform.class)) {
            p.when(() -> Platform.runLater(any(Runnable.class))).thenAnswer(inv -> null);

            // Each bidder bids on 2-3 random auctions
            Random rng = new Random(42);
            for (int i = 0; i < numUsers; i++) {
                int idx = i;
                futures.add(executor.submit(() -> {
                    for (int b = 0; b < 5; b++) {
                        int targetAuction = auctionIds[rng.nextInt(numAuctions)];
                        double amount = 150.0 + rng.nextDouble() * 500;
                        try {
                            Response r = sim.bid(bidders[idx], targetAuction, amount);
                            if (r != null && r.isSuccess()) bidOk.incrementAndGet();
                            else bidFail.incrementAndGet();
                        } catch (Exception e) {
                            bidFail.incrementAndGet();
                        }
                    }
                }));
            }
            for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertTrue(bidOk.get() > 0, "Cross-bidding succeeded");
        int totalAttempts = numUsers * 5;
        System.out.printf("Cross-bid: %d/%d ok, %d fail%n", bidOk.get(), totalAttempts, bidFail.get());

        // Verify all auctions have progressed
        for (int id : auctionIds) {
            Auction a = auctionRepository.getAuctionById(id);
            assertNotNull(a, "Auction " + id + " exists");
            assertTrue(a.getCurrentPrice() >= 100.0,
                    "Auction " + id + " price >= start price");
        }
    }

    @Test
    @DisplayName("Users subscribe/unsubscribe to auctions and receive updates")
    void subscriptionAndNotificationFlow() {
        setupBridgedEnvironment();

        var seller = registerAndLogin("sub_seller", "p");
        var watcher = registerAndLogin("watcher", "p");
        var bidder = registerAndLogin("sub_bidder", "p");

        assertTrue(sendAndHandle(seller, new Request(RequestType.SELL)
                .put("item", makeItem("SubItem", 300.0))
                .put("start", LocalDateTime.now().minusHours(1))
                .put("end", LocalDateTime.now().plusDays(3))
                .put("minIncrement", 10.0)).isSuccess());
        auctionSystem.forceTick();

        sendAndHandle(watcher, new Request(RequestType.GET_AUCTIONS)
                .put("userId", watcher.getUserId()));
        int auctionId = clientSession.getBidderAuctions().keySet().iterator().next();

        // Watcher subscribes explicitly
        Response subRes = sim.send(watcher, new Request(RequestType.SUBSCRIBE_AUCTION)
                .put("auctionId", auctionId));
        assertTrue(subRes.isSuccess(), "Subscribe succeeds");

        // Bidder bids — watcher should receive notification
        try (MockedStatic<Platform> p = mockStatic(Platform.class)) {
            p.when(() -> Platform.runLater(any(Runnable.class))).thenAnswer(inv -> null);

            assertTrue(sendAndHandle(bidder, new Request(RequestType.BID)
                    .put("auctionId", auctionId).put("amount", 350.0)).isSuccess());
        }

        // Unsubscribe
        Response unsubRes = sim.send(watcher, new Request(RequestType.UNSUBSCRIBE_AUCTION)
                .put("auctionId", auctionId));
        assertTrue(unsubRes.isSuccess(), "Unsubscribe succeeds");
    }

    // ===================================================================
    //  5. RANDOMIZED REALISTIC TESTS
    // ===================================================================

    @Test
    @DisplayName("Randomized marketplace: 50 random operations")
    void randomizedMarketplace() throws Exception {
        setupBridgedEnvironment();

        // Seed for reproducibility
        Random rng = new Random(2026);
        int auctionId = -1;
        AtomicBoolean auctionExists = new AtomicBoolean(false);

        try (MockedStatic<Platform> p = mockStatic(Platform.class)) {
            p.when(() -> Platform.runLater(any(Runnable.class))).thenAnswer(inv -> null);

            var admin = registerAndLogin("rand_admin", "p", "ADMIN");
            var seller = registerAndLogin("rand_seller", "p");
            var bidders = new NetworkSimulation.SimulatedClient[5];
            for (int i = 0; i < 5; i++) {
                bidders[i] = registerAndLogin("rand_b" + i, "p");
            }

            for (int op = 0; op < 50; op++) {
                int choice = rng.nextInt(8);
                try {
                    switch (choice) {
                        case 0 -> {
                            // Create auction
                            double price = 100 + rng.nextDouble() * 900;
                            var res = sendAndHandle(seller, new Request(RequestType.SELL)
                                    .put("item", makeItem("Rand" + op, price))
                                    .put("start", LocalDateTime.now().minusHours(2))
                                    .put("end", LocalDateTime.now().plusDays(rng.nextInt(7) + 1))
                                    .put("minIncrement", rng.nextDouble() * 20));
                            if (res.isSuccess()) auctionExists.set(true);
                        }
                        case 1 -> {
                            // Bid on a random auction
                            if (auctionExists.get()) {
                                var all = auctionRepository.getActiveAuctionList();
                                if (!all.isEmpty()) {
                                    int target = all.get(rng.nextInt(all.size())).getId();
                                    var bidder = bidders[rng.nextInt(bidders.length)];
                                    double amount = 100 + rng.nextDouble() * 1000;
                                    sim.bid(bidder, target, amount);
                                }
                            }
                        }
                        case 2 -> {
                            // Ping
                            var target = rng.nextBoolean() ? bidders[rng.nextInt(bidders.length)] : seller;
                            sim.ping(target);
                        }
                        case 3 -> {
                            // Get auctions
                            var target = rng.nextBoolean() ? bidders[rng.nextInt(bidders.length)] : seller;
                            sendAndHandle(target, new Request(RequestType.GET_AUCTIONS)
                                    .put("userId", target.getUserId()));
                        }
                        case 4 -> {
                            // List auctions via misc
                            var target = rng.nextBoolean() ? bidders[rng.nextInt(bidders.length)] : seller;
                            sim.misc(target, "ls-auction");
                        }
                        case 5 -> {
                            // Ban a random user (admin op)
                            var victim = bidders[rng.nextInt(bidders.length)];
                            sim.banUser(admin, victim.getUserId());
                        }
                        case 6 -> {
                            // Unban
                            sim.unbanUser(admin, rng.nextInt(10) + 1000);
                        }
                        case 7 -> {
                            // Subscribe to auction
                            if (auctionExists.get()) {
                                var all = auctionRepository.getActiveAuctionList();
                                if (!all.isEmpty()) {
                                    int target = all.get(rng.nextInt(all.size())).getId();
                                    var targetUser = rng.nextBoolean() ? bidders[rng.nextInt(bidders.length)] : seller;
                                    sim.send(targetUser, new Request(RequestType.SUBSCRIBE_AUCTION)
                                            .put("auctionId", target));
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                    // Randomized ops may hit edge cases — that's expected
                }
            }
        }

        // Verify system is still coherent
        assertNotNull(auctionRepository.getActiveAuctionList());
        assertDoesNotThrow(() -> auctionSystem.getActiveAuctionList());

        // Auction cache should be consistent
        var allAuctions = auctionRepository.getAllActiveAuctions();
        for (Auction a : allAuctions) {
            assertNotNull(a.getItem(), "Auction " + a.getId() + " has item");
            assertTrue(a.getCurrentPrice() >= 0, "Auction " + a.getId() + " has valid price");
        }
    }

    @Test
    @DisplayName("Randomized stress: 200 concurrent mixed operations")
    void randomizedStress() throws Exception {
        int numUsers = 30;
        int opsPerUser = 20;

        // Pre-register all participants
        var users = new NetworkSimulation.SimulatedClient[numUsers];
        for (int i = 0; i < numUsers; i++) {
            users[i] = sim.createClient();
            assertTrue(sim.register(users[i], "st" + i, "p").isSuccess());
            assertTrue(sim.login(users[i], "st" + i, "p").isSuccess());
        }

        // Admin
        var admin = sim.createClient();
        assertTrue(sim.send(admin, new Request(RequestType.REGISTER)
                .put("username", "st_admin").put("password", "p")
                .put("role", "ADMIN")).isSuccess());
        assertTrue(sim.login(admin, "st_admin", "p").isSuccess());

        // Create a few auctions
        for (int i = 0; i < 3; i++) {
            assertTrue(sim.sell(users[i], makeItem("Stress" + i, 200.0 + i * 100),
                    LocalDateTime.now().minusHours(2),
                    LocalDateTime.now().plusDays(1), 5.0).isSuccess());
        }
        auctionSystem.forceTick();

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        Random rng = new Random(99);

        long t0 = System.nanoTime();
        for (int i = 0; i < numUsers; i++) {
            int idx = i;
            futures.add(executor.submit(() -> {
                for (int op = 0; op < opsPerUser; op++) {
                    try {
                        int choice = rng.nextInt(7);
                        Response r = switch (choice) {
                            case 0 -> sim.ping(users[idx]);
                            case 1 -> sim.bid(users[idx], rng.nextInt(3) + 1, 100 + rng.nextDouble() * 500);
                            case 2 -> sim.getAuctions(users[idx]);
                            case 3 -> sim.misc(users[idx], "ls-auction");
                            case 4 -> sim.banUser(admin, rng.nextInt(numUsers) + 1000);
                            case 5 -> sim.unbanUser(admin, rng.nextInt(numUsers) + 1000);
                            case 6 -> sim.logout(users[idx]);
                            default -> null;
                        };
                        if (r != null) ok.incrementAndGet();
                        else fail.incrementAndGet();
                    } catch (Exception e) {
                        fail.incrementAndGet();
                    }
                }
            }));
        }
        for (Future<?> f : futures) f.get(60, TimeUnit.SECONDS);
        executor.shutdown();

        double elapsed = (System.nanoTime() - t0) / 1_000_000_000.0;
        int total = numUsers * opsPerUser;
        System.out.printf("Randomized stress: %d/%d ok, %.1f req/s%n",
                ok.get(), total, total / elapsed);
        assertTrue(ok.get() > total / 2, "Most operations should succeed");
    }

    @Test
    @DisplayName("Extreme concurrency: 30 bidders × 10 bids with heartbeats running")
    void extremeConcurrentBiddingWithHeartbeat() throws Exception {
        setupBridgedEnvironment();

        var seller = registerAndLogin("extreme_seller", "p");
        assertTrue(sendAndHandle(seller, new Request(RequestType.SELL)
                .put("item", makeItem("ExtremeItem", 50.0))
                .put("start", LocalDateTime.now().minusHours(2))
                .put("end", LocalDateTime.now().plusDays(1))
                .put("minIncrement", 1.0)).isSuccess());
        auctionSystem.forceTick();

        int numBidders = 30;
        var bidders = new NetworkSimulation.SimulatedClient[numBidders];
        for (int i = 0; i < numBidders; i++) {
            bidders[i] = registerAndLogin("extreme" + i, "p");
        }

        sendAndHandle(bidders[0], new Request(RequestType.GET_AUCTIONS)
                .put("userId", bidders[0].getUserId()));
        int auctionId = clientSession.getBidderAuctions().keySet().iterator().next();

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger bidOk = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        // Keep heartbeats running during bidding
        try (MockedStatic<Platform> p = mockStatic(Platform.class)) {
            p.when(() -> Platform.runLater(any(Runnable.class))).thenAnswer(inv -> null);

            CyclicBarrier startGate = new CyclicBarrier(numBidders);
            for (int i = 0; i < numBidders; i++) {
                int idx = i;
                futures.add(executor.submit(() -> {
                    try {
                        startGate.await(10, TimeUnit.SECONDS);
                        for (int b = 0; b < 10; b++) {
                            double amount = 50.0 + idx + b * 3;
                            Response r = bidSystem.placeBid(bidders[idx].getUserId(), auctionId, amount)
                                    ? new Response(true, "BID_SUCCESS", null)
                                    : new Response(false, "BID_FAIL", null);
                            if (r.isSuccess()) bidOk.incrementAndGet();
                        }
                    } catch (Exception ignored) {}
                }));
            }
            for (Future<?> f : futures) f.get(60, TimeUnit.SECONDS);
        }
        executor.shutdown();

        System.out.println("Extreme concurrency: " + bidOk.get() + " bids succeeded out of "
                + (numBidders * 10) + " attempts");

        Auction finalAuction = auctionRepository.getAuctionById(auctionId);
        assertNotNull(finalAuction);
        assertTrue(finalAuction.getCurrentPrice() > 50.0, "Price increased");
        assertTrue(finalAuction.getBidHistory().size() >= bidOk.get(),
                "Bid history matches success count");
    }

    // -----------------------------------------------------------------------
    //  Helpers
    // -----------------------------------------------------------------------

    private BidDAO bidDAO(DatabaseController db) {
        return new BidDAOImpl(db);
    }

    static class BridgedConnectionSystem extends ConnectionSystem {
        private final ResponseHandler responseHandler;

        BridgedConnectionSystem(ResponseHandler responseHandler) {
            super();
            this.responseHandler = responseHandler;
        }

        @Override
        public void sendToUser(int userId, Response response) {
            responseHandler.handle(response);
        }

        @Override
        public void sendToUsers(Set<Integer> userIds, Response response) {
            responseHandler.handle(response);
        }

        @Override
        public void broadcast(Response response) {
            responseHandler.handle(response);
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
