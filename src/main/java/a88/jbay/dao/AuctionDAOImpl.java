package a88.jbay.dao;

import a88.jbay.common.auction.AuctionData;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.server.DatabaseController;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class AuctionDAOImpl extends BaseDAO implements AuctionDAO {

    private static final String AUCTION_SELECT =
            "SELECT a.id, a.item, a.seller, a.start_price, a.min_increment, " +
            "       COALESCE(b.amt, a.start_price) AS cur_price, " +
            "       b.userid AS winner_id, " +
            "       a.start_time, a.end_time, a.state " +
            "FROM auctions a " +
            "LEFT JOIN bids b ON b.id = a.cur_bid ";

    public AuctionDAOImpl(DatabaseController dbController) {
        super(dbController);
    }

    private AuctionData mapAuction(ResultSet rs) throws SQLException {
        Integer winnerId = rs.getObject("winner_id") == null ? null : rs.getInt("winner_id");
        return new AuctionData(
                rs.getInt("id"), rs.getInt("item"), rs.getInt("seller"),
                rs.getDouble("start_price"), rs.getDouble("cur_price"),
                rs.getDouble("min_increment"), winnerId,
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime(),
                rs.getString("state"), ""
        );
    }

    public int insertAuction(int itemId, int sellerId, double startPrice,
                             double minIncrement, LocalDateTime startTime, LocalDateTime endTime) {
        return executeInsert("""
                INSERT INTO auctions (item, seller, start_price, min_increment, cur_bid, start_time, end_time, state)
                VALUES (?, ?, ?, ?, NULL, ?, ?, 'OPENING')
                """,
                itemId, sellerId, startPrice, minIncrement, startTime, endTime
        );
    }

    public boolean updateCurrentBid(int auctionId, int bidId) {
        return executeUpdate("UPDATE auctions SET cur_bid = ? WHERE id = ?",
                bidId, auctionId) > 0;
    }

    @Override
    public int insertAuction(Connection connection, int itemId, int sellerId,
                             double startPrice, double minIncrement,
                             LocalDateTime startTime, LocalDateTime endTime) throws SQLException {
        return executeInsert(connection, """
            INSERT INTO auctions (item, seller, start_price, min_increment, cur_bid, start_time, end_time, state)
            VALUES (?, ?, ?, ?, NULL, ?, ?, 'OPENING')
            """,
                itemId, sellerId, startPrice, minIncrement, startTime, endTime
        );
    }

    @Override
    public boolean updateCurrentBid(Connection connection, int auctionId, int bidId) throws SQLException {
        return executeUpdate(connection,
                "UPDATE auctions SET cur_bid = ? WHERE id = ?",
                bidId, auctionId
        ) > 0;
    }

    public boolean updateEndTime(int auctionId, LocalDateTime newEndTime) {
        return executeUpdate("UPDATE auctions SET end_time = ? WHERE id = ?",
                newEndTime, auctionId) > 0;
    }

    public boolean setAuctionState(int auctionId, AuctionState newState) {
        return executeUpdate("UPDATE auctions SET state = ? WHERE id = ?",
                newState.name(), auctionId) > 0;
    }

    public Double findCurrentPrice(int auctionId) {
        return executeQuery(
                "SELECT COALESCE(b.amt, a.start_price) AS cur_price " +
                "FROM auctions a LEFT JOIN bids b ON b.id = a.cur_bid " +
                "WHERE a.id = ?",
                rs -> rs.next() ? rs.getDouble("cur_price") : null, auctionId);
    }

    public AuctionData findAuctionById(int auctionId) {
        return executeQuery(AUCTION_SELECT + "WHERE a.id = ?",
                rs -> rs.next() ? mapAuction(rs) : null, auctionId);
    }

    public List<AuctionData> findAuctionsBySellerId(int sellerId) {
        return executeQueryList(AUCTION_SELECT + "WHERE a.seller = ?", this::mapAuction, sellerId);
    }

    public List<AuctionData> findAuctionsByWinnerId(int winnerId) {
        return executeQueryList(
                AUCTION_SELECT + "WHERE b.userid = ? AND a.state IN ('FINISHED', 'PAID')",
                this::mapAuction, winnerId
        );
    }

    public List<AuctionData> findAllActiveAuctions() {
        return executeQueryList(
                AUCTION_SELECT + "WHERE a.state IN ('OPENING', 'RUNNING')",
                this::mapAuction
        );
    }

    public List<AuctionData> getAllAuctionsForAdmin() {
        return executeQueryList("""
                SELECT a.id, a.item, a.seller, a.start_price, a.min_increment,
                       COALESCE(b.amt, a.start_price) AS cur_price,
                       b.userid AS winner_id,
                       a.start_time, a.end_time, a.state,
                       i.name AS item_name
                FROM auctions a
                LEFT JOIN bids b ON b.id = a.cur_bid
                JOIN items i ON a.item = i.id
                ORDER BY a.id DESC
                """,
                rs -> {
                    Integer winnerId = rs.getObject("winner_id") == null ? null : rs.getInt("winner_id");
                    return new AuctionData(
                            rs.getInt("id"), rs.getInt("item"), rs.getInt("seller"),
                            rs.getDouble("start_price"), rs.getDouble("cur_price"),
                            rs.getDouble("min_increment"), winnerId,
                            rs.getTimestamp("start_time").toLocalDateTime(),
                            rs.getTimestamp("end_time").toLocalDateTime(),
                            rs.getString("state"), rs.getString("item_name")
                    );
                }
        );
    }
}
