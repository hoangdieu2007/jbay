package a88.jbay.data;

import a88.jbay.common.auction.BidTransaction;
import a88.jbay.dao.AuctionDAO;
import a88.jbay.dao.BidDAO;
import a88.jbay.server.DatabaseController;
import a88.jbay.util.JBayLogger;

import java.sql.Connection;
import java.sql.SQLException;

public class BidRepository {

    private final AuctionDAO auctionDAO;
    private final BidDAO bidDAO;
    private final DatabaseController dbController;
    private final JBayLogger logger;

    public BidRepository(DatabaseController dbController, AuctionDAO auctionDAO, BidDAO bidDAO) {
        this.dbController = dbController;
        this.auctionDAO = auctionDAO;
        this.bidDAO = bidDAO;
        this.logger = JBayLogger.getLogger(BidRepository.class);
    }

    public boolean saveBid(int auctionId, BidTransaction tx) {
        try (Connection connection = dbController.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int bidId = bidDAO.insertBid(connection, tx.getUserID(), auctionId, tx.getAmt(), tx.getTimestamp());
                auctionDAO.updateCurrentBid(connection, auctionId, bidId);

                connection.commit();
                return true;

            } catch (Exception e) {
                connection.rollback();
                logger.error("Transaction failed saving bid for auction: " + auctionId);
                e.printStackTrace();
                return false;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}