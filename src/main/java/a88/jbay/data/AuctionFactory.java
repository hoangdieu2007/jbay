package a88.jbay.data;

import a88.jbay.common.auction.*;
import a88.jbay.common.item.Item;
import a88.jbay.dao.BidDAO;
import a88.jbay.dao.ItemDAO;
import a88.jbay.dao.UserDAO;
import a88.jbay.util.JBayLogger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AuctionFactory {

    private final ItemDAO itemDAO;
    private final UserDAO userDAO;
    private final BidDAO bidDAO;
    private final JBayLogger logger;

    public AuctionFactory(ItemDAO itemDAO, UserDAO userDAO, BidDAO bidDAO) {
        this.itemDAO = itemDAO;
        this.userDAO = userDAO;
        this.bidDAO = bidDAO;
        this.logger = JBayLogger.getLogger(AuctionFactory.class);
    }

    /**
     * Reconstructs a full Auction object from persistent data, including
     * bid history, bidder subscriptions, and seller subscription.
     */
    public Auction reconstruct(AuctionData data) {
        Item item = itemDAO.findItemById(data.itemId());
        if (item == null) {
            logger.error("Item not found for auction " + data.id());
            return null;
        }

        String sellerName = userDAO.findByUserId(data.sellerId()).username();

        logger.debug("Loading auction: " + data.id() +
                " - " + item.getName() +
                " - State: " + data.state());

        Auction auction = new Auction(
                data.id(),
                item,
                sellerName,
                data.startTime(),
                data.endTime()
        );

        auction.setAuctionState(AuctionState.valueOf(data.state()));

        restoreBidHistory(auction, data.id());

        auction.subscribe(data.sellerId());

        return auction;
    }

    /**
     * Reconstructs a lightweight Auction for admin display.
     * Does not restore bid history or subscriptions.
     */
    public Auction reconstructForAdmin(AuctionData data) {
        Item item = new Item(
                data.itemId(),
                data.itemName(),
                "UNKNOWN",
                "",
                data.startPrice()
        );

        Auction auction = new Auction(
                data.id(),
                item,
                String.valueOf(data.sellerId()),
                data.startTime(),
                data.endTime()
        );

        try {
            auction.setAuctionState(AuctionState.valueOf(data.state()));
        } catch (Exception e) {
            auction.setAuctionState(AuctionState.OPENING);
        }

        return auction;
    }

    private void restoreBidHistory(Auction auction, int auctionId) {
        List<BidData> bidHistory = bidDAO.findBidHistoryByAuctionId(auctionId);
        Set<Integer> bidders = new HashSet<>();

        for (BidData bidData : bidHistory) {
            String bidderName = userDAO.findByUserId(bidData.userId()).username();

            BidTransaction tx = new BidTransaction(
                    bidData.userId(),
                    bidderName,
                    bidData.amount(),
                    bidData.time()
            );

            /*
             * IMPORTANT:
             * Use reconstruction-safe method.
             * DO NOT call live business workflow methods here.
             */
            auction.addBid(bidData.amount(), tx);
            bidders.add(bidData.userId());
        }

        for (int bidderId : bidders) {
            auction.subscribe(bidderId);
        }
    }
}