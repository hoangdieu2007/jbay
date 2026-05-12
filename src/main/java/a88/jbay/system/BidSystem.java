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
    private static BidSystem instance;
    private final AuctionDAO auctionDAO;
    private final BidDAO bidDAO;

    private BidSystem() {
        this.auctionDAO = AuctionDAO.getInstance();
        this.bidDAO = BidDAO.getInstance();
    }

    public static synchronized BidSystem getInstance() {
        if (instance == null) {
            instance = new BidSystem();
        }
        return instance;
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

        BidTransaction tx = new BidTransaction(userId, UserDAO.getInstance().findByUserId(userId).username(), amount, LocalDateTime.now());
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
