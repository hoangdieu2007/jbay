package a88.jbay.dao;

import a88.jbay.common.auction.BidData;
import a88.jbay.server.DatabaseController;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class BidDAOImpl extends BaseDAO implements BidDAO {

    public BidDAOImpl(DatabaseController dbController) {
        super(dbController);
    }

    public int insertBid(int userId, int auctionId, double amount, LocalDateTime time) {
        return executeInsert(
                "INSERT INTO bids (userid, auctionid, amt, time) VALUES (?, ?, ?, ?)",
                userId, auctionId, amount, time
        );
    }

    @Override
    public int insertBid(Connection connection, int userId, int auctionId,
                         double amount, LocalDateTime time) throws SQLException {
        return executeInsert(connection,
                "INSERT INTO bids (userid, auctionid, amt, time) VALUES (?, ?, ?, ?)",
                userId, auctionId, amount, time
        );
    }

    public List<BidData> findBidHistoryByAuctionId(int auctionId) {
        return executeQueryList(
                "SELECT b.userid, b.auctionid, b.amt, b.time " +
                        "FROM bids b JOIN users u ON u.id = b.userid " +
                        "WHERE b.auctionid = ? AND u.role != 'BAN' " +
                        "ORDER BY b.time ASC",
                rs -> new BidData(
                        rs.getInt("userid"),
                        rs.getInt("auctionid"),
                        rs.getDouble("amt"),
                        rs.getTimestamp("time").toLocalDateTime()
                ),
                auctionId
        );
    }
}
