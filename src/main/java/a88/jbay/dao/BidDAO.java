package a88.jbay.dao;

import a88.jbay.common.auction.BidData;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public interface BidDAO {

    boolean insertBid(
            int userId,
            int auctionId,
            double amount,
            LocalDateTime time
    );

    List<BidData> findBidHistoryByAuctionId(
            int auctionId
    );

    // --- transactional overload ---
    boolean insertBid(Connection connection, int userId, int auctionId,
                      double amount, LocalDateTime time) throws SQLException;
}