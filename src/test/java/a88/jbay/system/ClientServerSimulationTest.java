package a88.jbay.system;

import a88.jbay.client.ClientSession;
import a88.jbay.client.ResponseHandler;
import a88.jbay.client.ServerConnection;
import a88.jbay.dao.*;
import a88.jbay.data.*;
import a88.jbay.common.auction.Auction;
import a88.jbay.common.item.Item;
import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import a88.jbay.common.network.Response;
import a88.jbay.common.user.User;
import a88.jbay.common.user.role.Role;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Full client-server simulation: a real RequestHandler + real systems on H2
 * talk to a real ClientSession + ResponseHandler through a synchronous bridge.
 * No sockets. No timing races.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientServerSimulationTest {

    private static DatabaseController dbController;

    private RequestHandler requestHandler;
    private AuctionSystem auctionSystem;
    private BidSystem bidSystem;
    private ConnectionSystem connectionSystem;

    private ClientSession clientSession;
    private ResponseHandler responseHandler;
    private ControllerProvider controllerProvider;

    private NetworkSimulation sim;

    /**
     * A ConnectionSystem that route all sends synchronously to the client ResponseHandler.
     * Register/unregister still works on the underlying connection system.
     */
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

    @BeforeAll
    static void initDb() throws Exception {
        dbController = new DatabaseController();
        dbController.initializePool(
                "jdbc:h2:mem:clientserver;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa", ""
        );
        try (var in = ClientServerSimulationTest.class
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

        var userDAO = new UserDAOImpl(dbController);
        var itemDAO = new ItemDAOImpl(dbController);
        var bidDAO = new BidDAOImpl(dbController);
        var auctionDAO = new AuctionDAOImpl(dbController);

        var userRepository = new UserRepository(userDAO);
        var auctionRepository = new AuctionRepository(
                dbController, auctionDAO, itemDAO, userDAO, bidDAO);

        // Client side first (needed for the bridge)
        clientSession = new ClientSession();
        controllerProvider = mock(ControllerProvider.class);
        var viewManager = mock(ViewManager.class);
        responseHandler = new ResponseHandler(clientSession, controllerProvider, viewManager);
        when(controllerProvider.getController(ClientRegisterController.class))
                .thenReturn(mock(ClientRegisterController.class));

        // Server side with synchronous bridge
        connectionSystem = new BridgedConnectionSystem(responseHandler);
        var updateSystem = new UpdateSystem(connectionSystem);
        userSystem = new UserSystem(userRepository);
        bidSystem = new BidSystem(
                auctionRepository,
                new BidRepository(dbController, auctionDAO, bidDAO),
                bidDAO, auctionDAO, updateSystem);
        bidSystem.setAutoBidDelayMs(0);
        auctionSystem = new AuctionSystem(updateSystem, auctionRepository, userRepository);

        var adminService = new AdminService(userDAO, userRepository, connectionSystem, auctionSystem, userSystem);
        requestHandler = new RequestHandler(userSystem, adminService, auctionSystem,
                connectionSystem, updateSystem, bidSystem);
        sim = new NetworkSimulation(requestHandler);
    }

    private UserSystem userSystem;

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

    /** Send a request through the server and feed the response to the client handler. */
    private Response sendAndHandle(NetworkSimulation.SimulatedClient client, Request request) {
        Response response = sim.send(client, request);
        if (response != null && !"GET_AUCTIONS_SUCCESS".equals(response.getMessage())
                && !"GET_USERS_SUCCESS".equals(response.getMessage())) {
            responseHandler.handle(response);
        }
        return response;
    }

    // ===== TESTS =====

    @Test
    @DisplayName("REGISTER → client register label updated")
    void registerUpdatesClientLabel() {
        var client = sim.createClient();
        Response reg = sendAndHandle(client, new Request(RequestType.REGISTER)
                .put("username", "alice")
                .put("password", "pass"));

        assertTrue(reg.isSuccess());
        assertEquals("REGISTER_SUCCESS", reg.getMessage());
        // The sync response updates the register label
    }

    @Test
    @DisplayName("LOGIN → client session has user")
    void loginSetsSessionUser() {
        var client = sim.createClient();
        assertTrue(sim.register(client, "bob", "pass").isSuccess());

        when(controllerProvider.getController(ClientLoginController.class))
                .thenReturn(mock(ClientLoginController.class));

        try (MockedStatic<ServerConnection> sc = mockStatic(ServerConnection.class)) {
            sc.when(ServerConnection::getInstance).thenReturn(mock(ServerConnection.class));
            sendAndHandle(client, new Request(RequestType.LOGIN)
                    .put("username", "bob")
                    .put("password", "pass"));
        }

        assertEquals("bob", clientSession.getUser().getUsername());
        assertEquals(Role.USER, clientSession.getUser().getRole());
        assertTrue(clientSession.getUser().getId() > 0);
        verify(controllerProvider.getController(ClientLoginController.class))
                .updateLoginLabel("Login successful");
    }

    @Test
    @DisplayName("LOGIN with wrong password → fail label")
    void loginFailUpdatesLabel() {
        var client = sim.createClient();
        sim.register(client, "carol", "pass");

        when(controllerProvider.getController(ClientLoginController.class))
                .thenReturn(mock(ClientLoginController.class));

        sendAndHandle(client, new Request(RequestType.LOGIN)
                .put("username", "carol")
                .put("password", "wrong"));

        assertEquals(-1, clientSession.getUser().getId());
        verify(controllerProvider.getController(ClientLoginController.class))
                .updateLoginLabel("Login failed");
    }

    @Test
    @DisplayName("LOGIN → GET_AUCTIONS → seller auction list populated")
    void getAuctionsPopulatesSellerAuctions() {
        var seller = sim.createClient();
        assertTrue(sim.register(seller, "dave", "pass").isSuccess());

        when(controllerProvider.getController(ClientLoginController.class))
                .thenReturn(mock(ClientLoginController.class));

        try (MockedStatic<ServerConnection> sc = mockStatic(ServerConnection.class)) {
            sc.when(ServerConnection::getInstance).thenReturn(mock(ServerConnection.class));
            sendAndHandle(seller, new Request(RequestType.LOGIN)
                    .put("username", "dave")
                    .put("password", "pass"));
        }

        Item laptop = makeItem("Laptop", 1000.0);
        Response sellRes = sendAndHandle(seller, new Request(RequestType.SELL)
                .put("item", laptop)
                .put("start", LocalDateTime.now().minusHours(1))
                .put("end", LocalDateTime.now().plusDays(3))
                .put("minIncrement", 10.0));
        assertTrue(sellRes.isSuccess());

        // GET_AUCTIONS triggers synchronous SELLER_AUCTION_LIST via the bridge
        Response getRes = sendAndHandle(seller, new Request(RequestType.GET_AUCTIONS)
                .put("userId", seller.getUserId()));
        assertTrue(getRes.isSuccess());

        assertFalse(clientSession.getSellerAuctions().isEmpty(),
                "Seller should see their auction in seller list");
    }

    @Test
    @DisplayName("Full two-user scenario")
    void twoUserFullScenario() {
        // Seller side
        var seller = sim.createClient();
        assertTrue(sim.register(seller, "seller_main", "pass").isSuccess());

        when(controllerProvider.getController(ClientLoginController.class))
                .thenReturn(mock(ClientLoginController.class));

        try (MockedStatic<ServerConnection> sc = mockStatic(ServerConnection.class)) {
            sc.when(ServerConnection::getInstance).thenReturn(mock(ServerConnection.class));
            sendAndHandle(seller, new Request(RequestType.LOGIN)
                    .put("username", "seller_main")
                    .put("password", "pass"));
        }

        Item phone = makeItem("Phone", 500.0);
        assertTrue(sendAndHandle(seller, new Request(RequestType.SELL)
                .put("item", phone)
                .put("start", LocalDateTime.now().minusHours(2))
                .put("end", LocalDateTime.now().plusDays(7))
                .put("minIncrement", 5.0)).isSuccess());

        auctionSystem.forceTick();

        // GET_AUCTIONS → SELLER_AUCTION_LIST via bridge
        sendAndHandle(seller, new Request(RequestType.GET_AUCTIONS)
                .put("userId", seller.getUserId()));
        assertFalse(clientSession.getSellerAuctions().isEmpty());

        // Bidder side
        var bidder = sim.createClient();
        assertTrue(sim.register(bidder, "bidder_main", "pass").isSuccess());

        when(controllerProvider.getController(ClientLoginController.class))
                .thenReturn(mock(ClientLoginController.class));

        try (MockedStatic<ServerConnection> sc = mockStatic(ServerConnection.class)) {
            sc.when(ServerConnection::getInstance).thenReturn(mock(ServerConnection.class));
            sendAndHandle(bidder, new Request(RequestType.LOGIN)
                    .put("username", "bidder_main")
                    .put("password", "pass"));
        }

        // GET_AUCTIONS → ACTIVE_AUCTION_LIST via bridge
        clientSession.getBidderAuctions().clear();
        sendAndHandle(bidder, new Request(RequestType.GET_AUCTIONS)
                .put("userId", bidder.getUserId()));
        assertFalse(clientSession.getBidderAuctions().isEmpty(),
                "Bidder should see the auction");

        // Bid (mock Platform to avoid JavaFX toolkit requirement in handleAuctionUpdateNotify)
        try (MockedStatic<Platform> p = mockStatic(Platform.class)) {
            p.when(() -> Platform.runLater(any(Runnable.class))).thenAnswer(inv -> null);

            int auctionId = clientSession.getBidderAuctions().keySet().iterator().next();
            Response bidRes = sendAndHandle(bidder, new Request(RequestType.BID)
                    .put("auctionId", auctionId)
                    .put("amount", 550.0));
            assertTrue(bidRes.isSuccess());

            // After bid, the AUCTION_UPDATE broadcast was routed synchronously
            Auction updated = clientSession.getBidderAuctions().get(auctionId);
            assertNotNull(updated);
            assertEquals(550.0, updated.getCurrentPrice(), 0.001,
                    "Bidder should see updated price");
        }
    }

    @Test
    @DisplayName("LOGOUT resets client session")
    void logoutResetsSession() {
        var client = sim.createClient();
        assertTrue(sim.register(client, "logout_user", "pass").isSuccess());

        when(controllerProvider.getController(ClientLoginController.class))
                .thenReturn(mock(ClientLoginController.class));

        try (MockedStatic<ServerConnection> sc = mockStatic(ServerConnection.class)) {
            sc.when(ServerConnection::getInstance).thenReturn(mock(ServerConnection.class));
            sendAndHandle(client, new Request(RequestType.LOGIN)
                    .put("username", "logout_user")
                    .put("password", "pass"));
        }

        assertEquals("logout_user", clientSession.getUser().getUsername());

        Response logoutRes = sendAndHandle(client, new Request(RequestType.LOGOUT));
        assertTrue(logoutRes.isSuccess());

        assertEquals(-1, clientSession.getUser().getId());
        assertTrue(clientSession.getBidderAuctions().isEmpty());
        verify(controllerProvider).clearControllers();
    }

    @Test
    @DisplayName("Banned user receives BAN_USER")
    void bannedUserReceivesBan() {
        var client = sim.createClient();
        assertTrue(sim.register(client, "willbebanned", "pass").isSuccess());

        when(controllerProvider.getController(ClientLoginController.class))
                .thenReturn(mock(ClientLoginController.class));

        try (MockedStatic<ServerConnection> sc = mockStatic(ServerConnection.class);
             MockedStatic<Platform> p = mockStatic(Platform.class)) {
            sc.when(ServerConnection::getInstance).thenReturn(mock(ServerConnection.class));

            // First login succeeds
            Response loginRes = sendAndHandle(client, new Request(RequestType.LOGIN)
                    .put("username", "willbebanned")
                    .put("password", "pass"));
            assertTrue(loginRes.isSuccess());

            // Admin bans the user
            var admin = sim.createClient();
            sim.send(admin, new Request(RequestType.REGISTER)
                    .put("username", "admin_ban")
                    .put("password", "pass")
                    .put("role", "ADMIN"));
            sim.login(admin, "admin_ban", "pass");
            sim.banUser(admin, client.getUserId());

            // Create a fresh client (no existing sessionId) to retry login
            var bannedClient = sim.createClient();
            Response banRes = sendAndHandle(bannedClient, new Request(RequestType.LOGIN)
                    .put("username", "willbebanned")
                    .put("password", "pass"));
            assertTrue(banRes.isSuccess());
            assertEquals("BAN_USER", banRes.getMessage());
        }
    }

    @Test
    @DisplayName("Register broadcasts NEW_USER_REGISTERED to admin")
    void newUserRegisteredBroadcastToAdmin() {
        var admin = sim.createClient();
        assertTrue(sim.send(admin, new Request(RequestType.REGISTER)
                .put("username", "existing_admin")
                .put("password", "pass")
                .put("role", "ADMIN")).isSuccess());

        when(controllerProvider.getController(ClientLoginController.class))
                .thenReturn(mock(ClientLoginController.class));

        clientSession.getAdminUsers().clear();

        // Register a new user → server broadcasts NEW_USER_REGISTERED → bridge routes it
        // Keep Platform mock active during registration so the broadcast doesn't require JavaFX
        try (MockedStatic<ServerConnection> sc = mockStatic(ServerConnection.class);
             MockedStatic<Platform> p = mockStatic(Platform.class)) {
            p.when(() -> Platform.runLater(any(Runnable.class)))
                    .thenAnswer(inv -> { ((Runnable) inv.getArgument(0)).run(); return null; });
            sc.when(ServerConnection::getInstance).thenReturn(mock(ServerConnection.class));

            sendAndHandle(admin, new Request(RequestType.LOGIN)
                    .put("username", "existing_admin")
                    .put("password", "pass"));

            var newbie = sim.createClient();
            sendAndHandle(newbie, new Request(RequestType.REGISTER)
                    .put("username", "newcomer")
                    .put("password", "pass"));
        }

        boolean found = clientSession.getAdminUsers().values().stream()
                .anyMatch(u -> "newcomer".equals(u.getUsername()));
        assertTrue(found, "Admin should see newly registered user via broadcast");
    }

    @Test
    @DisplayName("GET_USERS sends ADMIN_USER_LIST to admin")
    void adminGetsUserList() {
        // Register a normal user so there is someone to list
        var normal = sim.createClient();
        assertTrue(sim.register(normal, "regular_user", "pass").isSuccess());

        var admin = sim.createClient();
        assertTrue(sim.send(admin, new Request(RequestType.REGISTER)
                .put("username", "supervisor")
                .put("password", "pass")
                .put("role", "ADMIN")).isSuccess());

        when(controllerProvider.getController(ClientLoginController.class))
                .thenReturn(mock(ClientLoginController.class));

        try (MockedStatic<ServerConnection> sc = mockStatic(ServerConnection.class)) {
            sc.when(ServerConnection::getInstance).thenReturn(mock(ServerConnection.class));
            sendAndHandle(admin, new Request(RequestType.LOGIN)
                    .put("username", "supervisor")
                    .put("password", "pass"));
        }

        // GET_USERS triggers ADMIN_USER_LIST via bridge
        Response getUsers = sendAndHandle(admin, new Request(RequestType.GET_USERS));
        assertTrue(getUsers.isSuccess());

        assertFalse(clientSession.getAdminUsers().isEmpty(),
                "Admin should see users in their user list");
    }
}
