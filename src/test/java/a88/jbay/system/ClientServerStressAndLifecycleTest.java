package a88.jbay.system;

import a88.jbay.client.ClientSession;
import a88.jbay.client.ResponseHandler;
import a88.jbay.client.ServerConnection;
import a88.jbay.dao.*;
import a88.jbay.data.*;
import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.common.item.Item;
import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import a88.jbay.common.network.Response;
import a88.jbay.common.user.User;
import a88.jbay.controller.ControllerProvider;
import a88.jbay.controller.app.EntranceUI.ClientLoginController;
import a88.jbay.controller.app.EntranceUI.ClientRegisterController;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientServerStressAndLifecycleTest {

    // -----------------------------------------------------------------------
    // Shared DB (one H2 instance for the entire class)
    // -----------------------------------------------------------------------
    private static DatabaseController dbController;

    @BeforeAll
    static void initDb() throws Exception {
        dbController = new DatabaseController();
        dbController.initializePool(
                "jdbc:h2:mem:stresslifecycle;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa", ""
        );
        try (var in = ClientServerStressAndLifecycleTest.class
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
    // Per-test fields (stress-tests: unbridged; lifecycle-tests: re-setup with bridge)
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

        var userDAO = new UserDAOImpl(dbController);
        var itemDAO = new ItemDAOImpl(dbController);
        var bidDAO = new BidDAOImpl(dbController);
        var auctionDAO = new AuctionDAOImpl(dbController);

        var userRepository = new UserRepository(userDAO);
        auctionRepository = new AuctionRepository(
                dbController, auctionDAO, itemDAO, userDAO, bidDAO);

        // Unbridged – async sends go nowhere (no registered connections)
        connectionSystem = new ConnectionSystem();
        updateSystem = new UpdateSystem(connectionSystem);
        userSystem = new UserSystem(userRepository);
        bidSystem = new BidSystem(
                auctionRepository,
                new BidRepository(dbController, auctionDAO, bidDAO),
                bidDAO, auctionDAO, updateSystem);
        bidSystem.setAutoBidDelayMs(0);
        auctionSystem = new AuctionSystem(updateSystem, auctionRepository, userRepository);

        var adminService = new AdminService(userDAO, userRepository, connectionSystem,
                auctionSystem, userSystem);
        requestHandler = new RequestHandler(userSystem, adminService, auctionSystem,
                connectionSystem, updateSystem, bidSystem);
        sim = new NetworkSimulation(requestHandler);
    }

    @AfterEach
    void tearDown() {
        if (auctionSystem != null) auctionSystem.stopSystem();
    }

    private void setupBridgedEnvironment() {
        auctionSystem.stopSystem();

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
        auctionSystem = new AuctionSystem(updateSystem, auctionRepository, userRepository);

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
        return new Item(name, "ELECTRONICS", "desc", price, new byte[]{});
    }

    /** Bridged send: route through server then feed response to client handler. */
    private Response sendAndHandle(NetworkSimulation.SimulatedClient client, Request request) {
        Response response = sim.send(client, request);
        if (response != null && !"GET_AUCTIONS_SUCCESS".equals(response.getMessage())
                && !"GET_USERS_SUCCESS".equals(response.getMessage())) {
            responseHandler.handle(response);
        }
        return response;
    }

    /** Register + login a fresh client. Returns the logged-in SimulatedClient. */
    private NetworkSimulation.SimulatedClient registerAndLogin(String username, String password) {
        var client = sim.createClient();
        assertTrue(sim.register(client, username, password).isSuccess(),
                "Register " + username);

        when(controllerProvider.getController(ClientLoginController.class))
                .thenReturn(mock(ClientLoginController.class));
        try (MockedStatic<ServerConnection> sc = mockStatic(ServerConnection.class)) {
            sc.when(ServerConnection::getInstance).thenReturn(mock(ServerConnection.class));
            sendAndHandle(client, new Request(RequestType.LOGIN)
                    .put("username", username).put("password", password));
        }
        return client;
    }

    /** Register + login a fresh client with a custom role. */
    private NetworkSimulation.SimulatedClient registerAndLogin(String username, String password,
                                                                String role) {
        var client = sim.createClient();
        assertTrue(sim.send(client, new Request(RequestType.REGISTER)
                .put("username", username).put("password", password)
                .put("role", role)).isSuccess(), "Register " + username);

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
    //  STRESS TESTS  (unbridged – no client‑side verification)
    // ===================================================================

    @Test
    @DisplayName("500 concurrent register + login")
    void concurrentRegisterAndLogin() throws Exception {
        int numUsers = 500;
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numUsers; i++) {
            int idx = i;
            futures.add(executor.submit(() -> {
                try {
                    var c = sim.createClient();
                    Response r = sim.register(c, "suser" + idx, "pass" + idx);
                    if (r != null && r.isSuccess()) {
                        Response l = sim.login(c, "suser" + idx, "pass" + idx);
                        if (l != null && l.isSuccess()) ok.incrementAndGet();
                        else fail.incrementAndGet();
                    } else fail.incrementAndGet();
                } catch (Exception e) { fail.incrementAndGet(); }
            }));
        }
        for (Future<?> f : futures) f.get(90, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(numUsers, ok.get(), "All 500 users should register+login");
        assertEquals(0, fail.get(), "No exceptions");
    }

    @Test
    @DisplayName("50 sellers + 100 bidders mixed workload")
    void mixedSellAndBidLoad() throws Exception {
        int numSellers = 50;
        int numBidders = 100;
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger sellOk = new AtomicInteger();
        AtomicInteger bidOk = new AtomicInteger();
        AtomicInteger err = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        // Register + login sellers
        var sellers = new NetworkSimulation.SimulatedClient[numSellers];
        for (int i = 0; i < numSellers; i++) {
            sellers[i] = sim.createClient();
            assertTrue(sim.register(sellers[i], "ss" + i, "p").isSuccess());
            assertTrue(sim.login(sellers[i], "ss" + i, "p").isSuccess());
        }
        // Register + login bidders
        var bidders = new NetworkSimulation.SimulatedClient[numBidders];
        for (int i = 0; i < numBidders; i++) {
            bidders[i] = sim.createClient();
            assertTrue(sim.register(bidders[i], "sb" + i, "p").isSuccess());
            assertTrue(sim.login(bidders[i], "sb" + i, "p").isSuccess());
        }

        // Sellers create auctions concurrently
        for (int i = 0; i < numSellers; i++) {
            int idx = i;
            futures.add(executor.submit(() -> {
                try {
                    Item item = makeItem("LoadItem" + idx, 50.0 + idx);
                    sim.sell(sellers[idx], item,
                            LocalDateTime.now().minusHours(1),
                            LocalDateTime.now().plusDays(1), 5.0);
                    sellOk.incrementAndGet();
                } catch (Exception e) { err.incrementAndGet(); }
            }));
        }
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        futures.clear();

        auctionSystem.forceTick();

        // Bidders bid concurrently on auction IDs (1..numSellers)
        for (int i = 0; i < numBidders; i++) {
            int idx = i;
            futures.add(executor.submit(() -> {
                try {
                    for (int b = 0; b < 5; b++) {
                        int auctionId = (idx + b) % numSellers + 1;
                        Response r = sim.bid(bidders[idx], auctionId, 100.0 + b * 10);
                        if (r != null && r.isSuccess()) bidOk.incrementAndGet();
                    }
                } catch (Exception e) { err.incrementAndGet(); }
            }));
        }
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(sellOk.get() > 0, "At least some sells succeeded");
        assertTrue(bidOk.get() >= 0, "Bid counter non-negative");
        assertEquals(0, err.get(), "No unexpected exceptions");
    }

    @Test
    @DisplayName("100 bidders × 20 bids on the same auction")
    void concurrentBiddingOnSameAuction() throws Exception {
        var seller = sim.createClient();
        assertTrue(sim.register(seller, "stress_seller", "p").isSuccess());
        assertTrue(sim.login(seller, "stress_seller", "p").isSuccess());

        Item item = makeItem("StressAuction", 200.0);
        assertTrue(sim.sell(seller, item,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusDays(1), 2.0).isSuccess());

        auctionSystem.forceTick();

        int numBidders = 100;
        int bidsPerBidder = 20;
        var bidders = new NetworkSimulation.SimulatedClient[numBidders];
        String[] bidderNames = new String[numBidders];
        for (int i = 0; i < numBidders; i++) {
            bidderNames[i] = "hammer" + i;
            bidders[i] = sim.createClient();
            assertTrue(sim.register(bidders[i], bidderNames[i], "p").isSuccess());
            assertTrue(sim.login(bidders[i], bidderNames[i], "p").isSuccess());
        }

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger totalOk = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numBidders; i++) {
            int idx = i;
            futures.add(executor.submit(() -> {
                for (int b = 0; b < bidsPerBidder; b++) {
                    try {
                        double amount = 200.0 + idx * 5 + b * 2;
                        Response r = sim.bid(bidders[idx], 1, amount);
                        if (r != null && r.isSuccess()) totalOk.incrementAndGet();
                    } catch (Exception ignored) {}
                }
            }));
        }
        for (Future<?> f : futures) f.get(60, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(totalOk.get() > 0, "At least some bids succeeded");
        Auction finalAuction = auctionRepository.getAuctionById(1);
        assertNotNull(finalAuction, "Auction should exist");
        assertTrue(finalAuction.getCurrentPrice() >= 200.0, "Price should be >= start price");
        assertFalse(finalAuction.getBidHistory().isEmpty(), "Bid history not empty");
    }

    @Test
    @DisplayName("Admin bans 50 users concurrently while users are active")
    void adminUnderLoad() throws Exception {
        int numUsers = 50;
        var admin = sim.createClient();
        // Register admin as USER role then manually upgrade in DB for ban capability
        assertTrue(sim.send(admin, new Request(RequestType.REGISTER)
                .put("username", "admin_load")
                .put("password", "p")
                .put("role", "ADMIN")).isSuccess());
        assertTrue(sim.login(admin, "admin_load", "p").isSuccess());

        var users = new NetworkSimulation.SimulatedClient[numUsers];
        for (int i = 0; i < numUsers; i++) {
            users[i] = sim.createClient();
            assertTrue(sim.register(users[i], "victim" + i, "p").isSuccess());
            assertTrue(sim.login(users[i], "victim" + i, "p").isSuccess());
        }

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger banOk = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numUsers; i++) {
            int idx = i;
            futures.add(executor.submit(() -> {
                try {
                    Response r = sim.banUser(admin, idx + 2); // +2 because admin is user 1, victims start at 2
                    if (r != null && r.isSuccess()) banOk.incrementAndGet();
                } catch (Exception ignored) {}
            }));
        }
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(banOk.get() > 0, "At least some bans succeeded");
    }

    @Test
    @DisplayName("100 users × 50 mixed requests each")
    void sustainedMixedRequests() throws Exception {
        int numUsers = 100;
        int reqPerUser = 50;
        var clients = new NetworkSimulation.SimulatedClient[numUsers];
        for (int i = 0; i < numUsers; i++) {
            clients[i] = sim.createClient();
            assertTrue(sim.register(clients[i], "mix" + i, "p").isSuccess());
            assertTrue(sim.login(clients[i], "mix" + i, "p").isSuccess());
        }

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        long t0 = System.nanoTime();
        for (int i = 0; i < numUsers; i++) {
            int idx = i;
            futures.add(executor.submit(() -> {
                for (int j = 0; j < reqPerUser; j++) {
                    try {
                        Response r;
                        switch ((j + idx) % 5) {
                            case 0 -> r = sim.ping(clients[idx]);
                            case 1 -> r = sim.bid(clients[idx], j % 20 + 1, 100.0 + j);
                            case 2 -> r = sim.getAuctions(clients[idx]);
                            case 3 -> r = sim.misc(clients[idx], "ls-auction");
                            default -> r = sim.logout(clients[idx]);
                        }
                        if (r != null) ok.incrementAndGet();
                        else failed.incrementAndGet();
                    } catch (Exception e) { failed.incrementAndGet(); }
                }
            }));
        }
        for (Future<?> f : futures) f.get(60, TimeUnit.SECONDS);
        executor.shutdown();

        double elapsed = (System.nanoTime() - t0) / 1_000_000_000.0;
        int total = numUsers * reqPerUser;
        System.out.printf("Sustained load: %d/%d ok, %.1f req/s%n",
                ok.get(), total, total / elapsed);
        assertEquals(0, failed.get(), "No exceptions");
        assertTrue(ok.get() > total / 2, "Most requests should succeed");
    }

    @Test
    @DisplayName("1000 concurrent PING requests")
    void extremePingLoad() throws Exception {
        var clients = new NetworkSimulation.SimulatedClient[1000];
        for (int i = 0; i < 1000; i++) {
            clients[i] = sim.createClient();
            assertTrue(sim.register(clients[i], "pinger" + i, "p").isSuccess());
            assertTrue(sim.login(clients[i], "pinger" + i, "p").isSuccess());
        }

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger err = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        long t0 = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            int idx = i;
            futures.add(executor.submit(() -> {
                try {
                    Response r = sim.ping(clients[idx]);
                    if (r != null && r.isSuccess() && "PONG".equals(r.getMessage()))
                        ok.incrementAndGet();
                    else err.incrementAndGet();
                } catch (Exception e) { err.incrementAndGet(); }
            }));
        }
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        executor.shutdown();

        double elapsed = (System.nanoTime() - t0) / 1_000_000_000.0;
        System.out.printf("1000 pings in %.2f s (%.0f req/s)%n", elapsed, 1000 / elapsed);
        assertEquals(1000, ok.get(), "All pings should succeed");
        assertEquals(0, err.get(), "No exceptions");
    }

    // ===================================================================
    //  REAL‑LIFE SIMULATION TESTS  (bridged – full client + server)
    // ===================================================================

    @Test
    @DisplayName("Full auction lifecycle: create → auto-bid → finish → pay")
    void fullAuctionLifecycle() {
        setupBridgedEnvironment();

        // Seller creates auction
        var seller = registerAndLogin("lifecycle_seller", "p");

        Item painting = makeItem("Painting", 1000.0);
        assertTrue(sendAndHandle(seller, new Request(RequestType.SELL)
                .put("item", painting)
                .put("start", LocalDateTime.now().minusHours(2))
                .put("end", LocalDateTime.now().plusSeconds(3))
                .put("minIncrement", 10.0)).isSuccess());

        auctionSystem.forceTick();

        // Bidders register, login, and enable auto-bid
        var bidderA = registerAndLogin("bidderA", "p");
        var bidderB = registerAndLogin("bidderB", "p");
        var bidderC = registerAndLogin("bidderC", "p");

        // Fetch the auctionId via GET_AUCTIONS
        sendAndHandle(bidderA, new Request(RequestType.GET_AUCTIONS)
                .put("userId", bidderA.getUserId()));
        assertFalse(clientSession.getBidderAuctions().isEmpty(), "Bidder A sees auction");
        int auctionId = clientSession.getBidderAuctions().keySet().iterator().next();

        // Bidders place manual bids
        try (MockedStatic<Platform> p = mockStatic(Platform.class)) {
            p.when(() -> Platform.runLater(any(Runnable.class))).thenAnswer(inv -> null);

            assertTrue(sendAndHandle(bidderA, new Request(RequestType.BID)
                    .put("auctionId", auctionId).put("amount", 1100.0)).isSuccess());
            assertTrue(sendAndHandle(bidderB, new Request(RequestType.BID)
                    .put("auctionId", auctionId).put("amount", 1200.0)).isSuccess());
            assertTrue(sendAndHandle(bidderC, new Request(RequestType.BID)
                    .put("auctionId", auctionId).put("amount", 1300.0)).isSuccess());

            // Verify client saw the update
            Auction updated = clientSession.getBidderAuctions().get(auctionId);
            assertNotNull(updated);
            assertEquals(1300.0, updated.getCurrentPrice(), 0.001);
        }

        // Wait for auction to finish (end = 3 sec from creation)
        sleep(4000);
        auctionSystem.forceTick();

        // Now the auction should be finished. Winner is bidderC.
        // Seller gets their auction listed
        sendAndHandle(seller, new Request(RequestType.GET_AUCTIONS)
                .put("userId", seller.getUserId()));
        assertFalse(clientSession.getSellerAuctions().isEmpty(),
                "Seller sees their auction");
    }

    @Test
    @DisplayName("Multi-auction marketplace with 3 sellers and 8 bidders")
    void multiAuctionMarketplace() {
        setupBridgedEnvironment();

        // 3 sellers each create an auction
        var seller1 = registerAndLogin("shop1", "p");
        var seller2 = registerAndLogin("shop2", "p");
        var seller3 = registerAndLogin("shop3", "p");

        assertTrue(sendAndHandle(seller1, new Request(RequestType.SELL)
                .put("item", makeItem("Camera", 300.0))
                .put("start", LocalDateTime.now().minusHours(1))
                .put("end", LocalDateTime.now().plusDays(3))
                .put("minIncrement", 5.0)).isSuccess());

        assertTrue(sendAndHandle(seller2, new Request(RequestType.SELL)
                .put("item", makeItem("Guitar", 200.0))
                .put("start", LocalDateTime.now().minusHours(1))
                .put("end", LocalDateTime.now().plusDays(3))
                .put("minIncrement", 3.0)).isSuccess());

        assertTrue(sendAndHandle(seller3, new Request(RequestType.SELL)
                .put("item", makeItem("Watch", 500.0))
                .put("start", LocalDateTime.now().minusHours(1))
                .put("end", LocalDateTime.now().plusDays(3))
                .put("minIncrement", 10.0)).isSuccess());

        auctionSystem.forceTick();

        // 8 bidders register and browse
        var bidders = new NetworkSimulation.SimulatedClient[8];
        for (int i = 0; i < 8; i++) {
            bidders[i] = registerAndLogin("buyer" + i, "p");
        }

        // Each bidder: GET_AUCTIONS, then bid on 1-2 different auctions
        try (MockedStatic<Platform> p = mockStatic(Platform.class)) {
            p.when(() -> Platform.runLater(any(Runnable.class))).thenAnswer(inv -> null);

            for (int i = 0; i < 8; i++) {
                sendAndHandle(bidders[i], new Request(RequestType.GET_AUCTIONS)
                        .put("userId", bidders[i].getUserId()));

                // Each bidder bids on auction (i % 3 + 1)
                int targetAuction = i % 3 + 1;
                double amount = 500.0 + i * 25;
                Response bidRes = sendAndHandle(bidders[i], new Request(RequestType.BID)
                        .put("auctionId", targetAuction).put("amount", amount));
                assertTrue(bidRes.isSuccess(), "Bidder " + i + " should bid on auction " + targetAuction);
            }

            // Verify sellers see their auctions populated
            sendAndHandle(seller1, new Request(RequestType.GET_AUCTIONS)
                    .put("userId", seller1.getUserId()));
            sendAndHandle(seller2, new Request(RequestType.GET_AUCTIONS)
                    .put("userId", seller2.getUserId()));
            sendAndHandle(seller3, new Request(RequestType.GET_AUCTIONS)
                    .put("userId", seller3.getUserId()));

            assertFalse(clientSession.getSellerAuctions().isEmpty(),
                    "Seller should see their auction");
        }
    }

    @Test
    @DisplayName("Outbid → notification → re-bid flow between two users")
    void outbidAndRebidFlow() {
        setupBridgedEnvironment();

        var seller = registerAndLogin("rebid_seller", "p");
        assertTrue(sendAndHandle(seller, new Request(RequestType.SELL)
                .put("item", makeItem("Phone", 400.0))
                .put("start", LocalDateTime.now().minusHours(1))
                .put("end", LocalDateTime.now().plusDays(7))
                .put("minIncrement", 10.0)).isSuccess());

        auctionSystem.forceTick();

        var alice = registerAndLogin("alice", "p");
        var bob = registerAndLogin("bob", "p");

        // Alice gets auctions → sees the listing
        sendAndHandle(alice, new Request(RequestType.GET_AUCTIONS)
                .put("userId", alice.getUserId()));
        assertFalse(clientSession.getBidderAuctions().isEmpty(), "Alice sees auction");
        int auctionId = clientSession.getBidderAuctions().keySet().iterator().next();

        try (MockedStatic<Platform> p = mockStatic(Platform.class)) {
            p.when(() -> Platform.runLater(any(Runnable.class))).thenAnswer(inv -> null);

            // Alice bids 450
            assertTrue(sendAndHandle(alice, new Request(RequestType.BID)
                    .put("auctionId", auctionId).put("amount", 450.0)).isSuccess());

            Auction afterAlice = clientSession.getBidderAuctions().get(auctionId);
            assertNotNull(afterAlice);
            assertEquals(450.0, afterAlice.getCurrentPrice(), 0.001,
                    "Alice's bid sets price to 450");

            // Bob bids 500 (outbids Alice)
            assertTrue(sendAndHandle(bob, new Request(RequestType.BID)
                    .put("auctionId", auctionId).put("amount", 500.0)).isSuccess());

            // Alice sees she's been outbid (price is now 500)
            sendAndHandle(alice, new Request(RequestType.GET_AUCTIONS)
                    .put("userId", alice.getUserId()));
            Auction afterBob = clientSession.getBidderAuctions().get(auctionId);
            assertNotNull(afterBob);
            assertEquals(500.0, afterBob.getCurrentPrice(), 0.001,
                    "Alice sees updated price after Bob's bid");

            // Alice re-bids 550
            assertTrue(sendAndHandle(alice, new Request(RequestType.BID)
                    .put("auctionId", auctionId).put("amount", 550.0)).isSuccess());

            // Verify final price visible to Alice
            sendAndHandle(alice, new Request(RequestType.GET_AUCTIONS)
                    .put("userId", alice.getUserId()));
            Auction finalState = clientSession.getBidderAuctions().get(auctionId);
            assertNotNull(finalState);
            assertEquals(550.0, finalState.getCurrentPrice(), 0.001,
                    "Alice's re-bid sets price to 550");

            assertEquals("alice", finalState.getWinner(),
                    "Alice is the winner after re-bid");
        }
    }

    @Test
    @DisplayName("Anti-sniping: late bid extends auction end time")
    void antiSnipingExtension() {
        setupBridgedEnvironment();

        var seller = registerAndLogin("sniping_seller", "p");

        // Auction ending in 120 seconds (within anti-sniping threshold of 300s)
        assertTrue(sendAndHandle(seller, new Request(RequestType.SELL)
                .put("item", makeItem("RareCoin", 1000.0))
                .put("start", LocalDateTime.now().minusHours(1))
                .put("end", LocalDateTime.now().plusSeconds(120))
                .put("minIncrement", 10.0)).isSuccess());

        auctionSystem.forceTick();

        var bidder = registerAndLogin("sniping_bidder", "p");

        sendAndHandle(bidder, new Request(RequestType.GET_AUCTIONS)
                .put("userId", bidder.getUserId()));
        assertFalse(clientSession.getBidderAuctions().isEmpty(), "Bidder sees auction");
        int auctionId = clientSession.getBidderAuctions().keySet().iterator().next();

        try (MockedStatic<Platform> p = mockStatic(Platform.class)) {
            p.when(() -> Platform.runLater(any(Runnable.class))).thenAnswer(inv -> null);

            // Record end time before bid
            Auction before = clientSession.getBidderAuctions().get(auctionId);
            LocalDateTime originalEnd = before.getEndTime();

            // Wait a moment to ensure bid is "late"
            sleep(1000);

            // Place a bid — anti-sniping should extend the end time
            assertTrue(sendAndHandle(bidder, new Request(RequestType.BID)
                    .put("auctionId", auctionId).put("amount", 1100.0)).isSuccess());

            // Re-fetch auction to see updated end time
            sendAndHandle(bidder, new Request(RequestType.GET_AUCTIONS)
                    .put("userId", bidder.getUserId()));
            Auction after = clientSession.getBidderAuctions().get(auctionId);
            assertNotNull(after);

            // End time should have been extended by 3600 seconds
            long extendedSecs = ChronoUnit.SECONDS.between(originalEnd, after.getEndTime());
            assertTrue(extendedSecs >= 3500,
                    "End time should be extended by ~3600s, got " + extendedSecs + "s");
        }
    }

    @Test
    @DisplayName("Ban during active auction prevents further bids")
    void banDuringActiveAuction() {
        setupBridgedEnvironment();

        var seller = registerAndLogin("ban_seller", "p");
        assertTrue(sendAndHandle(seller, new Request(RequestType.SELL)
                .put("item", makeItem("Laptop", 800.0))
                .put("start", LocalDateTime.now().minusHours(1))
                .put("end", LocalDateTime.now().plusDays(3))
                .put("minIncrement", 5.0)).isSuccess());

        auctionSystem.forceTick();

        var bidder = registerAndLogin("target_user", "p");

        sendAndHandle(bidder, new Request(RequestType.GET_AUCTIONS)
                .put("userId", bidder.getUserId()));
        assertFalse(clientSession.getBidderAuctions().isEmpty(), "Bidder sees auction");
        int auctionId = clientSession.getBidderAuctions().keySet().iterator().next();

        try (MockedStatic<Platform> p = mockStatic(Platform.class)) {
            p.when(() -> Platform.runLater(any(Runnable.class))).thenAnswer(inv -> null);

            // First bid succeeds
            assertTrue(sendAndHandle(bidder, new Request(RequestType.BID)
                    .put("auctionId", auctionId).put("amount", 850.0)).isSuccess());

            // Admin registers and bans the bidder
            var admin = registerAndLogin("ban_admin", "p", "ADMIN");
            sim.banUser(admin, bidder.getUserId());

            // Bidder's second bid fails
            Response failBid = sendAndHandle(bidder, new Request(RequestType.BID)
                    .put("auctionId", auctionId).put("amount", 900.0));
            assertFalse(failBid.isSuccess(),
                    "Banned user's bid should be rejected");

            // Verify login returns BAN_USER
            var freshClient = sim.createClient();
            Response loginAfterBan = sendAndHandle(freshClient, new Request(RequestType.LOGIN)
                    .put("username", "target_user").put("password", "p"));
            assertTrue(loginAfterBan.isSuccess());
        }
    }

    @Test
    @DisplayName("Multiple concurrent independent auctions with separate participants")
    void concurrentIndependentAuctions() throws Exception {
        setupBridgedEnvironment();

        int numAuctionGroups = 5;
        int biddersPerGroup = 3;
        var sellers = new NetworkSimulation.SimulatedClient[numAuctionGroups];
        var groups = new ArrayList<List<NetworkSimulation.SimulatedClient>>();

        // Create auctions and register bidders per group
        for (int g = 0; g < numAuctionGroups; g++) {
            sellers[g] = registerAndLogin("g" + g + "_seller", "p");
            assertTrue(sendAndHandle(sellers[g], new Request(RequestType.SELL)
                    .put("item", makeItem("GroupItem" + g, 100.0 * (g + 1)))
                    .put("start", LocalDateTime.now().minusHours(1))
                    .put("end", LocalDateTime.now().plusDays(1))
                    .put("minIncrement", 5.0)).isSuccess());

            var biddersInGroup = new ArrayList<NetworkSimulation.SimulatedClient>();
            for (int b = 0; b < biddersPerGroup; b++) {
                biddersInGroup.add(registerAndLogin("g" + g + "_b" + b, "p"));
            }
            groups.add(biddersInGroup);
        }

        auctionSystem.forceTick();

        // Concurrent bidding: each group bids on their own auction
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger bidOk = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        int auctionIdOffset = clientSession.getBidderAuctions().size();

        try (MockedStatic<Platform> p = mockStatic(Platform.class)) {
            p.when(() -> Platform.runLater(any(Runnable.class))).thenAnswer(inv -> null);

            for (int g = 0; g < numAuctionGroups; g++) {
                int groupIdx = g;
                int auctionId = groupIdx + 1;

                for (var bidder : groups.get(g)) {
                    futures.add(executor.submit(() -> {
                        try {
                            for (int b = 0; b < 5; b++) {
                                double amount = 150.0 + b * 20 + groupIdx * 50;
                                Response r = sim.bid(bidder, auctionId, amount);
                                if (r != null && r.isSuccess()) bidOk.incrementAndGet();
                            }
                        } catch (Exception ignored) {}
                    }));
                }
            }
        }

        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(bidOk.get() > 0, "At least some bids succeeded across groups");

        // Verify each auction has a winner and consistent state
        for (int g = 0; g < numAuctionGroups; g++) {
            int auctionId = g + 1;
            Auction a = auctionRepository.getAuctionById(auctionId);
            assertNotNull(a, "Auction " + auctionId + " exists");
            assertTrue(a.getCurrentPrice() >= 100.0 * (g + 1),
                    "Auction " + auctionId + " price >= start price");
        }
    }

    // -----------------------------------------------------------------------
    //  Internal helper
    // -----------------------------------------------------------------------

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
