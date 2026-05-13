package a88.jbay.repository;

import a88.jbay.common.auction.Auction;
import a88.jbay.common.item.Item;
import a88.jbay.dao.AuctionDAO;
import a88.jbay.dao.AuctionDAO.AuctionData;
import a88.jbay.dao.BidDAO;
import a88.jbay.dao.UserDAO;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.common.auction.BidTransaction;
import a88.jbay.util.JBayLogger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository for managing active auctions in memory.
 * This class separates data storage concerns from business logic.
 */
public class AuctionRepository {
    private final Map<Integer, Auction> activeAuctions;
    private final AuctionDAO auctionDAO;
    private final UserDAO userDAO;
    private final BidDAO bidDAO;
    private final JBayLogger logger;

    public AuctionRepository(AuctionDAO auctionDAO, UserDAO userDAO, BidDAO bidDAO) {
        this.activeAuctions = new ConcurrentHashMap<>();
        this.auctionDAO = auctionDAO;
        this.userDAO = userDAO;
        this.bidDAO = bidDAO;
        this.logger = JBayLogger.getLogger(AuctionRepository.class);
    }

    /**
     * Store an active auction in memory.
     */
    public void storeActiveAuction(Auction auction) {
        activeAuctions.put(auction.getId(), auction);
    }

    /**
     * Get an active auction by ID.
     */
    public Auction getActiveAuction(int auctionId) {
        return activeAuctions.get(auctionId);
    }

    /**
     * Remove an active auction from memory.
     */
    public void removeActiveAuction(int auctionId) {
        activeAuctions.remove(auctionId);
    }

    /**
     * Check if an auction is active.
     */
    public boolean isAuctionActive(int auctionId) {
        return activeAuctions.containsKey(auctionId);
    }

    /**
     * Get all active auctions.
     */
    public java.util.Collection<Auction> getAllActiveAuctions() {
        return activeAuctions.values();
    }

    public void loadActiveAuctions() {
        java.util.List<AuctionData> activeAuctionData = auctionDAO.findAllActiveAuctions();
        logger.info("Loading " + activeAuctionData.size() + " active auctions from database");

        for (AuctionData auctionData : activeAuctionData) {
            logger.debug("Loading auction: " + auctionData.id() + " - " + auctionData.item().getName() + " - State: " + auctionData.state());
            try {
                Auction auction = new Auction(
                auctionData.id(),
                auctionData.item(),
                userDAO.findByUserId(auctionData.sellerId()).username(),
                auctionData.startTime(),
                auctionData.endTime()
            );

            auction.setAuctionState(AuctionState.valueOf(auctionData.state()));

            // reconstruct bid history and observers
            java.util.List<BidDAO.BidData> bidHistory = auctionDAO.findBidHistoryByAuctionId(auctionData.id());
            java.util.Set<Integer> bidders = new HashSet<>();
            
            for (BidDAO.BidData bidData : bidHistory) {
                BidTransaction tx = new BidTransaction(bidData.userId(), userDAO.findByUserId(bidData.userId()).username(), bidData.amount(), bidData.time());
                auction.updatePrice(bidData.amount(), tx);
                bidders.add(bidData.userId());
            }

            // subscribe all bidders as observers
            for (Integer bidderId : bidders) {
                auction.subscribe(bidderId);
            }

            // always subscribe seller
            auction.subscribe(auctionData.sellerId());

            activeAuctions.put(auctionData.id(), auction);
            
            } catch (Exception e) {
                logger.error("Failed to load auction " + auctionData.id() + ": " + e.getMessage(), e);
            }
        }
    }

    public List<Auction> getActiveAuctionList() {
        return new ArrayList<>(activeAuctions.values());
    }

    public List<Auction> getActiveAuctionListExceptForSeller(int userId) {
        String sellerName = userDAO.findByUserId(userId).username();

        ArrayList<Auction> activeAuctionsExceptForSeller = new ArrayList<>();
        for (Auction auction : activeAuctions.values()) {
            if (!auction.getSellerName().equals(sellerName)) {
                activeAuctionsExceptForSeller.add(auction);
            }
        }

        return activeAuctionsExceptForSeller;
    }

