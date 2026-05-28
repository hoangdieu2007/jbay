package a88.jbay.dao;

import a88.jbay.server.DatabaseController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

public abstract class DaoTestBase {

    protected static DatabaseController dbController;

    @BeforeAll
    static void initDatabase() throws Exception {
        dbController = new DatabaseController();
        dbController.initializePool(
                "jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        executeSchema();
    }

    @AfterAll
    static void tearDownDatabase() {
        if (dbController != null) {
            dbController.close();
        }
    }

    @BeforeEach
    void cleanTables() throws Exception {
        try (Connection conn = dbController.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");
            stmt.execute("TRUNCATE TABLE bids");
            stmt.execute("TRUNCATE TABLE auctions");
            stmt.execute("TRUNCATE TABLE items");
            stmt.execute("TRUNCATE TABLE users");
            stmt.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }

    private static void executeSchema() throws Exception {
        try (InputStream is = DaoTestBase.class.getResourceAsStream("/a88/jbay/db/schema-h2.sql")) {
            if (is == null) {
                throw new IllegalStateException("schema-h2.sql not found on classpath");
            }
            String sql = new BufferedReader(new InputStreamReader(is))
                    .lines()
                    .collect(Collectors.joining("\n"));

            try (Connection conn = dbController.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        }
    }

    protected int insertUser(String username, String password, String role, byte[] qr) throws Exception {
        return executeInsert(
                "INSERT INTO users (username, password, role, qr) VALUES (?, ?, ?, ?)",
                username, password, role, qr
        );
    }

    protected int insertItem(String name, String type, String desc, double price, byte[] image) throws Exception {
        return executeInsert(
                "INSERT INTO items (name, type, desc, start_price, image) VALUES (?, ?, ?, ?, ?)",
                name, type, desc, price, image
        );
    }

    protected int insertAuction(int itemId, int sellerId, double startPrice,
                                double minIncrement, LocalDateTime start,
                                LocalDateTime end, String state) throws Exception {
        return executeInsert(
                "INSERT INTO auctions (item, seller, start_price, min_increment, start_time, end_time, state) VALUES (?, ?, ?, ?, ?, ?, ?)",
                itemId, sellerId, startPrice, minIncrement, start, end, state
        );
    }

    protected int insertBid(int userId, int auctionId, double amount, LocalDateTime time) throws Exception {
        return executeInsert(
                "INSERT INTO bids (userid, auctionid, amt, time) VALUES (?, ?, ?, ?)",
                userId, auctionId, amount, time
        );
    }

    private int executeInsert(String sql, Object... params) throws Exception {
        try (Connection conn = dbController.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new Exception("No generated key");
            }
        }
    }
}
