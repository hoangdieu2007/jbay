package a88.jbay.system;

import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.common.auction.BidTransaction;
import a88.jbay.dao.AuctionDAO;
import a88.jbay.dao.BidDAO;
import a88.jbay.di.ApplicationContext;
import a88.jbay.repository.AuctionRepository;
import a88.jbay.util.JBayLogger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Coordinates bid placement, bid persistence, and auto-bidding behavior for active auctions.
 */
public class BidSystem {
    private final AuctionRepository auctionRepository;
    private final AtomicBoolean isAutoBidding = new AtomicBoolean(false);
    private final JBayLogger logger;

    // auto-bid configuration - supports multiple users per auction
    private final Map<Integer, Map<Integer, AutoBidConfig>> auctionAutoBidConfigs;
    private final BidDAO bidDAO;
    private final AuctionDAO auctionDAO;

    /**
     * Creates a bid system with the repositories and DAOs needed to manage bids.
     *
     * <p>Processing: stores the injected collaborators, initializes the in-memory auto-bid
     * configuration map, and prepares a class-specific logger for bid and auto-bid events.</p>
     *
     * @param auctionRepository repository used to read and update in-memory auction state
     * @param bidDAO DAO used to read and write bid transactions
     * @param auctionDAO DAO used to read and update persisted auction price data
     */
    public BidSystem(
            AuctionRepository auctionRepository,
            BidDAO bidDAO,
            AuctionDAO auctionDAO
    ) {

        this.auctionRepository = auctionRepository;
        this.auctionAutoBidConfigs = new HashMap<>();
        this.bidDAO = bidDAO;
        this.auctionDAO = auctionDAO;
        this.logger = JBayLogger.getLogger(BidSystem.class);
    }

    /**
     * Returns the application-wide {@code BidSystem} instance.
     *
     * <p>Processing: retrieves the singleton {@link ApplicationContext} and asks it for the
     * registered {@code BidSystem} dependency.</p>
     *
     * @return the configured {@code BidSystem} instance
     */
    public static BidSystem getInstance() {

        return ApplicationContext
                .getInstance()
                .getDependency(BidSystem.class);
    }

    /**
     * Places a manual bid for a user on an active auction.
     *
     * <p>Processing: loads the active auction, validates that the bid is higher than the current
     * price and that the auction is running, creates a bid transaction, applies it to the in-memory
     * auction, and persists both the updated price and the bid transaction.</p>
     *
     * @param userId ID of the user placing the bid
     * @param auctionId ID of the auction being bid on
     * @param amount bid amount offered by the user
     * @return {@code true} if the bid is valid and saved successfully; {@code false} otherwise
     */
    public synchronized boolean placeBid(int userId, int auctionId, double amount) {

        Auction auction = auctionRepository.getActiveAuctionById(auctionId);

        if (!isValidBid(auction, amount)) {
            return false;
        }

        BidTransaction tx = createBidTransaction(userId, amount);
        addBid(auction, tx); // subscribe user to auction and update auction price
        return saveBid(auctionId, tx); // save bid to DB
    }

    /**
     * Gets the persisted bid history for an auction.
     *
     * <p>Processing: delegates directly to {@link BidDAO} so the DAO can query and map the bid
     * records associated with the given auction ID.</p>
     *
     * @param auctionId ID of the auction whose bid history should be loaded
     * @return list of bid history records for the auction
     */
    public List<BidDAO.BidData> getBidHistory(int auctionId) {
        return bidDAO.findBidHistoryByAuctionId(auctionId);
    }

    /**
     * Gets the current persisted price for an auction.
     *
     * <p>Processing: delegates to {@link AuctionDAO} to read the current price from the persisted
     * auction record.</p>
     *
     * @param auctionId ID of the auction whose current price should be loaded
     * @return the current price, or {@code null} if the DAO cannot find a value
     */
    public Double getCurrentPrice(int auctionId) {
        return auctionDAO.findCurrentPrice(auctionId);
    }

    /**
     * Checks whether a bid can be accepted for the supplied auction.
     *
     * <p>Processing: rejects missing auctions, rejects auctions that are not in the
     * {@link AuctionState#RUNNING} state, and finally checks that the submitted amount is greater
     * than the auction's current price.</p>
     *
     * @param auction active auction to validate against
     * @param amount submitted bid amount
     * @return {@code true} if the auction exists, is running, and the amount is high enough
     */
    private boolean isValidBid(Auction auction, double amount) {

        if (auction == null) {
            return false;
        }

        if (auction.getAuctionState() != AuctionState.RUNNING) {
            return false;
        }

        return amount > auction.getCurrentPrice();
    }

    /**
     * Builds a bid transaction object for a newly accepted bid.
     *
     * <p>Processing: resolves the bidder's username from the repository and creates a
     * {@link BidTransaction} with the user ID, username, amount, and current timestamp.</p>
     *
     * @param userId ID of the user placing the bid
     * @param amount accepted bid amount
     * @return bid transaction ready to be applied and persisted
     */
    private BidTransaction createBidTransaction(int userId, double amount) {

        String username = auctionRepository.getUsernameByUserId(userId); // get bidder username

        return new BidTransaction(
                userId,
                username,
                amount,
                LocalDateTime.now()
        );
    }

