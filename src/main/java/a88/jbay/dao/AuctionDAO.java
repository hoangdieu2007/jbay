package a88.jbay.dao;

import a88.jbay.controller.server.DatabaseController;
import a88.jbay.model.entity.item.Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class AuctionDAO {
    private static AuctionDAO instance;

    private AuctionDAO() {}

    public static synchronized AuctionDAO getInstance() {
        if (instance == null) {
            instance = new AuctionDAO();
        }
        return instance;
    }

    public int insertItem(Item item) {
        DatabaseController databaseController = new DatabaseController();

        String sql = "INSERT INTO items (name, `desc`, start_price) VALUES (?, ?, ?)";

        try (Connection connection = databaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, item.getName());
            stmt.setString(2, item.getDescription());
            stmt.setDouble(3, item.getInitPrice());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                return -1;
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }

            return -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int insertAuction(int itemId, int sellerId, double startPrice, double curPrice,
                             LocalDateTime startTime, LocalDateTime endTime) {
        DatabaseController databaseController = new DatabaseController();

        String sql = """
                INSERT INTO auctions (item, seller, start_price, cur_price, winner, start_time, end_time)
                VALUES (?, ?, ?, ?, NULL, ?, ?)
                """;

        try (Connection connection = databaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, itemId);
            stmt.setInt(2, sellerId);
            stmt.setDouble(3, startPrice);
            stmt.setDouble(4, curPrice);
            stmt.setObject(5, startTime);
            stmt.setObject(6, endTime);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                return -1;
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }

            return -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public boolean updateCurrentPrice(int auctionId, double newPrice) {
        DatabaseController databaseController = new DatabaseController();

        String sql = "UPDATE auctions SET cur_price = ? WHERE id = ?";

        try (Connection connection = databaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setDouble(1, newPrice);
            stmt.setInt(2, auctionId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean closeAuction(int auctionId, Integer winnerId) {
        DatabaseController databaseController = new DatabaseController();

        String sql = "UPDATE auctions SET winner = ? WHERE id = ?";

        try (Connection connection = databaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            if (winnerId == null) {
                stmt.setNull(1, java.sql.Types.INTEGER);
            } else {
                stmt.setInt(1, winnerId);
            }
            stmt.setInt(2, auctionId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertBid(int userId, int auctionId, double amount, LocalDateTime time) {
        DatabaseController databaseController = new DatabaseController();

        String sql = "INSERT INTO bids (userid, auctionid, amt, time) VALUES (?, ?, ?, ?)";

        try (Connection connection = databaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, auctionId);
            stmt.setDouble(3, amount);
            stmt.setObject(4, time);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Double findCurrentPrice(int auctionId) {
        DatabaseController databaseController = new DatabaseController();

        String sql = "SELECT cur_price FROM auctions WHERE id = ?";

        try (Connection connection = databaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, auctionId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getDouble("cur_price");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Integer findSellerId(int auctionId) {
        DatabaseController databaseController = new DatabaseController();

        String sql = "SELECT seller FROM auctions WHERE id = ?";

        try (Connection connection = databaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, auctionId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getInt("seller");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}