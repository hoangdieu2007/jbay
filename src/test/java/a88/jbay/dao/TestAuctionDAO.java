package a88.jbay.dao;

import a88.jbay.model.entity.item.Item;
import a88.jbay.testutil.TestDatabaseController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test version of AuctionDAO that uses H2 test database
 */
public class TestAuctionDAO {
    
    private static TestAuctionDAO instance;
    
    public record AuctionData(
        int id,
        Item item,
        int seller,
        double startPrice,
        double curPrice,
        Integer winner,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String state
    ) {
        public int sellerId() { return seller; }
        public Integer winnerId() { return winner; }
    }
    
    public TestAuctionDAO() {}
    
    public static synchronized TestAuctionDAO getInstance() {
        if (instance == null) {
            instance = new TestAuctionDAO();
        }
        return instance;
    }
    
    public int insertAuction(Item item, int seller, double startPrice, double curPrice, 
                            LocalDateTime startTime, LocalDateTime endTime, String state) {
        String sql = "INSERT INTO auctions (item, seller, start_price, cur_price, start_time, end_time, state) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = TestDatabaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, item.getId());
            stmt.setInt(2, seller);
            stmt.setDouble(3, startPrice);
            stmt.setDouble(4, curPrice);
            stmt.setObject(5, startTime);
            stmt.setObject(6, endTime);
            stmt.setString(7, state);

            int result = stmt.executeUpdate();
            
            if (result > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            return -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }
    
    public AuctionData findAuctionById(int auctionId) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        try (Connection connection = TestDatabaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, auctionId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Item item = findItemById(rs.getInt("item"));
                    if (item != null) {
                        Integer winnerValue = rs.getObject("winner") != null ? rs.getInt("winner") : null;
                        return new AuctionData(
                            rs.getInt("id"),
                            item,
                            rs.getInt("seller"),
                            rs.getDouble("start_price"),
                            rs.getDouble("cur_price"),
                            winnerValue,
                            rs.getTimestamp("start_time").toLocalDateTime(),
                            rs.getTimestamp("end_time").toLocalDateTime(),
                            rs.getString("state")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public List<AuctionData> findAuctionsBySellerId(int sellerId) {
        String sql = "SELECT * FROM auctions WHERE seller = ?";
        List<AuctionData> sellerAuctions = new ArrayList<>();

        try (Connection connection = TestDatabaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, sellerId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Item item = findItemById(rs.getInt("item"));
                    if (item != null) {
                        Integer winnerValue = rs.getObject("winner") != null ? rs.getInt("winner") : null;
                        AuctionData auctionData = new AuctionData(
                            rs.getInt("id"),
                            item,
                            rs.getInt("seller"),
                            rs.getDouble("start_price"),
                            rs.getDouble("cur_price"),
                            winnerValue,
                            rs.getTimestamp("start_time").toLocalDateTime(),
                            rs.getTimestamp("end_time").toLocalDateTime(),
                            rs.getString("state")
                        );
                        sellerAuctions.add(auctionData);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sellerAuctions;
    }
    
    public List<AuctionData> findAuctionsByWinnerId(int winnerId) {
        String sql = "SELECT * FROM auctions WHERE winner = ?";
        List<AuctionData> winnerAuctions = new ArrayList<>();

        try (Connection connection = TestDatabaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, winnerId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Item item = findItemById(rs.getInt("item"));
                    if (item != null) {
                        Integer winnerValue = rs.getObject("winner") != null ? rs.getInt("winner") : null;
                        AuctionData auctionData = new AuctionData(
                            rs.getInt("id"),
                            item,
                            rs.getInt("seller"),
                            rs.getDouble("start_price"),
                            rs.getDouble("cur_price"),
                            winnerValue,
                            rs.getTimestamp("start_time").toLocalDateTime(),
                            rs.getTimestamp("end_time").toLocalDateTime(),
                            rs.getString("state")
                        );
                        winnerAuctions.add(auctionData);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return winnerAuctions;
    }
    
    public List<AuctionData> findAllActiveAuctions() {
        String sql = "SELECT * FROM auctions WHERE state IN ('OPENING', 'RUNNING')";
        List<AuctionData> activeAuctions = new ArrayList<>();

        try (Connection connection = TestDatabaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Item item = findItemById(rs.getInt("item"));
                    if (item != null) {
                        Integer winnerValue = rs.getObject("winner") != null ? rs.getInt("winner") : null;
                        AuctionData auctionData = new AuctionData(
                            rs.getInt("id"),
                            item,
                            rs.getInt("seller"),
                            rs.getDouble("start_price"),
                            rs.getDouble("cur_price"),
                            winnerValue,
                            rs.getTimestamp("start_time").toLocalDateTime(),
                            rs.getTimestamp("end_time").toLocalDateTime(),
                            rs.getString("state")
                        );
                        activeAuctions.add(auctionData);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return activeAuctions;
    }
    
    public boolean setAuctionState(int auctionId, String state) {
        String sql = "UPDATE auctions SET state = ? WHERE id = ?";
        try (Connection connection = TestDatabaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, state);
            stmt.setInt(2, auctionId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean updateCurrentPrice(int auctionId, double newPrice, Integer winnerId) {
        String sql = "UPDATE auctions SET cur_price = ?, winner = ? WHERE id = ?";
        try (Connection connection = TestDatabaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setDouble(1, newPrice);
            if (winnerId != null) {
                stmt.setInt(2, winnerId);
            } else {
                stmt.setNull(2, java.sql.Types.INTEGER);
            }
            stmt.setInt(3, auctionId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean updateEndTime(int auctionId, LocalDateTime newEndTime) {
        String sql = "UPDATE auctions SET end_time = ? WHERE id = ?";
        try (Connection connection = TestDatabaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setObject(1, newEndTime);
            stmt.setInt(2, auctionId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean finalizeAuction(int auctionId, Integer winnerId) {
        String sql = "UPDATE auctions SET state = 'FINISHED', winner = ? WHERE id = ?";
        try (Connection connection = TestDatabaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            if (winnerId != null) {
                stmt.setInt(1, winnerId);
            } else {
                stmt.setNull(1, java.sql.Types.INTEGER);
            }
            stmt.setInt(2, auctionId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public Integer findSellerId(int auctionId) {
        String sql = "SELECT seller FROM auctions WHERE id = ?";
        try (Connection connection = TestDatabaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, auctionId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("seller");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private Item findItemById(int itemId) {
        String sql = "SELECT id, name, type, desc, start_price, image FROM items WHERE id = ?";
        try (Connection connection = TestDatabaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, itemId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Item(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getString("desc"),
                        rs.getDouble("start_price"),
                        rs.getBytes("image")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