    /**
     * Applies a bid transaction to the in-memory auction state.
     *
     * <p>Processing: subscribes the bidder to auction updates and then updates the auction price,
     * winner, transaction history, and observer notifications through {@link Auction#updatePrice}.</p>
     *
     * @param auction auction that should receive the bid
     * @param tx accepted bid transaction
     */
    private void addBid(Auction auction, BidTransaction tx) {
        // bidder automatically becomes observer
        auction.subscribe(tx.getUserID()); // subscribe first so that client can get a notification
        auction.updatePrice(tx.getAmt(), tx);
    }

    /**
     * Persists an accepted bid and its resulting auction price.
     *
     * <p>Processing: first updates the auction's current price and winner in persistent storage;
     * only if that succeeds does it insert the bid transaction record.</p>
     *
     * @param auctionId ID of the auction receiving the bid
     * @param tx accepted bid transaction to save
     * @return {@code true} if both the auction price update and bid insert succeed
     */
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

    /**
     * Stores or replaces a user's auto-bid configuration for an auction.
     *
     * <p>Processing: creates the per-auction configuration map when needed, writes the user's
     * maximum amount and increment, and synchronizes the active auction object with the new
     * configuration snapshot.</p>
     *
     * @param auctionId ID of the auction where auto-bidding is enabled
     * @param userId ID of the user enabling auto-bidding
     * @param maxAmount highest amount the system may bid for the user
     * @param increment amount to add above the current price for each automated bid
     */
    public void setAutoBidConfig(int auctionId, int userId, double maxAmount, double increment) {
        auctionAutoBidConfigs.computeIfAbsent(auctionId, k -> new HashMap<>()).put(userId, new AutoBidConfig(maxAmount, increment));
        syncAuctionAutoBidConfigs(auctionId);
    }

    /**
     * Removes one user's auto-bid configuration from an auction.
     *
     * <p>Processing: finds the auction's configuration map, removes the user's entry, deletes the
     * per-auction map if it becomes empty, and refreshes the active auction's auto-bid snapshot.</p>
     *
     * @param auctionId ID of the auction where auto-bidding should be cleared
     * @param userId ID of the user whose auto-bid configuration should be removed
     */
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

    /**
     * Returns the auto-bid configurations currently registered for an auction.
     *
     * <p>Processing: reads the in-memory per-auction configuration map and returns a defensive copy
     * so callers cannot directly mutate the internal auto-bid state.</p>
     *
     * @param auctionId ID of the auction whose auto-bid configurations should be read
     * @return copied map from user ID to auto-bid configuration, or an empty map when none exist
     */
    public Map<Integer, AutoBidConfig> getAutoBidConfigs(int auctionId) {
        Map<Integer, AutoBidConfig> configs = auctionAutoBidConfigs.get(auctionId);
        return configs != null ? new HashMap<>(configs) : new HashMap<>();
    }

    /**
     * Clears every auto-bid configuration for an auction.
     *
     * <p>Processing: removes the auction's configuration map from memory and synchronizes the
     * active auction object so observers see that no auto-bid configurations remain.</p>
     *
     * @param auctionId ID of the auction whose auto-bid configurations should be removed
     */
    public void clearAllAutoBidConfigs(int auctionId) {
        auctionAutoBidConfigs.remove(auctionId);
        syncAuctionAutoBidConfigs(auctionId);
    }

    /**
     * Checks whether a user has enabled auto-bidding on an auction.
     *
     * <p>Processing: looks up the auction's configuration map and tests whether it contains the
     * requested user ID.</p>
     *
     * @param auctionId ID of the auction to inspect
     * @param userId ID of the user to check
     * @return {@code true} if the user currently has an auto-bid configuration on the auction
     */
    public boolean hasAutoBidConfig(int auctionId, int userId) {
        Map<Integer, AutoBidConfig> configs = auctionAutoBidConfigs.get(auctionId);
        return configs != null && configs.containsKey(userId);
    }

