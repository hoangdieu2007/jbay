package a88.jbay.dao;

import a88.jbay.server.DatabaseController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {
    private final DatabaseController dbController;

    public record BidData(
        int userId,
        int auctionId,
        double amount,
        LocalDateTime time
    ) {}

    // Constructor for dependency injection
    public BidDAO(DatabaseController dbController) {
        this.dbController = dbController;
    }

    // Deprecated singleton method - use dependency injection instead
    @Deprecated
    public static synchronized BidDAO getInstance() {
        return new BidDAO(DatabaseController.getInstance());
    }

    public boolean insertBid(int userId, int auctionId, double amount, LocalDateTime time) {
        String sql = "INSERT INTO bids (userid, auctionid, amt, time) VALUES (?, ?, ?, ?)";

        try (Connection connection = dbController.getConnection();
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

    public List<BidData> findBidHistoryByAuctionId(int auctionId) {
        String sql = "SELECT userid, auctionid, amt, time FROM bids WHERE auctionid = ? ORDER BY time ASC";
        List<BidData> bidHistory = new ArrayList<>();

        try (Connection connection = dbController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, auctionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    BidData bid = new BidData(
                        rs.getInt("userid"),
                        rs.getInt("auctionid"),
                        rs.getDouble("amt"),
                        rs.getTimestamp("time").toLocalDateTime()
                    );
                    bidHistory.add(bid);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bidHistory;
    }

    public Double findCurrentPrice(int auctionId) {
        String sql = "SELECT cur_price FROM auctions WHERE id = ?";

        try (Connection connection = dbController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, auctionId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("cur_price");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
