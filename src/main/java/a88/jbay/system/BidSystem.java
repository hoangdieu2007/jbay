package a88.jbay.system;

import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.common.auction.BidTransaction;
import a88.jbay.dao.BidDAO;
import a88.jbay.di.ApplicationContext;
import a88.jbay.repository.AuctionRepository;

import java.time.LocalDateTime;
import java.util.List;

public class BidSystem {
    private final AuctionRepository auctionRepository;

    // Constructor for dependency injection
    public BidSystem(AuctionRepository auctionRepository) {
        this.auctionRepository = auctionRepository;
    }

    // Singleton accessor via ApplicationContext
    public static BidSystem getInstance() {
        return ApplicationContext.getInstance().getDependency(BidSystem.class);
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

        String username = auctionRepository.getUsernameByUserId(userId);
        BidTransaction tx = new BidTransaction(userId, username, amount, LocalDateTime.now());
        auction.subscribe(userId); // bidder is automatically subscribed
        auction.updatePrice(amount, tx);

        boolean bidInserted = auctionRepository.insertBid(userId, auctionId, amount, tx.getTimestamp());
        boolean priceUpdated = auctionRepository.updateCurrentPrice(auctionId, amount, userId);

        return bidInserted && priceUpdated;
    }

    public List<BidDAO.BidData> getBidHistory(int auctionId) {
        return auctionRepository.findBidHistoryByAuctionId(auctionId);
    }

    public Double getCurrentPrice(int auctionId) {
        return auctionRepository.findCurrentPrice(auctionId);
    }

    private Auction getAuctionById(int auctionId) {
        return auctionRepository.getActiveAuctionById(auctionId);
    }
}
