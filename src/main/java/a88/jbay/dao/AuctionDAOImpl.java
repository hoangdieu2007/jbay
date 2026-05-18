package a88.jbay.dao;

import a88.jbay.common.auction.AuctionData;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.server.DatabaseController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAOImpl extends BaseDAO implements AuctionDAO {

    public AuctionDAOImpl(DatabaseController dbController) {
        super(dbController);
    }

    private AuctionData mapAuction(ResultSet rs) throws SQLException {
        Integer winnerId = rs.getObject("winner") == null ? null : rs.getInt("winner");
        return new AuctionData(
                rs.getInt("id"), rs.getInt("item"), rs.getInt("seller"),
                rs.getDouble("start_price"), rs.getDouble("cur_price"), winnerId,
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime(),
                rs.getString("state"), ""
        );
    }

    public int insertAuction(int itemId, int sellerId, double startPrice, double curPrice,
                             LocalDateTime startTime, LocalDateTime endTime) {
        return executeInsert("""
                INSERT INTO auctions (item, seller, start_price, cur_price, winner, start_time, end_time, state)
                VALUES (?, ?, ?, ?, NULL, ?, ?, 'OPENING')
                """,
                itemId, sellerId, startPrice, curPrice, startTime, endTime
        );
    }

    public boolean updateCurrentPrice(int auctionId, double newPrice, int winnerId) {
        return executeUpdate("UPDATE auctions SET cur_price = ?, winner = ? WHERE id = ?",
                newPrice, winnerId, auctionId) > 0;
    }

    public boolean updateEndTime(int auctionId, LocalDateTime newEndTime) {
        return executeUpdate("UPDATE auctions SET end_time = ? WHERE id = ?",
                newEndTime, auctionId) > 0;
    }

    public boolean finalizeAuction(int auctionId, Integer winnerId) {
        return executeUpdate("UPDATE auctions SET winner = ?, state = 'FINISHED' WHERE id = ?",
                winnerId, auctionId) > 0;
    }

    public boolean setAuctionState(int auctionId, AuctionState newState) {
        return executeUpdate("UPDATE auctions SET state = ? WHERE id = ?",
                newState.name(), auctionId) > 0;
    }

    public Integer findSellerId(int auctionId) {
        return executeQuery("SELECT seller FROM auctions WHERE id = ?",
                rs -> rs.next() ? rs.getInt("seller") : null, auctionId);
    }

    public Double findCurrentPrice(int auctionId) {
        return executeQuery("SELECT cur_price FROM auctions WHERE id = ?",
                rs -> rs.next() ? rs.getDouble("cur_price") : null, auctionId);
    }

    public AuctionData findAuctionById(int auctionId) {
        return executeQuery("SELECT * FROM auctions WHERE id = ?",
                rs -> rs.next() ? mapAuction(rs) : null, auctionId);
    }

    public List<AuctionData> findAuctionsBySellerId(int sellerId) {
        return executeQueryList("SELECT * FROM auctions WHERE seller = ?", this::mapAuction, sellerId);
    }

    public List<AuctionData> findAuctionsByWinnerId(int winnerId) {
        return executeQueryList("SELECT * FROM auctions WHERE winner = ?", this::mapAuction, winnerId);
    }

    public List<AuctionData> findAllActiveAuctions() {
        return executeQueryList("SELECT * FROM auctions WHERE state IN ('OPENING', 'RUNNING')", this::mapAuction);
    }

    public List<AuctionData> getAllAuctionsForAdmin() {
        return executeQueryList("""
                SELECT a.id, a.item, a.seller, a.start_price, a.cur_price,
                       a.winner, a.start_time, a.end_time, a.state, i.name AS item_name
                FROM auctions a
                JOIN items i ON a.item = i.id
                ORDER BY a.id DESC
                """,
                rs -> {
                    Integer winnerId = rs.getObject("winner") == null ? null : rs.getInt("winner");
                    return new AuctionData(
                            rs.getInt("id"), rs.getInt("item"), rs.getInt("seller"),
                            rs.getDouble("start_price"), rs.getDouble("cur_price"), winnerId,
                            rs.getTimestamp("start_time").toLocalDateTime(),
                            rs.getTimestamp("end_time").toLocalDateTime(),
                            rs.getString("state"), rs.getString("item_name")
                    );
                }
        );
    }
}