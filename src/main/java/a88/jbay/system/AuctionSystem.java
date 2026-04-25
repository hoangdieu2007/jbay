package a88.jbay.model.system;

import a88.jbay.dao.AuctionDAO;
import a88.jbay.model.entity.item.Item;
import a88.jbay.model.event.Auction;
import a88.jbay.model.event.BidTransaction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionSystem {
    private static AuctionSystem instance;
    private final AuctionDAO auctionDAO;

    // Memory cache for active auctions to handle real-time bidding
    private final Map<Integer, Auction> activeAuctions;

    private AuctionSystem() {
        this.auctionDAO = AuctionDAO.getInstance();
        this.activeAuctions = new ConcurrentHashMap<>();
    }

    public static synchronized AuctionSystem getInstance() {
        if (instance == null) {
            instance = new AuctionSystem();
        }
        return instance;
    }

    //create auction and store to database
    public boolean createAuction(Item item, int sellerId, LocalDateTime start, LocalDateTime end) {
        // 1. Insert the item first
        int itemId = auctionDAO.insertItem(item);
        if (itemId == -1) return false;

        // 2. Create the auction record
        int auctionId = auctionDAO.insertAuction(
                itemId,
                sellerId,
                item.getInitPrice(),
                item.getInitPrice(),
                start,
                end
        );

        return auctionId != -1;
    }

    //place bid and validate bid
    public synchronized boolean placeBid(int userId, int auctionId, double amount) {
        Double currentPrice = auctionDAO.findCurrentPrice(auctionId);

        if (currentPrice == null || amount <= currentPrice) {
            return false;
        }

        // Check if the user is the seller (business rule: seller cannot bid on own item)
        Integer sellerId = auctionDAO.findSellerId(auctionId);
        if (sellerId != null && sellerId == userId) {
            return false;
        }

        // Persist the bid transaction
        boolean bidInserted = auctionDAO.insertBid(userId, auctionId, amount, LocalDateTime.now());
        if (bidInserted) {
            // Update the current price in the auction record
            return auctionDAO.updateCurrentPrice(auctionId, amount);
        }

        return false;
    }

    //close auction
    public boolean finalizeAuction(int auctionId, Integer winnerId) {
        boolean closed = auctionDAO.closeAuction(auctionId, winnerId);
        if (closed) {
            activeAuctions.remove(auctionId);
        }
        return closed;
    }

    public List<Auction> getActiveAuctions() {
        return new ArrayList<>(activeAuctions.values());
    }
}