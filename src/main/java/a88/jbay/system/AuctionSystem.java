package a88.jbay.system;

import a88.jbay.common.auction.AutoBidConfig;
import a88.jbay.util.JBayLogger;
import a88.jbay.common.item.Item;
import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.common.auction.BidTransaction;
import a88.jbay.common.network.Response;
import a88.jbay.di.ApplicationContext;
import a88.jbay.repository.AuctionRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/*
the code for operations on the auction data

features: real-time bidding, auction lifecycle management
 */

public class AuctionSystem {
    private final UpdateSystem updateSystem;
    private final AuctionRepository auctionRepository;
    private final JBayLogger logger;

    private final ScheduledExecutorService scheduler;

    // Constructor for dependency injection
    public AuctionSystem(UpdateSystem updateSystem, AuctionRepository auctionRepository) {
        this.updateSystem = updateSystem;
        this.auctionRepository = auctionRepository;
        this.logger = JBayLogger.getLogger(AuctionSystem.class);
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        auctionRepository.loadActiveAuctions();
        startHeartbeat();
    }

    // Singleton accessor via ApplicationContext
    public static AuctionSystem getInstance() {
        return ApplicationContext.getInstance().getDependency(AuctionSystem.class);
    }


    //create auction and store to database
    public boolean createAuction(Item item, int sellerId, LocalDateTime start, LocalDateTime end) {
        logger.info("Creating auction for item: " + item.getName() + " by seller: " + sellerId);

        // 1. Insert the item first
        int itemId = auctionRepository.insertItem(item);
        if (itemId == -1) {
            logger.error("Failed to insert item: " + item.getName());
            return false;
        }

        // 2. Create the auction record
        int auctionId = auctionRepository.insertAuction(
                itemId,
                sellerId,
                item.getInitPrice(),
                item.getInitPrice(),
                start,
                end
        );
        if (auctionId == -1) {
            logger.error("Failed to create auction for item: " + item.getName());
            return false;
        }

        String sellerName = auctionRepository.getUsernameByUserId(sellerId);
        Auction auction = new Auction(
                auctionId,
                item,
                sellerName,
                start,
                end
        );
        auctionRepository.storeActiveAuction(auction);
        auction.subscribe(sellerId); // Seller is automatically subscribed

        //update everyone about this auction
        UpdateSystem.getInstance().updateAllUsers(
                new Response(true, "AUCTION_UPDATE", auction)
        );

        logger.info("Auction created successfully: ID=" + auctionId + ", Item=" + item.getName());
        return true;
    }

    //place bid and validate bid - delegate to BidSystem
    public synchronized boolean placeBid(int userId, int auctionId, double amount) {
        logger.debug("Bid attempt: User=" + userId + ", Auction=" + auctionId + ", Amount=" + amount);
        boolean bidPlaced = BidSystem.getInstance().placeBid(userId, auctionId, amount);
        
        if (bidPlaced) {
            logger.info("Bid placed successfully: User=" + userId + ", Auction=" + auctionId + ", Amount=" + amount);
            // anti-sniping check upon successful bid
            Auction auction = auctionRepository.getActiveAuctionById(auctionId);
            if (auction != null) {
                extendEndTime(LocalDateTime.now(), auction);
            }
        } else {
            logger.warn("Bid failed: User=" + userId + ", Auction=" + auctionId + ", Amount=" + amount);
        }
        
        return bidPlaced;
    }

