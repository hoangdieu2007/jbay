package a88.jbay.dao;

import a88.jbay.model.entity.item.Item;
import a88.jbay.model.event.AuctionState;
import a88.jbay.server.DatabaseConnectionProvider;
import a88.jbay.server.DatabaseController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class AuctionDAO {
    private static final AuctionDAO instance = new AuctionDAO();
    private final DatabaseConnectionProvider dbProvider;
    private final ItemDAO itemDAO;
    private final BidDAO bidDAO;

    public record AuctionData(
        int id,
        Item item,
        int sellerId,
        double startPrice,
        double curPrice,
        Integer winnerId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String state
    ) {}

    private AuctionDAO() {
        this.dbProvider = DatabaseController.getInstance();
        this.itemDAO = ItemDAO.getInstance();
        this.bidDAO = BidDAO.getInstance();
    }

    // dependency injection
    public AuctionDAO(DatabaseConnectionProvider dbProvider, ItemDAO itemDAO, BidDAO bidDAO) {
        this.dbProvider = dbProvider;
        this.itemDAO = itemDAO;
        this.bidDAO = bidDAO;
    }

    public static AuctionDAO getInstance() {
        return instance;
    }

    private AuctionData mapAuction(ResultSet rs) throws SQLException {
        Item item = itemDAO.findItemById(rs.getInt("item"));
        if (item == null) return null;

        return new AuctionData(
                rs.getInt("id"),
                item,
                rs.getInt("seller"),
                rs.getDouble("start_price"),
                rs.getDouble("cur_price"),
                rs.getInt("winner"),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime(),
                rs.getString("state")
        );
    }

    public int insertItem(Item item) {
        return itemDAO.insertItem(item);
    }

    public int insertAuction(int itemId, int sellerId, double startPrice, double curPrice,
                             LocalDateTime startTime, LocalDateTime endTime) {

        String sql = """
                INSERT INTO auctions (item, seller, start_price, cur_price, winner, start_time, end_time, state)
                VALUES (?, ?, ?, ?, NULL, ?, ?, 'OPENING')
                """;

        try (Connection connection = dbProvider.getConnection();
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

        try (Connection connection = dbProvider.getConnection();
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

    public boolean updateEndTime(int auctionId, LocalDateTime newEndTime) {
        String sql = "UPDATE auctions SET end_time = ? WHERE id = ?";

        try (Connection connection = dbProvider.getConnection();
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

        String sql = "UPDATE auctions SET winner = ?, state = 'FINISHED' WHERE id = ?";

        try (Connection connection = dbProvider.getConnection();
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

        try (Connection connection = dbProvider.getConnection();
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
        return bidDAO.insertBid(userId, auctionId, amount, time);
    }

    public Double findCurrentPrice(int auctionId) {
        return bidDAO.findCurrentPrice(auctionId);
    }

    public Integer findSellerId(int auctionId) {

        String sql = "SELECT seller FROM auctions WHERE id = ?";

        try (Connection connection = dbProvider.getConnection();
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

    public AuctionData findAuctionById(int auctionId) {
        String sql = "SELECT * FROM auctions WHERE id = ?";

        try (Connection connection = dbProvider.getConnection();
            PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                return mapAuction(rs);
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public java.util.List<BidDAO.BidData> findBidHistoryByAuctionId(int auctionId) {
        return bidDAO.findBidHistoryByAuctionId(auctionId);
    }

    public java.util.List<AuctionData> findAuctionsBySellerId(int sellerId) {
        String sql = "SELECT * FROM auctions WHERE seller = ?";
        java.util.List<AuctionData> sellerAuctions = new java.util.ArrayList<>();

        try (Connection connection = dbProvider.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, sellerId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                        AuctionData auctionData = mapAuction(rs);
                        sellerAuctions.add(auctionData);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sellerAuctions;
    }

    public java.util.List<AuctionData> findAuctionsByWinnerId(int winnerId) {
        String sql = "SELECT * FROM auctions WHERE winner = ?";
        java.util.List<AuctionData> winnerAuctions = new java.util.ArrayList<>();

        try (Connection connection = dbProvider.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, winnerId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                        AuctionData auctionData = mapAuction(rs);
                        winnerAuctions.add(auctionData);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return winnerAuctions;
    }

    public java.util.List<AuctionData> findAllActiveAuctions() {
        String sql = "SELECT * FROM auctions WHERE state IN ('OPENING', 'RUNNING')";
        java.util.List<AuctionData> activeAuctions = new java.util.ArrayList<>();

        try (Connection connection = dbProvider.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AuctionData auctionData = mapAuction(rs);
                    activeAuctions.add(auctionData);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return activeAuctions;
    }


}