    public Auction getActiveAuctionById(int auctionId) {
        return activeAuctions.get(auctionId);
    }

    public Auction getAuctionById(int auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction != null) {
            return auction;
        }

        AuctionData auctionData = auctionDAO.findAuctionById(auctionId);
        if (auctionData == null) {
            return null;
        }

        auction = new Auction(
            auctionData.id(),
            auctionData.item(),
            userDAO.findByUserId(auctionData.sellerId()).username(),
            auctionData.startTime(),
            auctionData.endTime()
        );

        auction.setAuctionState(AuctionState.valueOf(auctionData.state()));

        // reconstruct bid history and observers
        java.util.List<BidDAO.BidData> bidHistory = bidDAO.findBidHistoryByAuctionId(auctionData.id());
        java.util.Set<Integer> bidders = new HashSet<>();
        
        for (BidDAO.BidData bidData : bidHistory) {
            BidTransaction tx = new BidTransaction(bidData.userId(), userDAO.findByUserId(bidData.userId()).username(), bidData.amount(), bidData.time());
            auction.updatePrice(bidData.amount(), tx);
            bidders.add(bidData.userId());
        }

        // subscribe all bidders as observers
        for (Integer bidderId : bidders) {
            auction.subscribe(bidderId);
        }

        // always subscribe seller
        auction.subscribe(auctionData.sellerId());

        activeAuctions.put(auctionData.id(), auction);

        return auction;
    }

    public List<Auction> getAuctionsBySellerId(int sellerId) {
        java.util.List<AuctionData> auctionDataList = auctionDAO.findAuctionsBySellerId(sellerId);
        java.util.List<Auction> auctions = new java.util.ArrayList<>();
        
        for (AuctionData auctionData : auctionDataList) {
            Auction auction = getAuctionById(auctionData.id());
            if (auction != null) {
                auctions.add(auction);
            }
        }
        
        return auctions;
    }

    public List<Auction> getAuctionsByWinnerId(int winnerId) {
        java.util.List<AuctionData> auctionDataList = auctionDAO.findAuctionsByWinnerId(winnerId);
        java.util.List<Auction> auctions = new java.util.ArrayList<>();
        
        for (AuctionData auctionData : auctionDataList) {
            Auction auction = getAuctionById(auctionData.id());
            if (auction != null) {
                auctions.add(auction);
            }
        }
        
        return auctions;
    }

    public String listActiveAuctions() {
        String result = "";
        for (Auction auction : activeAuctions.values()) {
            result += auction.toString() + "\n\n";
        }
        return result;
    }

    public boolean insertBid(int userId, int auctionId, double amount, LocalDateTime time) {
        return bidDAO.insertBid(userId, auctionId, amount, time);
    }

    public boolean updateCurrentPrice(int auctionId, double newPrice, int winnerId) {
        return auctionDAO.updateCurrentPrice(auctionId, newPrice, winnerId);
    }

    public java.util.List<BidDAO.BidData> findBidHistoryByAuctionId(int auctionId) {
        return bidDAO.findBidHistoryByAuctionId(auctionId);
    }

    public Double findCurrentPrice(int auctionId) {
        return bidDAO.findCurrentPrice(auctionId);
    }

    public String getUsernameByUserId(int userId) {
        return userDAO.findByUserId(userId).username();
    }

    public int insertItem(Item item) {
        return auctionDAO.insertItem(item);
    }

    public int insertAuction(int itemId, int sellerId, double startPrice, double curPrice,
                             LocalDateTime startTime, LocalDateTime endTime) {
        return auctionDAO.insertAuction(itemId, sellerId, startPrice, curPrice, startTime, endTime);
    }

    public boolean updateEndTime(int auctionId, LocalDateTime newEndTime) {
        return auctionDAO.updateEndTime(auctionId, newEndTime);
    }

    public boolean setAuctionState(int auctionId, AuctionState newState) {
        return auctionDAO.setAuctionState(auctionId, newState);
    }
}