    public synchronized void placeBidAutomated(int userId, int auctionId, double max_amount, double increment) {
        /*
        Phương thức dùng để tự động hoá quá trình placeBid
        Tự động placeBid khi một giá mới được đăng ký (current price trong auction)
        Giá auto placeBid sẽ cao hơn giá mới nhất một lượng bằng "increment"
        Nếu giá auto placeBid cao hơn max_amount thì dừng auto bid
        */

        Auction auction = auctionRepository.getActiveAuctionById(auctionId);
        if (auction == null) {
            logger.warn("Auto-bid failed - auction not found or not active: " + auctionId);
            return;
        }

        // Check if this auction already has auto-bids (excluding the current user if they already have one)
        boolean existingAutoBid = auction.hasAutoBidConfig(userId);
        int autoBidCount = auction.getAutoBidConfigs().size();
        
        // Store auto-bid configuration for this user and auction
        auction.setAutoBidConfig(userId, max_amount, increment);

        // Subscribe user to auction to receive price change notifications
        auction.subscribe(userId);

        logger.info("Auto-bid enabled for user " + userId + " on auction " + auctionId +
                          " with max_amount=" + max_amount + ", increment=" + increment);

        // If there are now 2 or more auto-bids, apply the new logic
        if (autoBidCount >= 1 || (!existingAutoBid && auction.getAutoBidConfigs().size() >= 2)) {
            logger.info("Multiple auto-bids detected on auction " + auctionId + ", applying competitive bidding logic");
            handleMultipleAutoBids(auction);
            return;
        }

        // Immediately place a bid after enabling auto-bid (single auto-bid case)
        // Check if current winner is the user making the auto-bid request
        if (auction.getWinnerId() != null && auction.getWinnerId().equals(userId)) {
            logger.info("Skipping initial auto-bid for user " + userId + " on auction " + auctionId +
                              ": user is already the current winner");
            return;
        }

        double currentPrice = auction.getCurrentPrice();
        double initialBidAmount = currentPrice + increment;

        // Check if the initial bid amount exceeds max_amount
        if (initialBidAmount <= max_amount) {
            placeBid(userId, auctionId, initialBidAmount);
        }
    }

    private void handleMultipleAutoBids(Auction auction) {
        // Get all auto-bid configs and sort by max_amount descending
        java.util.List<java.util.Map.Entry<Integer, AutoBidConfig>> sortedConfigs = auction.getAutoBidConfigs().entrySet().stream()
                .sorted(java.util.Comparator.comparingDouble((java.util.Map.Entry<Integer, AutoBidConfig> e) -> e.getValue().getMaxAmount()).reversed())
                .collect(java.util.stream.Collectors.toList());

        if (sortedConfigs.size() < 2) {
            logger.warn("handleMultipleAutoBids called with less than 2 auto-bids");
            return;
        }

        // Get top 2 bidders by max_amount
        java.util.Map.Entry<Integer, AutoBidConfig> topBidder = sortedConfigs.get(0);
        java.util.Map.Entry<Integer, AutoBidConfig> secondBidder = sortedConfigs.get(1);

        int topUserId = topBidder.getKey();
        double topMaxAmount = topBidder.getValue().getMaxAmount();
        double topIncrement = topBidder.getValue().getIncrement();

        double secondMaxAmount = secondBidder.getValue().getMaxAmount();
        double secondIncrement = secondBidder.getValue().getIncrement();

        // Calculate final price: min(max_amount of top bidder, max_amount of second bidder + increment of top bidder)
        double finalPrice = Math.min(topMaxAmount, secondMaxAmount + topIncrement);

        logger.info("Competitive auto-bid resolution: User " + topUserId + " wins with price " + finalPrice +
                          " (max=" + topMaxAmount + ", second_max=" + secondMaxAmount + ", increment=" + topIncrement + ")");

        // Place the final bid
        placeBid(topUserId, auction.getId(), finalPrice);

        // Cancel all auto-bids in this auction
        auction.clearAllAutoBidConfigs();
        logger.info("All auto-bids canceled for auction " + auction.getId());
    }

    public synchronized void cancelAutoBid(int userId, int auctionId) {
        Auction auction = auctionRepository.getActiveAuctionById(auctionId);
        if (auction == null) {
            logger.warn("Cancel auto-bid failed - auction not found or not active: " + auctionId);
            return;
        }

        auction.clearAutoBidConfig(userId);
        logger.info("Auto-bid canceled for user " + userId + " on auction " + auctionId);
    }

