package a88.jbay.system;

import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.common.auction.BidTransaction;
import a88.jbay.dao.AuctionDAO;
import a88.jbay.dao.BidDAO;
import a88.jbay.di.ApplicationContext;
import a88.jbay.repository.AuctionRepository;

import java.time.LocalDateTime;
import java.util.List;

public class BidSystem {

    private final AuctionRepository auctionRepository;
    private final BidDAO bidDAO;
    private final AuctionDAO auctionDAO;

    // Constructor for dependency injection
    public BidSystem(
            AuctionRepository auctionRepository,
            BidDAO bidDAO,
            AuctionDAO auctionDAO
    ) {

        this.auctionRepository = auctionRepository;
        this.bidDAO = bidDAO;
        this.auctionDAO = auctionDAO;
    }

    // Singleton accessor via ApplicationContext
    public static BidSystem getInstance() {

        return ApplicationContext
                .getInstance()
                .getDependency(BidSystem.class);
    }

    public synchronized boolean placeBid(
            int userId,
            int auctionId,
            double amount
    ) {

        Auction auction = auctionRepository.getActiveAuctionById(auctionId);

        if (!isValidBid(auction, amount)) {
            return false;
        }

        BidTransaction tx = createBidTransaction(
                        userId,
                        amount
                );

        addBid(
                auction,
                tx
        );

        return saveBid(
                auctionId,
                tx
        );
    }

    public List<BidDAO.BidData> getBidHistory(int auctionId) {
        return bidDAO.findBidHistoryByAuctionId(auctionId);
    }

    public Double getCurrentPrice(int auctionId) {
        return auctionDAO.findCurrentPrice(auctionId);
    }

    private boolean isValidBid(
            Auction auction,
            double amount
    ) {

        if (auction == null) {
            return false;
        }

        if (auction.getAuctionState() != AuctionState.RUNNING) {
            return false;
        }

        return amount > auction.getCurrentPrice();
    }

    private BidTransaction createBidTransaction(
            int userId,
            double amount
    ) {

        String username =
                auctionRepository.getUsernameByUserId(userId);

        return new BidTransaction(
                userId,
                username,
                amount,
                LocalDateTime.now()
        );
    }

    private void addBid(
            Auction auction,
            BidTransaction tx
    ) {

        // bidder automatically becomes observer

        auction.subscribe(
                tx.getUserID()
        );

        auction.updatePrice(
                tx.getAmt(),
                tx
        );
    }

    private boolean saveBid(
            int auctionId,
            BidTransaction tx
    ) {

        boolean priceUpdated = auctionDAO.updateCurrentPrice(
                        auctionId,
                        tx.getAmt(),
                        tx.getUserID()
                );

        if (!priceUpdated) {
            return false;
        }

        return bidDAO.insertBid(
                tx.getUserID(),
                auctionId,
                tx.getAmt(),
                tx.getTimestamp()
        );
    }
}