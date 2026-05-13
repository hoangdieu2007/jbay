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
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Comparator;
import java.util.stream.Collectors;

public class BidSystem {
    private final AuctionRepository auctionRepository;
    private final AtomicBoolean isAutoBidding = new AtomicBoolean(false);

    // auto-bid configuration - supports multiple users per auction
    private final Map<Integer, Map<Integer, AutoBidConfig>> auctionAutoBidConfigs;
    private final BidDAO bidDAO;
    private final AuctionDAO auctionDAO;

    // Constructor for dependency injection
    public BidSystem(
            AuctionRepository auctionRepository,
            BidDAO bidDAO,
            AuctionDAO auctionDAO
    ) {

        this.auctionRepository = auctionRepository;
        this.auctionAutoBidConfigs = new HashMap<>();
        this.bidDAO = bidDAO;
        this.auctionDAO = auctionDAO;
    }

    // Singleton accessor via ApplicationContext
    public static BidSystem getInstance() {

        return ApplicationContext
                .getInstance()
                .getDependency(BidSystem.class);
    }

    public synchronized boolean placeBid(int userId, int auctionId, double amount) {

        Auction auction = auctionRepository.getActiveAuctionById(auctionId);

        if (!isValidBid(auction, amount)) {
            return false;
        }

        BidTransaction tx = createBidTransaction(userId, amount);
        addBid(auction, tx);
        return saveBid(auctionId, tx);
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

    private BidTransaction createBidTransaction(int userId, double amount) {

        String username = auctionRepository.getUsernameByUserId(userId);

        return new BidTransaction(
                userId,
                username,
                amount,
                LocalDateTime.now()
        );
    }

    private void addBid(Auction auction, BidTransaction tx) {
        // bidder automatically becomes observer
        auction.subscribe(tx.getUserID());
        auction.updatePrice(tx.getAmt(), tx);
    }

    private boolean saveBid(int auctionId, BidTransaction tx) {
        boolean priceUpdated = auctionDAO.updateCurrentPrice(auctionId, tx.getAmt(), tx.getUserID());

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

    // auto-bid methods - moved from Auction
    public void setAutoBidConfig(int auctionId, int userId, double maxAmount, double increment) {
        auctionAutoBidConfigs.computeIfAbsent(auctionId, k -> new HashMap<>()).put(userId, new AutoBidConfig(maxAmount, increment));
        syncAuctionAutoBidConfigs(auctionId);
    }

    public void clearAutoBidConfig(int auctionId, int userId) {
        Map<Integer, AutoBidConfig> configs = auctionAutoBidConfigs.get(auctionId);
        if (configs != null) {
            configs.remove(userId);
            if (configs.isEmpty()) {
                auctionAutoBidConfigs.remove(auctionId);
            }
        }
        syncAuctionAutoBidConfigs(auctionId);
    }

    public Map<Integer, AutoBidConfig> getAutoBidConfigs(int auctionId) {
        Map<Integer, AutoBidConfig> configs = auctionAutoBidConfigs.get(auctionId);
        return configs != null ? new HashMap<>(configs) : new HashMap<>();
    }

    public void clearAllAutoBidConfigs(int auctionId) {
        auctionAutoBidConfigs.remove(auctionId);
        syncAuctionAutoBidConfigs(auctionId);
    }

    public boolean hasAutoBidConfig(int auctionId, int userId) {
        Map<Integer, AutoBidConfig> configs = auctionAutoBidConfigs.get(auctionId);
        return configs != null && configs.containsKey(userId);
    }

    private void syncAuctionAutoBidConfigs(int auctionId) {
        Auction auction = auctionRepository.getActiveAuctionById(auctionId);
        if (auction != null) {
            auction.setAutoBidConfigs(getAutoBidConfigs(auctionId));
        }
    }

    public void triggerAutoBid(Auction auction) {
        int auctionId = auction.getId();
        Map<Integer, AutoBidConfig> autoBidConfigs = auctionAutoBidConfigs.get(auctionId);
        if (autoBidConfigs == null || autoBidConfigs.isEmpty()) {
            return;
        }

        // Prevent recursive auto-bid calls
        if (!isAutoBidding.compareAndSet(false, true)) {
            return; // another thread is already processing auto-bid
        }

        try {
            // Filter out current winner from auto-bid candidates
            List<Map.Entry<Integer, AutoBidConfig>> candidates = autoBidConfigs.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(auction.getWinnerId()))
                    .sorted(Comparator.comparingDouble((Map.Entry<Integer, AutoBidConfig> e) -> e.getValue().getMaxAmount()).reversed())
                    .collect(Collectors.toList());

            if (candidates.isEmpty()) {
                return;
            }

            // Get the top bidder by max_amount
            Map.Entry<Integer, AutoBidConfig> topBidder = candidates.get(0);
            int winningUserId = topBidder.getKey();
            double winningMaxAmount = topBidder.getValue().getMaxAmount();
            double winningIncrement = topBidder.getValue().getIncrement();

            double autoBidAmount = auction.getCurrentPrice() + winningIncrement;

            // Check if auto-bid amount exceeds max_amount
            if (autoBidAmount > winningMaxAmount) {
                System.out.println("Auto-bid stopped for user " + winningUserId + " on auction " + auction.getId() +
                                  ": auto-bid amount (" + autoBidAmount + ") exceeds max_amount (" + winningMaxAmount + ")");
                clearAutoBidConfig(auctionId, winningUserId);
                auction.notifyObservers();
                return;
            }

            // Check if auto-bid amount is higher than current price
            if (autoBidAmount <= auction.getCurrentPrice()) {
                return;
            }

            // Place the auto-bid
            placeBid(winningUserId, auction.getId(), autoBidAmount);
        } finally {
            // Reset flag after bid is placed
            isAutoBidding.set(false);
        }
    }
}
