package a88.jbay.system;

import a88.jbay.common.auction.*;
import a88.jbay.common.item.Item;
import a88.jbay.common.user.UserData;
import a88.jbay.dao.*;
import a88.jbay.data.*;
import a88.jbay.server.DatabaseController;
import a88.jbay.system.update.UpdateSystem;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for BidSystem with a real H2 database.
 * Exercises the full persistence pipeline: BidSystem → BidRepository → DAO → H2.
 */
class BidSystemIntegrationTest {

    private static DatabaseController dbController;

    private BidSystem bidSystem;
    private AuctionRepository auctionRepository;
    private BidDAO bidDAO;
    private UserDAO userDAO;
    private ItemDAO itemDAO;
    private AuctionDAO auctionDAO;
    private UpdateSystem updateSystem;
    private BidRepository bidRepository;
    private AuctionFactory auctionFactory;

    @BeforeAll
    static void initDb() throws Exception {
        dbController = new DatabaseController();
        dbController.initializePool(
                "jdbc:h2:mem:inttest;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa", ""
        );
        try (var in = BidSystemIntegrationTest.class
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
    void setUp() throws Exception {
        // Clean tables
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
        bidRepository = new BidRepository(dbController, auctionDAO, bidDAO);
        auctionFactory = new AuctionFactory(itemDAO, userDAO, bidDAO);

        bidSystem = new BidSystem(
                auctionRepository, bidRepository, bidDAO, auctionDAO, updateSystem);
        bidSystem.setAutoBidDelayMs(0);
    }

    // ---------------------------------------------------------------
    // Helper: insert a user and return its ID
    // ---------------------------------------------------------------

    private int insertUser(String username, String password, String role) throws Exception {
        return executeInsert(
                "INSERT INTO users (username, password, role) VALUES (?, ?, ?)",
                username, password, role);
    }

    private int insertItem(String name, String type, String desc, double price) throws Exception {
        return executeInsert(
                "INSERT INTO items (name, type, desc, start_price, image) VALUES (?, ?, ?, ?, ?)",
                name, type, desc, price, new byte[]{});
    }

    private int insertAuction(int itemId, int sellerId, double startPrice,
                              double minIncrement, LocalDateTime start,
                              LocalDateTime end, String state) throws Exception {
        return executeInsert(
                "INSERT INTO auctions (item, seller, start_price, min_increment, start_time, end_time, state) VALUES (?, ?, ?, ?, ?, ?, ?)",
                itemId, sellerId, startPrice, minIncrement, start, end, state);
    }

    private int insertBid(int userId, int auctionId, double amount, LocalDateTime time) throws Exception {
        return executeInsert(
                "INSERT INTO bids (userid, auctionid, amt, time) VALUES (?, ?, ?, ?)",
                userId, auctionId, amount, time);
    }

    private int executeInsert(String sql, Object... params) throws Exception {
        try (var conn = dbController.getConnection();
             var stmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.length; i++) stmt.setObject(i + 1, params[i]);
            stmt.executeUpdate();
            try (var keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new Exception("No generated key");
            }
        }
    }

    // ---------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Single bid through BidSystem persists to H2 and updates auction state")
    void singleBidPersistsToDatabase() throws Exception {
        int sellerId = insertUser("seller", "pass", "SELLER");
        int bidderId = insertUser("bidder", "pass", "USER");
        int itemId = insertItem("Guitar", "MUSIC", "Acoustic guitar", 100.0);
        int auctionId = insertAuction(itemId, sellerId, 100.0, 5.0,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(1), "RUNNING");

        UserData seller = userDAO.findByUserId(sellerId);
        Item item = itemDAO.findItemById(itemId);

        Auction auction = new Auction(auctionId, item, seller,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(1));
        auction.setMinIncrement(5.0);
        auction.setAuctionState(AuctionState.RUNNING);
        auctionRepository.storeActiveAuction(auction);

        boolean result = bidSystem.placeBid(bidderId, auctionId, 150.0);
        assertTrue(result, "Bid should succeed");

        assertEquals(150.0, auction.getCurrentPrice(), 0.001);
        assertEquals("bidder", auction.getWinner());
        assertEquals(bidderId, auction.getWinnerId());

        List<BidData> history = bidDAO.findBidHistoryByAuctionId(auctionId);
        assertEquals(1, history.size());
        assertEquals(150.0, history.get(0).amount(), 0.001);
        assertEquals(bidderId, history.get(0).userId());
    }

    @Test
    @DisplayName("Sequential bids update price and winner correctly with real DB")
    void sequentialBidsUpdatePriceCorrectly() throws Exception {
        int sellerId = insertUser("seller", "pass", "SELLER");
        int alice = insertUser("alice", "pass", "USER");
        int bob = insertUser("bob", "pass", "USER");
        int itemId = insertItem("Painting", "ART", "Oil painting", 50.0);
        int auctionId = insertAuction(itemId, sellerId, 50.0, 0.0,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(1), "RUNNING");

        UserData seller = userDAO.findByUserId(sellerId);
        Item item = itemDAO.findItemById(itemId);
        Auction auction = new Auction(auctionId, item, seller,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(1));
        auction.setMinIncrement(0.0);
        auction.setAuctionState(AuctionState.RUNNING);
        auctionRepository.storeActiveAuction(auction);

        assertTrue(bidSystem.placeBid(alice, auctionId, 50.0));
        assertTrue(bidSystem.placeBid(bob, auctionId, 75.0));
        assertTrue(bidSystem.placeBid(alice, auctionId, 100.0));

        assertEquals(100.0, auction.getCurrentPrice(), 0.001);
        assertEquals("alice", auction.getWinner());
        assertEquals(alice, auction.getWinnerId());

        List<BidData> history = bidDAO.findBidHistoryByAuctionId(auctionId);
        assertEquals(3, history.size());
        assertEquals(List.of(50.0, 75.0, 100.0),
                history.stream().map(BidData::amount).toList());
    }

    @Test
    @DisplayName("Bid below current price is rejected by BidSystem and not persisted")
    void lowBidRejectedAndNotPersisted() throws Exception {
        int sellerId = insertUser("seller", "pass", "SELLER");
        int alice = insertUser("alice", "pass", "USER");
        int bob = insertUser("bob", "pass", "USER");
        int itemId = insertItem("Vase", "HOME", "Ceramic vase", 100.0);
        int auctionId = insertAuction(itemId, sellerId, 100.0, 0.0,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(1), "RUNNING");

        UserData seller = userDAO.findByUserId(sellerId);
        Item item = itemDAO.findItemById(itemId);
        Auction auction = new Auction(auctionId, item, seller,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(1));
        auction.setMinIncrement(0.0);
        auction.setAuctionState(AuctionState.RUNNING);
        auctionRepository.storeActiveAuction(auction);

        assertTrue(bidSystem.placeBid(alice, auctionId, 150.0));
        assertFalse(bidSystem.placeBid(bob, auctionId, 120.0));

        assertEquals(150.0, auction.getCurrentPrice(), 0.001);
        assertEquals(alice, auction.getWinnerId());

        List<BidData> history = bidDAO.findBidHistoryByAuctionId(auctionId);
        assertEquals(1, history.size());
        assertEquals(150.0, history.get(0).amount(), 0.001);
    }

    @Test
    @DisplayName("Auction reconstruction from DB matches pre-restart state")
    void reconstructedAuctionMatchesOriginal() throws Exception {
        int sellerId = insertUser("seller", "pass", "SELLER");
        int alice = insertUser("alice", "pass", "USER");
        int bob = insertUser("bob", "pass", "USER");
        int carol = insertUser("carol", "pass", "USER");
        int itemId = insertItem("Phone", "ELECTRONICS", "Smartphone", 200.0);
        int auctionId = insertAuction(itemId, sellerId, 200.0, 10.0,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(1), "RUNNING");

        UserData seller = userDAO.findByUserId(sellerId);
        Item item = itemDAO.findItemById(itemId);
        Auction auction = new Auction(auctionId, item, seller,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(1));
        auction.setMinIncrement(10.0);
        auction.setAuctionState(AuctionState.RUNNING);
        auctionRepository.storeActiveAuction(auction);

        bidSystem.placeBid(alice, auctionId, 200.0);
        bidSystem.placeBid(bob, auctionId, 250.0);
        bidSystem.placeBid(carol, auctionId, 300.0);

        double originalPrice = auction.getCurrentPrice();
        int originalWinnerId = auction.getWinnerId();
        int originalHistorySize = auction.getBidHistory().size();

        // Simulate server restart: reconstruct from DB
        var reconstructed = auctionFactory.reconstruct(
                new AuctionData(
                        auctionId,
                        itemId,
                        sellerId,
                        item.getInitPrice(),
                        originalPrice,
                        10.0,
                        null, // winnerId — reconstructed from bid replay
                        LocalDateTime.now().minusHours(1),
                        LocalDateTime.now().plusDays(1),
                        "RUNNING",
                        item.getName()
                ));

        assertNotNull(reconstructed);
        assertEquals(originalPrice, reconstructed.getCurrentPrice(), 0.001);
        assertEquals(originalWinnerId, reconstructed.getWinnerId());
        assertEquals(originalHistorySize, reconstructed.getBidHistory().size());

        List<BidTransaction> reconstructedBids = reconstructed.getBidHistory();
        assertEquals(200.0, reconstructedBids.get(0).getAmt(), 0.001);
        assertEquals(250.0, reconstructedBids.get(1).getAmt(), 0.001);
        assertEquals(300.0, reconstructedBids.get(2).getAmt(), 0.001);
    }
}
