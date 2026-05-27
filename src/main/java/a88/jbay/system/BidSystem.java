package a88.jbay.system;

import a88.jbay.common.auction.*;
import a88.jbay.dao.AuctionDAO;
import a88.jbay.dao.BidDAO;
import a88.jbay.data.BidRepository;
import a88.jbay.di.ApplicationContext;
import a88.jbay.data.AuctionRepository;
import a88.jbay.system.update.UpdateSystem;
import a88.jbay.util.JBayLogger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Coordinates bid placement, bid persistence, and auto-bidding behavior for active auctions.
 */
public class BidSystem {
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AtomicBoolean isAutoBidding = new AtomicBoolean(false);
    private final ConcurrentHashMap<Integer, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();
    private final JBayLogger logger;

    private final BidDAO bidDAO;
    private final AuctionDAO auctionDAO;
    private final UpdateSystem updateSystem;

    /**
     * Creates a bid system with the repositories and DAOs needed to manage bids.
     *
     * <p>Processing: stores the injected collaborators and prepares a class-specific logger for
     * bid and auto-bid events.</p>
     *
     * @param auctionRepository repository used to read and update in-memory auction state
     * @param bidDAO DAO used to read and write bid transactions
     * @param auctionDAO DAO used to read and update persisted auction price data
     */
    public BidSystem(
            AuctionRepository auctionRepository,
            BidRepository bidRepository,
            BidDAO bidDAO,
            AuctionDAO auctionDAO,
            UpdateSystem updateSystem
    ) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.bidDAO = bidDAO;
        this.auctionDAO = auctionDAO;
        this.updateSystem = updateSystem;
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

    private ReentrantLock getLock(int auctionId) {
        return auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock());
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
    public boolean placeBid(int userId, int auctionId, double amount) {
        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            Auction auction = auctionRepository.getActiveAuctionById(auctionId);

            if (!isValidBid(auction, amount)) {
                return false;
            }

            BidTransaction tx = createBidTransaction(userId, amount);
            boolean saved = bidRepository.saveBid(auctionId, tx); // save bid to DB

            if (saved) {
                addBid(auction, tx); // subscribe user to auction and update auction price
                publishAuctionUpdate(auction);

                // anti-sniping check upon successful bid
                extendEndTime(tx.getTimestamp(), auction);

                // delay 1s before trigger auto-bid
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.error("Failed to delay auto-bid trigger", e);
                    Thread.currentThread().interrupt();
                }
                triggerAutoBid(auction);
            }

