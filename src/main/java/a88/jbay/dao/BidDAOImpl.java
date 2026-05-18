package a88.jbay.dao;

import a88.jbay.common.auction.BidData;
import a88.jbay.server.DatabaseController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BidDAOImpl extends BaseDAO implements BidDAO {

    public BidDAOImpl(DatabaseController dbController) {
        super(dbController);
    }

    public boolean insertBid(int userId, int auctionId, double amount, LocalDateTime time) {
        return executeUpdate(
                "INSERT INTO bids (userid, auctionid, amt, time) VALUES (?, ?, ?, ?)",
                userId, auctionId, amount, time
        ) > 0;
    }

    public List<BidData> findBidHistoryByAuctionId(int auctionId) {
        return executeQueryList(
                "SELECT userid, auctionid, amt, time FROM bids WHERE auctionid = ? ORDER BY time ASC",
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