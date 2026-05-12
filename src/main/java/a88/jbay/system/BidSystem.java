package a88.jbay.system;

import a88.jbay.dao.AuctionDAO;
import a88.jbay.dao.BidDAO;
import a88.jbay.dao.UserDAO;
import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.common.auction.BidTransaction;

import java.time.LocalDateTime;
import java.util.List;

public class BidSystem {
    private final AuctionDAO auctionDAO;
    private final BidDAO bidDAO;
    private final UserDAO userDAO;

    // Constructor for dependency injection
    public BidSystem(AuctionDAO auctionDAO, BidDAO bidDAO, UserDAO userDAO) {
        this.auctionDAO = auctionDAO;
        this.bidDAO = bidDAO;
        this.userDAO = userDAO;
    }

    // Deprecated singleton method - use dependency injection instead
    @Deprecated
    public static synchronized BidSystem getInstance() {
        return new BidSystem(AuctionDAO.getInstance(), BidDAO.getInstance(), UserDAO.getInstance());
    }

    public synchronized boolean placeBid(int userId, int auctionId, double amount) {
        Auction auction = getAuctionById(auctionId);
        if (auction == null) {
            return false;
        }

        if (amount <= auction.getCurrentPrice()) {
            return false;
        }

        if (auction.getAuctionState() != AuctionState.RUNNING) {
            return false;
        }

        BidTransaction tx = new BidTransaction(userId, userDAO.findByUserId(userId).username(), amount, LocalDateTime.now());
        auction.subscribe(userId); // bidder is automatically subscribed
        auction.updatePrice(amount, tx);

        boolean bidInserted = bidDAO.insertBid(userId, auctionId, amount, tx.getTimestamp());
        boolean priceUpdated = auctionDAO.updateCurrentPrice(auctionId, amount, userId);

        return bidInserted && priceUpdated;
    }

    public List<BidDAO.BidData> getBidHistory(int auctionId) {
        return bidDAO.findBidHistoryByAuctionId(auctionId);
    }

    public Double getCurrentPrice(int auctionId) {
        return bidDAO.findCurrentPrice(auctionId);
    }

    private Auction getAuctionById(int auctionId) {
        return AuctionSystem.getInstance().getActiveAuctionById(auctionId);
    }
}
