package a88.jbay.dao;

import a88.jbay.common.auction.BidData;
import a88.jbay.server.DatabaseController;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    /**
     * Find the bid history of a specific auction
     * Logic: For each price tag, only the earliest unbanned bid is returned.
     * @param auctionId
     * @return List of BidData objects
     */

    private List<BidData> findAllValidBidsByAuctionId(int auctionId) {
        return executeQueryList(
                "SELECT b.userid, b.auctionid, b.amt, b.time " +
                        "FROM bids b JOIN users u ON u.id = b.userid " +
                        "WHERE b.auctionid = ? AND u.role != 'BAN' " +
                        "ORDER BY b.id ASC",
                rs -> new BidData(
                        rs.getInt("userid"),
                        rs.getInt("auctionid"),
                        rs.getDouble("amt"),
                        rs.getTimestamp("time").toLocalDateTime()
                ),
                auctionId
        );
    }

    private List<BidData> stalinFilter(List<BidData> bids) {
        List<BidData> result = new ArrayList<>();
        double runningMax = Double.MIN_VALUE;
        for (BidData bid : bids) {
            if (bid.amount() > runningMax) {
                runningMax = bid.amount();
                result.add(bid);
            }
        }
        return result;
    }

    public List<BidData> findBidHistoryByAuctionId(int auctionId) {
        return stalinFilter(findAllValidBidsByAuctionId(auctionId));
    }
}
