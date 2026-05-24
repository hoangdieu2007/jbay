package a88.jbay.dao;

import a88.jbay.common.auction.AuctionData;
import a88.jbay.common.auction.AuctionState;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public interface AuctionDAO {

    int insertAuction(
            int itemId,
            int sellerId,
            double startPrice,
            double minIncrement,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    boolean updateCurrentBid(
            int auctionId,
            int bidId
    );

    boolean updateEndTime(
            int auctionId,
            LocalDateTime newEndTime
    );

    boolean setAuctionState(
            int auctionId,
            AuctionState newState
    );

    AuctionData findAuctionById(int auctionId);

    List<AuctionData> findAuctionsBySellerId(
            int sellerId
    );

    List<AuctionData> findAuctionsByWinnerId(
            int winnerId
    );

    List<AuctionData> findAllActiveAuctions();

    Double findCurrentPrice(int auctionId);

    List<AuctionData> getAllAuctionsForAdmin();

    // --- transactional overloads ---
    int insertAuction(Connection connection, int itemId, int sellerId,
                      double startPrice, double minIncrement,
                      LocalDateTime startTime, LocalDateTime endTime) throws SQLException;

    boolean updateCurrentBid(Connection connection, int auctionId,
                             int bidId) throws SQLException;
}
