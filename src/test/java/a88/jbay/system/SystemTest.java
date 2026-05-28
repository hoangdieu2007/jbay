package a88.jbay.system;

import a88.jbay.common.auction.*;
import a88.jbay.common.item.Item;
import a88.jbay.common.user.UserData;
import a88.jbay.dao.*;
import a88.jbay.data.*;
import a88.jbay.server.DatabaseController;
import a88.jbay.system.update.UpdateSystem;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * End-to-end system test for the server-side auction + bid pipeline.
 * Exercises: DAO → Repository → BidSystem → AuctionSystem → reconstruction,
 * all backed by a real H2 database with mocked network/UI layers.
 */
class SystemTest {

    private static DatabaseController dbController;

    private BidSystem bidSystem;
    private AuctionSystem auctionSystem;
    private AuctionRepository auctionRepository;
    private UserRepository userRepository;
    private BidDAO bidDAO;
    private UserDAO userDAO;
    private ItemDAO itemDAO;
    private AuctionDAO auctionDAO;
    private UpdateSystem updateSystem;
    private AuctionFactory auctionFactory;

    @BeforeAll
    static void initDb() throws Exception {
        dbController = new DatabaseController();
        dbController.initializePool(
                "jdbc:h2:mem:systest;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa", ""
        );
        try (var in = SystemTest.class.getResourceAsStream("/a88/jbay/db/schema-h2.sql")) {
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
    void setUp() throws Exception {
        try (var conn = dbController.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");
            stmt.execute("TRUNCATE TABLE bids");
            stmt.execute("TRUNCATE TABLE auctions");
            stmt.execute("TRUNCATE TABLE items");
            stmt.execute("TRUNCATE TABLE users");
            stmt.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }

        userDAO = new UserDAOImpl(dbController);
        itemDAO = new ItemDAOImpl(dbController);
        bidDAO = new BidDAOImpl(dbController);
        auctionDAO = new AuctionDAOImpl(dbController);
        updateSystem = mock(UpdateSystem.class);

        auctionRepository = new AuctionRepository(
                dbController, auctionDAO, itemDAO, userDAO, bidDAO);
        var bidRepository = new BidRepository(dbController, auctionDAO, bidDAO);
        userRepository = new UserRepository(userDAO);
        auctionFactory = new AuctionFactory(itemDAO, userDAO, bidDAO);

        bidSystem = new BidSystem(
                auctionRepository, bidRepository, bidDAO, auctionDAO, updateSystem);
        bidSystem.setAutoBidDelayMs(0);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private int insertUser(String username, String password, String role) throws Exception {
        try (var conn = dbController.getConnection();
             var stmt = conn.prepareStatement(
                     "INSERT INTO users (username, password, role) VALUES (?, ?, ?)",
                     java.sql.Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, role);
            stmt.executeUpdate();
            try (var keys = stmt.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    // ---------------------------------------------------------------
    // Test: full lifecycle — create auction, bid, restart, verify
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Full auction lifecycle: create → multi-bid → restart → verify persistence")
    void auctionLifecycleCreateBidRestartVerify() throws Exception {
        int aliceId = insertUser("alice", "pass", "SELLER");
        int bobId = insertUser("bob", "pass", "BIDDER");
        int carolId = insertUser("carol", "pass", "BIDDER");

        // Alice creates an item and auction
        Item item = new Item("Laptop", "ELECTRONICS", "Gaming laptop", 500.0, new byte[]{});
        int itemId = itemDAO.insertItem(item);

        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusDays(3);

        // Persist auction to DB first, then construct Auction object with the real ID
        int auctionId;
        try (var conn = dbController.getConnection();
             var stmt = conn.prepareStatement(
                     "INSERT INTO auctions (item, seller, start_price, min_increment, start_time, end_time, state) VALUES (?, ?, ?, ?, ?, ?, ?)",
                     java.sql.Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, itemId);
            stmt.setInt(2, aliceId);
            stmt.setDouble(3, 500.0);
            stmt.setDouble(4, 10.0);
            stmt.setObject(5, start);
            stmt.setObject(6, end);
            stmt.setString(7, "RUNNING");
            stmt.executeUpdate();
            try (var keys = stmt.getGeneratedKeys()) {
                keys.next();
                auctionId = keys.getInt(1);
            }
        }

        UserData alice = userDAO.findByUserId(aliceId);
        Item persistedItem = itemDAO.findItemById(itemId);
        Auction auction = new Auction(auctionId, persistedItem, alice, start, end);
        auction.setMinIncrement(10.0);
        auction.setAuctionState(AuctionState.RUNNING);
        auctionRepository.storeActiveAuction(auction);

        // Bob bids 550
        assertTrue(bidSystem.placeBid(bobId, auctionId, 550.0),
                "Bob's bid of 550 should succeed");
        assertEquals("bob", auction.getWinner());
        assertEquals(550.0, auction.getCurrentPrice(), 0.001);

        // Carol bids 600
        assertTrue(bidSystem.placeBid(carolId, auctionId, 600.0),
                "Carol's bid of 600 should succeed");
        assertEquals("carol", auction.getWinner());
        assertEquals(600.0, auction.getCurrentPrice(), 0.001);

        // Bob bids 650
        assertTrue(bidSystem.placeBid(bobId, auctionId, 650.0),
                "Bob's bid of 650 should succeed");
        assertEquals("bob", auction.getWinner());
        assertEquals(650.0, auction.getCurrentPrice(), 0.001);

        // Bob bids 620 (too low) → rejected
        assertFalse(bidSystem.placeBid(bobId, auctionId, 620.0),
                "Bob's low bid should be rejected");

        // Simulate server restart: clear cache and reconstruct
        auctionRepository.removeActiveAuction(auctionId);

        var reloaded = auctionRepository.getAuctionById(auctionId);
        assertNotNull(reloaded, "Auction should be loadable from DB after restart");

        assertEquals(650.0, reloaded.getCurrentPrice(), 0.001,
                "Reconstructed price should be 650");
        assertEquals(bobId, reloaded.getWinnerId(),
                "Reconstructed winner should be Bob");
        assertEquals(3, reloaded.getBidHistory().size(),
                "Reconstructed should have 3 bids");

        List<BidTransaction> bids = reloaded.getBidHistory();
        assertEquals(550.0, bids.get(0).getAmt(), 0.001);
        assertEquals(600.0, bids.get(1).getAmt(), 0.001);
        assertEquals(650.0, bids.get(2).getAmt(), 0.001);
    }

    @Test
    @DisplayName("Multiple concurrent auctions with bids: each independently correct after restart")
    void multipleConcurrentAuctionRestart() throws Exception {
        // Create users
        int seller1 = insertUser("seller1", "pass", "SELLER");
        int seller2 = insertUser("seller2", "pass", "SELLER");
        int bidder1 = insertUser("bidder1", "pass", "BIDDER");
        int bidder2 = insertUser("bidder2", "pass", "BIDDER");

        // Create two auctions
        Item item1 = new Item("Camera", "ELECTRONICS", "DSLR", 300.0, new byte[]{});
        Item item2 = new Item("Book", "BOOKS", "Novel", 15.0, new byte[]{});

        int item1Id = itemDAO.insertItem(item1);
        int item2Id = itemDAO.insertItem(item2);

        var start = LocalDateTime.now().minusHours(2);
        var end = LocalDateTime.now().plusDays(1);

        int auction1Id;
        int auction2Id;
        try (var conn = dbController.getConnection();
             var stmt = conn.prepareStatement(
                     "INSERT INTO auctions (item, seller, start_price, min_increment, start_time, end_time, state) VALUES (?, ?, ?, ?, ?, ?, ?)",
                     java.sql.Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, item1Id);
            stmt.setInt(2, seller1);
            stmt.setDouble(3, 300.0);
            stmt.setDouble(4, 0.0);
            stmt.setObject(5, start);
            stmt.setObject(6, end);
            stmt.setString(7, "RUNNING");
            stmt.executeUpdate();
            try (var keys = stmt.getGeneratedKeys()) { keys.next(); auction1Id = keys.getInt(1); }
        }
        try (var conn = dbController.getConnection();
             var stmt = conn.prepareStatement(
                     "INSERT INTO auctions (item, seller, start_price, min_increment, start_time, end_time, state) VALUES (?, ?, ?, ?, ?, ?, ?)",
                     java.sql.Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, item2Id);
            stmt.setInt(2, seller2);
            stmt.setDouble(3, 15.0);
            stmt.setDouble(4, 0.0);
            stmt.setObject(5, start);
            stmt.setObject(6, end);
            stmt.setString(7, "RUNNING");
            stmt.executeUpdate();
            try (var keys = stmt.getGeneratedKeys()) { keys.next(); auction2Id = keys.getInt(1); }
        }

        // Load auctions into cache
        UserData s1 = userDAO.findByUserId(seller1);
        UserData s2 = userDAO.findByUserId(seller2);
        Item i1 = itemDAO.findItemById(item1Id);
        Item i2 = itemDAO.findItemById(item2Id);

        Auction auc1 = new Auction(auction1Id, i1, s1, start, end);
        auc1.setAuctionState(AuctionState.RUNNING);
        auctionRepository.storeActiveAuction(auc1);

        Auction auc2 = new Auction(auction2Id, i2, s2, start, end);
        auc2.setAuctionState(AuctionState.RUNNING);
        auctionRepository.storeActiveAuction(auc2);

        // Bid on both auctions
        bidSystem.placeBid(bidder1, auction1Id, 350.0);
        bidSystem.placeBid(bidder2, auction1Id, 400.0);
        bidSystem.placeBid(bidder1, auction2Id, 20.0);
        bidSystem.placeBid(bidder2, auction2Id, 25.0);

        assertEquals(400.0, auc1.getCurrentPrice(), 0.001);
        assertEquals(bidder2, auc1.getWinnerId());
        assertEquals(25.0, auc2.getCurrentPrice(), 0.001);
        assertEquals(bidder2, auc2.getWinnerId());

        // "Restart" — clear cache and reload
        auctionRepository.removeActiveAuction(auction1Id);
        auctionRepository.removeActiveAuction(auction2Id);

        var r1 = auctionRepository.getAuctionById(auction1Id);
        var r2 = auctionRepository.getAuctionById(auction2Id);

        assertNotNull(r1);
        assertNotNull(r2);
        assertEquals(400.0, r1.getCurrentPrice(), 0.001);
        assertEquals(bidder2, r1.getWinnerId());
        assertEquals(25.0, r2.getCurrentPrice(), 0.001);
        assertEquals(bidder2, r2.getWinnerId());
        assertEquals(2, r1.getBidHistory().size());
        assertEquals(2, r2.getBidHistory().size());
    }
}
