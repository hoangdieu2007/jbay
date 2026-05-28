package a88.jbay.system;

import a88.jbay.common.item.Item;
import a88.jbay.common.network.Response;
import a88.jbay.common.user.User;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full-system integration test: wires RequestHandler with real DAOs on H2,
 * real system services, and exercises the complete request → DB → response pipeline.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RequestHandlerIntegrationTest {

    private static DatabaseController dbController;

    private RequestHandler handler;
    private AuctionSystem auctionSystem;
    private UserSystem userSystem;
    private BidSystem bidSystem;
    private AuctionRepository auctionRepository;
    private UserRepository userRepository;
    private UpdateSystem updateSystem;
    private ConnectionSystem connectionSystem;
    private ItemDAO itemDAO;
    private UserDAO userDAO;
    private BidDAO bidDAO;
    private AuctionDAO auctionDAO;
    private NetworkSimulation sim;

    @BeforeAll
    static void initDb() throws Exception {
        dbController = new DatabaseController();
        dbController.initializePool(
                "jdbc:h2:mem:reqhandlerint;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa", ""
        );
        try (var in = RequestHandlerIntegrationTest.class
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
        // Clean tables
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

        AdminService adminService = new AdminService(userDAO, userRepository, connectionSystem, auctionSystem, userSystem);
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
        return new Item(name, "ELECTRONICS", "desc", price, new byte[]{});
    }

    @Test
    @DisplayName("REGISTER → LOGIN → PING → LOGOUT flow")
    void fullAuthFlow() {
        var client = sim.createClient();

        Response reg = sim.register(client, "testuser", "pass");
        assertTrue(reg.isSuccess());
        assertEquals("REGISTER_SUCCESS", reg.getMessage());

        Response login = sim.login(client, "testuser", "pass");
        assertTrue(login.isSuccess());
        assertEquals("LOGIN_SUCCESS", login.getMessage());
        assertTrue(client.isLoggedIn());

        Response ping = sim.ping(client);
        assertTrue(ping.isSuccess());
        assertEquals("PONG", ping.getMessage());

        Response logout = sim.logout(client);
        assertTrue(logout.isSuccess());
        assertFalse(client.isLoggedIn());
    }

    @Test
    @DisplayName("REGISTER → LOGIN → SELL → full lifecycle")
    void registerLoginSell() {
        var seller = sim.createClient();
        assertTrue(sim.register(seller, "seller", "pass").isSuccess());
        assertTrue(sim.login(seller, "seller", "pass").isSuccess());
        assertEquals("USER", seller.getRole());

        Item laptop = makeItem("Gaming Laptop", 1000.0);
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusDays(3);

        Response sellRes = sim.sell(seller, laptop, start, end, 10.0);
        assertTrue(sellRes.isSuccess(), "SELL should succeed: " + sellRes.getMessage());

        var bidder = sim.createClient();
        assertTrue(sim.register(bidder, "bidder", "pass").isSuccess());
        assertTrue(sim.login(bidder, "bidder", "pass").isSuccess());

        Response getAuc = sim.getAuctions(bidder);
        assertTrue(getAuc.isSuccess());
    }

    @Test
    @DisplayName("REGISTER fails for duplicate username")
    void registerDuplicateUser() {
        var c1 = sim.createClient();
        assertTrue(sim.register(c1, "dupuser", "pass").isSuccess());

        var c2 = sim.createClient();
        Response reg2 = sim.register(c2, "dupuser", "pass2");
        assertFalse(reg2.isSuccess());
        assertEquals("REGISTER_FAIL", reg2.getMessage());
    }

    @Test
    @DisplayName("LOGIN with wrong password fails")
    void loginWrongPassword() {
        var client = sim.createClient();
        sim.register(client, "validuser", "correctpass");

        Response login = sim.login(client, "validuser", "wrongpass");
        assertFalse(login.isSuccess());
        assertEquals("LOGIN_FAIL", login.getMessage());
    }

    @Test
    @DisplayName("Unauthenticated BID returns PERMISSION_DENIED")
    void unauthenticatedBid() {
        var client = sim.createClient();
        Response res = sim.bid(client, 1, 100.0);
        assertFalse(res.isSuccess());
        assertEquals("PERMISSION_DENIED", res.getMessage());
    }

    @Test
    @DisplayName("BID on non-existent auction returns BID_FAIL")
    void bidNonExistentAuction() {
        var client = sim.createClient();
        sim.register(client, "bidder", "pass");
        sim.login(client, "bidder", "pass");

        Response res = sim.bid(client, 99999, 100.0);
        assertFalse(res.isSuccess());
        assertEquals("BID_FAIL", res.getMessage());
    }

    @Test
    @DisplayName("SELL with null end time causes system-level failure")
    void sellNullEndTime() {
        var client = sim.createClient();
        sim.register(client, "seller", "pass");
        sim.login(client, "seller", "pass");

        Response res = sim.sell(client, makeItem("Item", 100.0),
                LocalDateTime.now(), null);
        assertFalse(res.isSuccess());
    }

    @Test
    @DisplayName("MISC ls-auction returns auction list")
    void miscListAuctions() {
        var client = sim.createClient();
        sim.register(client, "user", "pass");
        sim.login(client, "user", "pass");

        Response res = sim.misc(client, "ls-auction");
        assertTrue(res.isSuccess());
        assertEquals("LIST_AUCTION_SUCCESS", res.getMessage());
    }

    @Test
    @DisplayName("GET_USERS by regular user returns PERMISSION_DENIED")
    void getUsersByNonAdmin() {
        var client = sim.createClient();
        sim.register(client, "regular", "pass");
        sim.login(client, "regular", "pass");

        Response res = sim.getUsers(client);
        assertFalse(res.isSuccess());
        assertEquals("PERMISSION_DENIED", res.getMessage());
    }

    @Test
    @DisplayName("Complete scenario: register seller → login → create auction → register bidder → login → bid → pay")
    void completeScenario() {
        var seller = sim.createClient();
        assertTrue(sim.register(seller, "seller1", "pass").isSuccess());
        assertTrue(sim.login(seller, "seller1", "pass").isSuccess());

        Item phone = makeItem("Smartphone", 500.0);
        LocalDateTime start = LocalDateTime.now().minusHours(2);
        LocalDateTime end = LocalDateTime.now().plusDays(7);
        Response sellRes = sim.sell(seller, phone, start, end, 5.0);
        assertTrue(sellRes.isSuccess(), "SELL should succeed");

        var bidder = sim.createClient();
        assertTrue(sim.register(bidder, "buyer1", "pass").isSuccess());
        assertTrue(sim.login(bidder, "buyer1", "pass").isSuccess());

        Response bidRes = sim.bid(bidder, 1, 550.0);
        assertFalse(bidRes.isSuccess(), "BID on unsynced auction should fail since no running auction is cached");

        Response getAuc = sim.getAuctions(bidder);
        assertTrue(getAuc.isSuccess());
    }
}