    /**
     * Enables automated bidding for a user on an active auction.
     *
     * <p>Processing: loads the active auction, stores the user's auto-bid limits, subscribes the
     * user for updates, notifies observers, resolves competitive auto-bids immediately when more
     * than one configuration exists, and otherwise places an initial automatic bid if the user is
     * not already winning and the next increment does not exceed the maximum amount.</p>
     *
     * @param userId ID of the user enabling auto-bidding
     * @param auctionId ID of the auction where auto-bidding should run
     * @param maxAmount highest amount the system may bid for the user
     * @param increment amount to add above the current auction price for automated bids
     */
    public synchronized void placeBidAutomated(int userId, int auctionId, double maxAmount, double increment) {
        Auction auction = auctionRepository.getActiveAuctionById(auctionId);
        if (auction == null) {
            logger.warn("Auto-bid failed - auction not found or not active: " + auctionId);
            return;
        }

        setAutoBidConfig(auctionId, userId, maxAmount, increment);

        // Subscribe user to auction to receive price change notifications.
        auction.subscribe(userId);

        logger.info("Auto-bid enabled for user " + userId + " on auction " + auctionId +
                          " with maxAmount=" + maxAmount + ", increment=" + increment);
        auction.notifyObservers();

        if (getAutoBidConfigs(auctionId).size() >= 2) {
            logger.info("Multiple auto-bids detected on auction " + auctionId + ", applying competitive bidding logic");
            handleMultipleAutoBids(auction);
            return;
        }

        if (auction.getWinnerId() != null && auction.getWinnerId().equals(userId)) {
            logger.info("Skipping initial auto-bid for user " + userId + " on auction " + auctionId +
                              ": user is already the current winner");
            return;
        }

        double initialBidAmount = auction.getCurrentPrice() + increment;

        if (initialBidAmount <= maxAmount) {
            placeBid(userId, auctionId, initialBidAmount);
        }
    }

    /**
     * Cancels automated bidding for a user on an active auction.
     *
     * <p>Processing: loads the active auction, removes the user's auto-bid configuration, logs the
     * cancellation, and notifies auction observers so connected clients can refresh their state.</p>
     *
     * @param userId ID of the user canceling auto-bidding
     * @param auctionId ID of the auction where auto-bidding should be canceled
     */
    public synchronized void cancelAutoBid(int userId, int auctionId) {
        Auction auction = auctionRepository.getActiveAuctionById(auctionId);
        if (auction == null) {
            logger.warn("Cancel auto-bid failed - auction not found or not active: " + auctionId);
            return;
        }

        clearAutoBidConfig(auctionId, userId);
        logger.info("Auto-bid canceled for user " + userId + " on auction " + auctionId);
        auction.notifyObservers();
    }

    /**
     * Resolves an auction that has multiple active auto-bid configurations.
     *
     * <p>Processing: sorts auto-bidders by maximum amount, compares the two strongest bidders,
     * places the winning bid at the lower of the top maximum and the second maximum plus the top
     * bidder's increment, then clears all auto-bid configurations for the auction and notifies
     * observers.</p>
     *
     * @param auction auction whose competing auto-bids should be resolved
     */
    private void handleMultipleAutoBids(Auction auction) {
        List<Map.Entry<Integer, AutoBidConfig>> sortedConfigs = getAutoBidConfigs(auction.getId()).entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<Integer, AutoBidConfig> e) -> e.getValue().getMaxAmount()).reversed())
                .collect(Collectors.toList());

        if (sortedConfigs.size() < 2) {
            logger.warn("handleMultipleAutoBids called with less than 2 auto-bids");
            return;
        }

        Map.Entry<Integer, AutoBidConfig> topBidder = sortedConfigs.get(0);
        Map.Entry<Integer, AutoBidConfig> secondBidder = sortedConfigs.get(1);

        int topUserId = topBidder.getKey();
        double topMaxAmount = topBidder.getValue().getMaxAmount();
        double topIncrement = topBidder.getValue().getIncrement();

        double secondMaxAmount = secondBidder.getValue().getMaxAmount();
        double finalPrice = Math.min(topMaxAmount, secondMaxAmount + topIncrement);

        logger.info("Competitive auto-bid resolution: User " + topUserId + " wins with price " + finalPrice +
                          " (max=" + topMaxAmount + ", second_max=" + secondMaxAmount + ", increment=" + topIncrement + ")");

        placeBid(topUserId, auction.getId(), finalPrice);

        clearAllAutoBidConfigs(auction.getId());
        logger.info("All auto-bids canceled for auction " + auction.getId());
        auction.notifyObservers();
    }

    /**
     * Copies the latest auto-bid configuration map into the active auction object.
     *
     * <p>Processing: loads the active auction by ID and, when it exists, assigns a defensive copy of
     * the current auto-bid configurations to that auction.</p>
     *
     * @param auctionId ID of the auction whose in-memory auto-bid state should be synchronized
     */
    private void syncAuctionAutoBidConfigs(int auctionId) {
        Auction auction = auctionRepository.getActiveAuctionById(auctionId);
        if (auction != null) {
            auction.setAutoBidConfigs(getAutoBidConfigs(auctionId));
        }
    }

    /**
     * Attempts to place an automatic bid after an auction price change.
     *
     * <p>Processing: reads the auction's auto-bid configurations, skips processing when none exist,
     * uses an atomic guard to avoid recursive auto-bid execution, filters out the current winner,
     * chooses the candidate with the highest maximum amount, computes the next bid from the current
     * price plus that user's increment, cancels the configuration if the computed amount exceeds the
     * user's maximum, and otherwise places the automated bid.</p>
     *
     * @param auction auction whose price change may trigger an auto-bid
     */
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
