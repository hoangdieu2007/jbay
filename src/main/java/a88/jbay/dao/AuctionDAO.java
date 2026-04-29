package a88.jbay.dao;

import a88.jbay.controller.server.DatabaseController;
import a88.jbay.model.entity.item.Item;
import a88.jbay.model.event.AuctionState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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

        String sql = "INSERT INTO items (name, type, `desc`, start_price, image) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseController.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, item.getName());
            stmt.setString(2, item.getType());
            stmt.setString(3, item.getDescription());
            stmt.setDouble(4, item.getInitPrice());
            stmt.setBytes(5, item.getImage());

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

        String sql = """
                INSERT INTO auctions (item, seller, start_price, cur_price, winner, start_time, end_time, state)
                VALUES (?, ?, ?, ?, NULL, ?, ?, 'OPENING')
                """;

        try (Connection connection = DatabaseController.getInstance().getConnection();
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

    public boolean updateCurrentPrice(int auctionId, double newPrice, int winnerId) {

        String sql = "UPDATE auctions SET cur_price = ?, winner = ? WHERE id = ?";

        try (Connection connection = DatabaseController.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setDouble(1, newPrice);
            stmt.setInt(2, winnerId);
            stmt.setInt(3, auctionId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean finalizeAuction(int auctionId, Integer winnerId) {

        String sql = "UPDATE auctions SET winner = ?, state = 'FINISHED' WHERE id = ?";

        try (Connection connection = DatabaseController.getInstance().getConnection();
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

    public boolean setAuctionState(int auctionId, AuctionState newState) {

        String sql = "UPDATE auctions SET state = ? WHERE id = ?";

        try (Connection connection = DatabaseController.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, newState.name());
            stmt.setInt(2, auctionId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertBid(int userId, int auctionId, double amount, LocalDateTime time) {

        String sql = "INSERT INTO bids (userid, auctionid, amt, time) VALUES (?, ?, ?, ?)";

        try (Connection connection = DatabaseController.getInstance().getConnection();
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

        String sql = "SELECT cur_price FROM auctions WHERE id = ?";

        try (Connection connection = DatabaseController.getInstance().getConnection();
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

        String sql = "SELECT seller FROM auctions WHERE id = ?";

        try (Connection connection = DatabaseController.getInstance().getConnection();
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

    public Map<String, String> findItemById(int itemId) {
        String sql = "SELECT id, name, `desc`, start_price FROM items WHERE id = ?";

        try (Connection connection = DatabaseController.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, itemId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                Map<String, String> itemData = new HashMap<>();
                itemData.put("id", String.valueOf(rs.getInt("id")));
                itemData.put("name", rs.getString("name"));
                itemData.put("description", rs.getString("desc"));
                itemData.put("start_price", String.valueOf(rs.getDouble("start_price")));

                return itemData;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}