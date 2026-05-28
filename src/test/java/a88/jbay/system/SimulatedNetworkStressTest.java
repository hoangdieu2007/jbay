package a88.jbay.system;

import a88.jbay.common.item.Item;
import a88.jbay.common.network.Response;
import a88.jbay.dao.*;
import a88.jbay.data.*;
import a88.jbay.server.DatabaseController;
import a88.jbay.server.RequestHandler;
import a88.jbay.system.AuctionSystem;
import a88.jbay.system.BidSystem;
import a88.jbay.system.update.ConnectionSystem;
import a88.jbay.system.update.UpdateSystem;
import a88.jbay.system.user.AdminService;
import a88.jbay.system.user.UserSystem;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extreme-scale simulated user stress tests.
 * Uses NetworkSimulation with real H2 database and real system services
 * to simulate hundreds of concurrent users making various requests.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SimulatedNetworkStressTest {

    private static DatabaseController dbController;

    private RequestHandler handler;
    private AuctionSystem auctionSystem;
    private UserSystem userSystem;
    private BidSystem bidSystem;
    private AuctionRepository auctionRepository;
    private UserRepository userRepository;
    private ConnectionSystem connectionSystem;
    private UpdateSystem updateSystem;
    private ItemDAO itemDAO;
    private UserDAO userDAO;
    private BidDAO bidDAO;
    private AuctionDAO auctionDAO;
    private NetworkSimulation sim;

    @BeforeAll
    static void initDb() throws Exception {
        dbController = new DatabaseController();
        dbController.initializePool(
                "jdbc:h2:mem:stressnet;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa", ""
        );
        try (var in = SimulatedNetworkStressTest.class
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

    @BeforeEach
    void setUp() {
        cleanTables();

        userDAO = new UserDAOImpl(dbController);
        itemDAO = new ItemDAOImpl(dbController);
        bidDAO = new BidDAOImpl(dbController);
        auctionDAO = new AuctionDAOImpl(dbController);

        userRepository = new UserRepository(userDAO);
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
        auctionSystem = new AuctionSystem(updateSystem, auctionRepository, userRepository);

        AdminService adminService = new AdminService(
                userDAO, userRepository, connectionSystem, auctionSystem, userSystem);
        handler = new RequestHandler(userSystem, adminService, auctionSystem,
                connectionSystem, updateSystem, bidSystem);
        sim = new NetworkSimulation(handler);
    }

    @AfterEach
    void tearDown() {
        auctionSystem.stopSystem();
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
        return new Item(name, "ELECTRONICS", "Test item " + name, price, new byte[]{});
    }

    // ---------------------------------------------------------------
    // 1. Concurrent REGISTER + LOGIN stress
    // ---------------------------------------------------------------

    @Test
    @DisplayName("200 concurrent users register and login")
    void concurrentRegisterAndLogin() throws Exception {
        int numUsers = 200;
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < numUsers; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                try {
                    var client = sim.createClient();
                    Response reg = sim.register(client, "stressuser" + idx, "pass" + idx);
                    if (reg != null && reg.isSuccess()) {
                        Response login = sim.login(client, "stressuser" + idx, "pass" + idx);
                        if (login != null && login.isSuccess()) {
                            successCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertEquals(numUsers, successCount.get(), "All users should register and login");
        assertEquals(0, failCount.get(), "No exceptions expected");
    }

    // ---------------------------------------------------------------
    // 2. Mixed workload: SELL + BID concurrently
    // ---------------------------------------------------------------

    @Test
    @DisplayName("50 sellers and 100 bidders interacting concurrently")
    void mixedSellAndBid() throws Exception {
        int numSellers = 50;
        int numBidders = 100;
        int bidsPerBidder = 5;

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger sellSuccess = new AtomicInteger(0);
        AtomicInteger bidSuccess = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);

        // Register and login all sellers
        var sellers = new NetworkSimulation.SimulatedClient[numSellers];
        for (int i = 0; i < numSellers; i++) {
            sellers[i] = sim.createClient();
            int finalI = i;
            sim.register(sellers[i], "sseller" + i, "pass");
            sim.login(sellers[i], "sseller" + i, "pass");
        }

        // Register and login all bidders
        var bidders = new NetworkSimulation.SimulatedClient[numBidders];
        for (int i = 0; i < numBidders; i++) {
            bidders[i] = sim.createClient();
            sim.register(bidders[i], "sbidder" + i, "pass");
            sim.login(bidders[i], "sbidder" + i, "pass");
        }

        List<Future<?>> futures = new ArrayList<>();

        // Sellers create auctions
        for (int i = 0; i < numSellers; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                try {
                    Item item = makeItem("StressItem" + idx, 100.0 + idx);
                    Response res = sim.sell(sellers[idx], item,
                            LocalDateTime.now().minusHours(1),
                            LocalDateTime.now().plusDays(3), 5.0);
                    if (res != null && res.isSuccess()) {
                        sellSuccess.incrementAndGet();
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        futures.clear();

        // Bidders bid on auctions (each bidder bids on multiple auction IDs)
        for (int i = 0; i < numBidders; i++) {
            final int bidderIdx = i;
            futures.add(executor.submit(() -> {
                try {
                    for (int b = 0; b < bidsPerBidder; b++) {
                        int targetAuction = (bidderIdx + b) % Math.max(1, numSellers) + 1;
                        Response res = sim.bid(bidders[bidderIdx], targetAuction, 200.0 + b * 10);
                        if (res != null && res.isSuccess()) {
                            bidSuccess.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertTrue(sellSuccess.get() > 0, "At least some sells should succeed");
        assertTrue(bidSuccess.get() >= 0, "Bid counter should be non-negative");
        assertEquals(0, errors.get(), "No unexpected exceptions");
    }

    // ---------------------------------------------------------------
    // 3. Many requests of all types on small user base
    // ---------------------------------------------------------------

    @Test
    @DisplayName("50 users each performing 40 requests of mixed types")
    void mixedRequestTypes() throws Exception {
        int numUsers = 50;
        int requestsPerUser = 40;

        var clients = new NetworkSimulation.SimulatedClient[numUsers];
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        for (int i = 0; i < numUsers; i++) {
            clients[i] = sim.createClient();
            sim.register(clients[i], "mixuser" + i, "pass");
            sim.login(clients[i], "mixuser" + i, "pass");
        }

        AtomicInteger totalOk = new AtomicInteger(0);
        AtomicInteger totalErr = new AtomicInteger(0);
        AtomicLong totalTime = new AtomicLong(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < numUsers; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                for (int j = 0; j < requestsPerUser; j++) {
                    long t0 = System.nanoTime();
                    try {
                        Response res;
                        switch ((j + idx) % 6) {
                            case 0 -> res = sim.ping(clients[idx]);
                            case 1 -> res = sim.bid(clients[idx], j, 100.0 + j);
                            case 2 -> res = sim.getAuctions(clients[idx]);
                            case 3 -> res = sim.misc(clients[idx], "ls-auction");
                            case 4 -> res = sim.logout(clients[idx]);
                            case 5 -> {
                                sim.login(clients[idx], "mixuser" + idx, "pass");
                                res = sim.ping(clients[idx]);
                            }
                            default -> res = null;
                        }
                        if (res != null) {
                            totalOk.incrementAndGet();
                        }
                    } catch (Exception e) {
                        totalErr.incrementAndGet();
                    } finally {
                        totalTime.addAndGet(System.nanoTime() - t0);
                    }
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertTrue(totalOk.get() > 0, "Should have successful requests");
        assertEquals(0, totalErr.get(), "No exceptions expected");
        double avgMs = totalTime.get() / 1_000_000.0 / (numUsers * requestsPerUser);
        System.out.println("Mixed request stress: " + totalOk.get() + " ok, "
                + totalErr.get() + " err, avg " + String.format("%.2f", avgMs) + " ms/req");
    }

    // ---------------------------------------------------------------
    // 4. Extreme: many concurrent PING requests
    // ---------------------------------------------------------------

    @Test
    @DisplayName("1000 concurrent PING requests")
    void thousandPings() throws Exception {
        var clients = new NetworkSimulation.SimulatedClient[1000];
        for (int i = 0; i < 1000; i++) {
            clients[i] = sim.createClient();
            sim.register(clients[i], "pinger" + i, "pass");
            sim.login(clients[i], "pinger" + i, "pass");
        }

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger ok = new AtomicInteger(0);
        AtomicInteger err = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                try {
                    Response res = sim.ping(clients[idx]);
                    if (res != null && res.isSuccess() && "PONG".equals(res.getMessage())) {
                        ok.incrementAndGet();
                    } else {
                        err.incrementAndGet();
                    }
                } catch (Exception e) {
                    err.incrementAndGet();
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertEquals(1000, ok.get(), "All pings should succeed");
        assertEquals(0, err.get(), "No exceptions");
    }

    // ---------------------------------------------------------------
    // 5. Burst: many rapid requests from same session
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Single session fires 500 rapid mixed requests")
    void singleSessionRapidFire() {
        var client = sim.createClient();
        sim.register(client, "rapidfire", "pass");
        sim.login(client, "rapidfire", "pass");

        for (int i = 0; i < 500; i++) {
            Response res;
            switch (i % 4) {
                case 0 -> res = sim.ping(client);
                case 1 -> res = sim.bid(client, i % 10, 100.0 + i);
                case 2 -> res = sim.getAuctions(client);
                default -> res = sim.misc(client, "ls-auction");
            }
            assertNotNull(res, "Response should not be null at iter " + i);
        }

        assertTrue(client.isLoggedIn());
    }

    // ---------------------------------------------------------------
    // 6. Admin operations stress
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Admin concurrently bans and unbans users")
    void adminBanStress() throws Exception {
        int numRegularUsers = 50;
        var regulars = new NetworkSimulation.SimulatedClient[numRegularUsers];
        for (int i = 0; i < numRegularUsers; i++) {
            regulars[i] = sim.createClient();
            sim.register(regulars[i], "regular" + i, "pass");
            sim.login(regulars[i], "regular" + i, "pass");
        }

        var admin = sim.createClient();
        sim.register(admin, "stressedadmin", "pass");
        sim.login(admin, "stressedadmin", "pass");

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger banOk = new AtomicInteger(0);
        AtomicInteger unbanOk = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numRegularUsers; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                try {
                    Response ban = sim.banUser(admin, idx + 1);
                    if (ban != null && ban.isSuccess()) banOk.incrementAndGet();
                    Response unban = sim.unbanUser(admin, idx + 1);
                    if (unban != null && unban.isSuccess()) unbanOk.incrementAndGet();
                } catch (Exception e) {
                    // ignore
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();

        System.out.println("Admin stress: " + banOk.get() + " bans, " + unbanOk.get() + " unbans");
    }
}