            return saved;
        } finally {
            lock.unlock();
        }
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
    public List<BidData> getBidHistory(int auctionId) {
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
     * than or equal to the auction's current price plus its minimum increment.</p>
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

        String winner = auction.getWinner();
        if (winner == null || winner.isEmpty()) {
            return amount >= auction.getCurrentPrice();
        }

        return amount >= auction.getCurrentPrice() + auction.getMinIncrement();
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
     * winner, and transaction history through {@link Auction#updatePrice}.</p>
     *
     * @param auction auction that should receive the bid
     * @param tx accepted bid transaction
     */
    private void addBid(Auction auction, BidTransaction tx) {
        // bidder automatically becomes observer
        auction.subscribe(tx.getUserID()); // subscribe first so that client can get a notification
        auction.updatePrice(tx.getAmt(), tx);
    }

    private void publishAuctionUpdate(Auction auction) {
        updateSystem.notifyAuctionSubscribers(auction);
        updateSystem.broadcastAuctionUpdate(auction);
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

    /**
     * Enables automated bidding for a user on an active auction.
     *
     * <p>Processing: loads the active auction, stores the user's auto-bid limits as the auction's
     * single current auto-bid configuration, subscribes the user for updates, and resolves a
     * competitive auto-bid immediately when another user's configuration is already active.</p>
     *
     * @param userId ID of the user enabling auto-bidding
     * @param auctionId ID of the auction where auto-bidding should run
     * @param maxAmount highest amount the system may bid for the user
     * @param increment amount to add above the current auction price for automated bids
     */
    public void placeBidAutomated(int userId, int auctionId, double maxAmount, double increment) {
        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            Auction auction = auctionRepository.getActiveAuctionById(auctionId);
            if (auction == null) {
                logger.warn("Auto-bid failed - auction not found or not active: " + auctionId);
                return;
            }

            // Subscribe user to auction to receive price change notifications.
            auction.subscribe(userId);

            logger.info("Auto-bid request from user " + userId + " on auction " + auctionId +
                    " with maxAmount=" + maxAmount + ", increment=" + increment);

            AutoBidConfig existingConfig = auction.getCurrAutoBidConfig();
            AutoBidConfig requestedConfig = new AutoBidConfig(userId, maxAmount, increment);

            if (existingConfig == null) {
                auction.setCurrAutoBidConfig(requestedConfig);

                if (isCurrentWinner(auction, userId)) {
                    logger.info("Skipping initial auto-bid for user " + userId + " on auction " + auctionId +
                            ": user is already the current winner");
                    publishAuctionUpdate(auction);
                    return;
                }

                placeSingleAutoBid(auction, requestedConfig);
            } else if (existingConfig.getUserId() == userId) {
                auction.setCurrAutoBidConfig(requestedConfig);

                if (isCurrentWinner(auction, userId)) {
                    logger.info("Updated auto-bid config for current winner " + userId +
                            " on auction " + auctionId);
                    publishAuctionUpdate(auction);
                    return;
                }

                placeSingleAutoBid(auction, requestedConfig);
            } else {
                handleCompetitiveAutoBid(auction, existingConfig, requestedConfig);
            }

            publishAuctionUpdate(auction);
        } finally {
            lock.unlock();
        }
    }

    private void placeSingleAutoBid(Auction auction, AutoBidConfig config) {
        double newPrice = Math.min(
                config.getMaxAmount(),
                auction.getCurrentPrice() + Math.max(config.getIncrement(), auction.getMinIncrement())
        );

        if (newPrice <= auction.getCurrentPrice()) {
            if (auction.getCurrentPrice() >= config.getMaxAmount()) {
                clearAutoBidConfig(auction.getId());
            }
            return;
        }

        placeBid(config.getUserId(), auction.getId(), newPrice);

        if (Double.compare(newPrice, config.getMaxAmount()) == 0) {
            logger.info("Auto-bid reached max amount, clearing auto-bid config");
            clearAutoBidConfig(auction.getId());
        }
    }

    private void handleCompetitiveAutoBid(
            Auction auction,
            AutoBidConfig existingConfig,
            AutoBidConfig requestedConfig
    ) {
        int userIdA = existingConfig.getUserId();
        double maxAmountA = existingConfig.getMaxAmount();
        double incrementA = existingConfig.getIncrement();

        int userIdB = requestedConfig.getUserId();
        double maxAmountB = requestedConfig.getMaxAmount();
        double incrementB = requestedConfig.getIncrement();

        logger.info("Competitive auto-bid: User A=" + userIdA +
                " (max=" + maxAmountA + ", inc=" + incrementA + ") vs User B=" + userIdB +
                " (max=" + maxAmountB + ", inc=" + incrementB + ")");

        if (maxAmountA >= maxAmountB) {
            double newPrice = Math.min(maxAmountA, Math.max(
                    auction.getCurrentPrice() + auction.getMinIncrement(),
                    maxAmountB + Math.max(incrementA, auction.getMinIncrement())
            ));
            logger.info("User A keeps auto-bid config; new price=" + newPrice);
            updateCompetitiveAutoBidPrice(auction, userIdA, newPrice, maxAmountA);
            return;
        }

        auction.setCurrAutoBidConfig(requestedConfig);
        double newPrice = Math.min(maxAmountB, Math.max(
                auction.getCurrentPrice() + auction.getMinIncrement(),
                maxAmountA + Math.max(incrementB, auction.getMinIncrement())
        ));
        logger.info("User B takes over auto-bid config; new price=" + newPrice);
        updateCompetitiveAutoBidPrice(auction, userIdB, newPrice, maxAmountB);
    }

    private void updateCompetitiveAutoBidPrice(
            Auction auction,
            int winnerId,
            double newPrice,
            double winnerMaxAmount
    ) {
        if (newPrice > auction.getCurrentPrice()) {
            placeBid(winnerId, auction.getId(), newPrice);
        }

        if (Double.compare(newPrice, winnerMaxAmount) == 0 ||
                auction.getCurrentPrice() >= winnerMaxAmount) {
            logger.info("Competitive auto-bid reached winner max amount, clearing config");
            clearAutoBidConfig(auction.getId());
        }
    }

    /**
     * Stores or replaces the auto-bid configuration for an auction.
     *
     * <p>Processing: stores the user's auto-bid configuration as the single current config
     * for the auction and synchronizes the active auction object with the new configuration.</p>
     *
     * @param auctionId ID of the auction where auto-bidding is enabled
     * @param userId ID of the user enabling auto-bidding
     * @param maxAmount highest amount the system may bid for the user
     * @param increment amount to add above the current price for each automated bid
     */
    public void setAutoBidConfig(int auctionId, int userId, double maxAmount, double increment) {
        Auction auction = auctionRepository.getActiveAuctionById(auctionId);
        if (auction != null) {
            auction.setCurrAutoBidConfig(new AutoBidConfig(userId, maxAmount, increment));
        }
    }

    /**
     * Removes the auto-bid configuration from an auction.
     *
     * <p>Processing: removes the auction's configuration and refreshes the active auction's
     * auto-bid snapshot.</p>
     *
     * @param auctionId ID of the auction where auto-bidding should be cleared
     */
    public void clearAutoBidConfig(int auctionId) {
        Auction auction = auctionRepository.getActiveAuctionById(auctionId);
        if (auction != null) {
            auction.setCurrAutoBidConfig(null);
        }
    }

    /**
     * Removes the current auto-bid configuration if it belongs to the requested user.
     *
     * @param auctionId ID of the auction where auto-bidding should be cleared
     * @param userId ID of the user whose auto-bid configuration should be removed
     */
    public void clearAutoBidConfig(int auctionId, int userId) {
        AutoBidConfig config = getCurrAutoBidConfig(auctionId);
        if (config != null && config.getUserId() == userId) {
            clearAutoBidConfig(auctionId);
        }
    }

    /**
     * Checks whether a user has enabled auto-bidding on an auction.
     *
     * <p>Processing: looks up the auction's configuration and tests whether it belongs to
     * the requested user ID.</p>
     *
     * @param auctionId ID of the auction to inspect
     * @param userId ID of the user to check
     * @return {@code true} if the user currently has the auto-bid configuration on the auction
     */
    public boolean hasAutoBidConfig(int auctionId, int userId) {
        AutoBidConfig config = getCurrAutoBidConfig(auctionId);
        return config != null && config.getUserId() == userId;
    }

    /**
     * Cancels automated bidding for a user on an active auction.
     *
     * <p>Processing: loads the active auction, removes the user's auto-bid configuration if it
     * belongs to them, logs the cancellation, and notifies auction observers so connected clients
     * can refresh their state.</p>
     *
     * @param userId ID of the user canceling auto-bidding
     * @param auctionId ID of the auction where auto-bidding should be canceled
     */
    public void cancelAutoBid(int userId, int auctionId) {
        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            Auction auction = auctionRepository.getActiveAuctionById(auctionId);
            if (auction == null) {
                logger.warn("Cancel auto-bid failed - auction not found or not active: " + auctionId);
                return;
            }

            AutoBidConfig config = auction.getCurrAutoBidConfig();
            if (config != null && config.getUserId() == userId) {
                clearAutoBidConfig(auctionId);
                logger.info("Auto-bid canceled for user " + userId + " on auction " + auctionId);
                publishAuctionUpdate(auction);
            } else {
                logger.warn("Cancel auto-bid failed - user " + userId + " does not have auto-bid config on auction " + auctionId);
            }
        } finally {
            lock.unlock();
        }
    }


    private AutoBidConfig getCurrAutoBidConfig(int auctionId) {
        Auction auction = auctionRepository.getActiveAuctionById(auctionId);
        return auction == null ? null : auction.getCurrAutoBidConfig();
    }

    /**
     * Attempts to place an automatic bid after an auction price change.
     *
     * <p>Processing: reads the auction's auto-bid configuration, skips processing when none exist,
     * uses an atomic guard to avoid recursive auto-bid execution, filters out the current winner,
     * computes the next bid from the current price plus the increment, cancels the configuration
     * if the computed amount exceeds the maximum, and otherwise places the automated bid.</p>
     *
     * @param auction auction whose price change may trigger an auto-bid
     */
    public void triggerAutoBid(Auction auction) {
        int auctionId = auction.getId();
        AutoBidConfig config = auction.getCurrAutoBidConfig();
        if (config == null) {
            return;
        }

        // Prevent recursive auto-bid calls
        if (!isAutoBidding.compareAndSet(false, true)) {
            return; // another thread is already processing auto-bid
        }

        try {
            int configUserId = config.getUserId();
            double maxAmount = config.getMaxAmount();
            double increment = config.getIncrement();

            // Filter out current winner from auto-bid candidates
            if (isCurrentWinner(auction, configUserId)) {
                return;
            }

            if (auction.getCurrentPrice() >= maxAmount) {
                logger.info("Auto-bid stopped for user " + configUserId + " on auction " + auction.getId() +
                        ": Max amount (" + maxAmount + ") has reached");
                clearAutoBidConfig(auctionId);
                publishAuctionUpdate(auction);
                return;
            }

            double autoBidAmount = Math.min(maxAmount,
                    auction.getCurrentPrice() + Math.max(increment, auction.getMinIncrement()));

            // Check if auto-bid amount is higher than current price
            if (autoBidAmount <= auction.getCurrentPrice()) {
                return;
            }

            // Place the auto-bid
            placeBid(configUserId, auction.getId(), autoBidAmount);

            if (Double.compare(autoBidAmount, maxAmount) == 0) {
                logger.info("Auto-bid stopped for user " + configUserId + " on auction " + auction.getId() +
                        ": Max amount (" + maxAmount + ") has reached");
                clearAutoBidConfig(auctionId);
                publishAuctionUpdate(auction);
            }
        } finally {
            // Reset flag after bid is placed
            isAutoBidding.set(false);
        }
    }

    private boolean isCurrentWinner(Auction auction, int userId) {
        return auction.getWinnerId() != null && auction.getWinnerId().equals(userId);
    }

    private void extendEndTime(LocalDateTime bidTime, Auction auction) {
        // anti-sniping: automatically extend the duration of an auction
        // when it receives a placeBid request close to its end

        int ANTI_SNIPING_EXTENSION_SECONDS = 3600;
        int ANTI_SNIPING_THRESHOLD_SECONDS = 300;

        long secondsUntilEnd = java.time.Duration.between(bidTime, auction.getEndTime()).getSeconds();

        if (secondsUntilEnd <= ANTI_SNIPING_THRESHOLD_SECONDS && secondsUntilEnd > 0) {
            LocalDateTime newEndTime = auction.getEndTime().plusSeconds(ANTI_SNIPING_EXTENSION_SECONDS);
            auction.setEndTime(newEndTime);

            boolean updated = auctionDAO.updateEndTime(auction.getId(), newEndTime);

            if (updated) {
                publishAuctionUpdate(auction);
                logger.info("Auction " + auction.getId() + " extended due to anti-sniping. " +
                        "New end time: " + newEndTime);
            }
        }
    }
}