    public void extendEndTime(LocalDateTime now, Auction auction) {
        //use anti-sniping: automatically extend the duration of an auction
        //when it receives a placeBid request close to its end

        int ANTI_SNIPING_EXTENSION_SECONDS = 3600;
        int ANTI_SNIPING_THRESHOLD_SECONDS = 300;

        long secondsUntilEnd = java.time.Duration.between(now, auction.getEndTime()).getSeconds();

        if (secondsUntilEnd <= ANTI_SNIPING_THRESHOLD_SECONDS && secondsUntilEnd > 0) {
            LocalDateTime newEndTime = auction.getEndTime().plusSeconds(ANTI_SNIPING_EXTENSION_SECONDS);
            auction.setEndTime(newEndTime);

            boolean updated = auctionRepository.updateEndTime(auction.getId(), newEndTime);

            if (updated) {
                // Notify all subscribers about the extension
                auction.notifyObservers();
                logger.info("Auction " + auction.getId() + " extended due to anti-sniping. " +
                        "New end time: " + newEndTime);
            }
        }
    }

    //cancel auction
    //ONLY ADMIN CAN CALL THIS
    public boolean cancelAuction(int auctionId) {
        logger.info("Attempting to cancel auction: " + auctionId);
        Auction auction = auctionRepository.getActiveAuctionById(auctionId);
        if (auction != null) {
            auction.cancel();
        }

        boolean closed = auctionRepository.setAuctionState(auctionId, AuctionState.CANCELED);
        if (closed) {
            auctionRepository.removeActiveAuction(auctionId);
            logger.info("Auction canceled successfully: " + auctionId);
            // Subscribers are automatically cleared when auction is removed from memory
        } else {
            logger.error("Failed to cancel auction: " + auctionId);
        }
        return closed;
    }

    public boolean isAuctionActive(int auctionId) {
        return auctionRepository.isAuctionActive(auctionId);
    }

    public List<Auction> getActiveAuctionList() {
        return auctionRepository.getActiveAuctionList();
    }

    public List<Auction> getActiveAuctionListExceptForSeller(int userId) {
        return auctionRepository.getActiveAuctionListExceptForSeller(userId);
    }

    public Auction getActiveAuctionById(int auctionId) {
        return auctionRepository.getActiveAuctionById(auctionId);
    }

    public Auction getAuctionById(int auctionId) {
        return auctionRepository.getAuctionById(auctionId);
    }

    public List<Auction> getAuctionsBySellerId(int sellerId) {
        return auctionRepository.getAuctionsBySellerId(sellerId);
    }

    public List<Auction> getAuctionsByWinnerId(int winnerId) {
        return auctionRepository.getAuctionsByWinnerId(winnerId);
    }

    public String listActiveAuctions() {
        return auctionRepository.listActiveAuctions();
    }

    /*
    code section for handling auction state transitions
     */
    private void startHeartbeat() {
        scheduler.scheduleAtFixedRate(this::checkAuctionTransitions, 0, 1, TimeUnit.SECONDS);
    }

    private void checkAuctionTransitions() {
        LocalDateTime now = LocalDateTime.now();
        List<Integer> ended = new ArrayList<>();

        for (Auction auction : auctionRepository.getAllActiveAuctions()) {
            // check and change state
            if (auction.tick(now)) {
                logger.debug("Heartbeat: State changed for auction " + auction.getId() + " to " + auction.getAuctionState());
                auctionRepository.setAuctionState(auction.getId(), auction.getAuctionState());
            }

            // Add canceled/finsihed auctions to ended list to remove
            if (auction.getAuctionState() == AuctionState.CANCELED || auction.getAuctionState() == AuctionState.FINISHED) {
                ended.add(auction.getId());
                logger.info("Auction ended: " + auction.getId() + " - " + auction.getAuctionState());
            }
        }

        //remove ended auctions from memory
        for (int auctionId : ended) {
            auctionRepository.removeActiveAuction(auctionId);
            // Subscribers are automatically cleared when auction is removed from memory
        }
    }

    //stopping the heartbeat, WARNING: no automatic auction lifecycle management after stopping
    //do NOT call this method unless for testing purpose
    public void stopSystem() {
        try {
            // Shutdown the scheduler and wait for tasks to complete
            scheduler.shutdown();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warn("Scheduler did not terminate");
                }
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
