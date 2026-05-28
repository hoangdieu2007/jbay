package a88.jbay.system;

import a88.jbay.common.user.UserData;
import a88.jbay.data.UserRepository;
import a88.jbay.system.update.ConnectionSystem;
import a88.jbay.system.update.UpdateSystem;
import a88.jbay.util.JBayLogger;
import a88.jbay.common.item.Item;
import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.common.network.Response;
import a88.jbay.di.ApplicationContext;
import a88.jbay.data.AuctionRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/*
the code for operations on the auction data

features: real-time bidding, auction lifecycle management
 */

public class AuctionSystem {
    private final UpdateSystem updateSystem;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final JBayLogger logger;

    private final ScheduledExecutorService scheduler;

    // Constructor for dependency injection
    public AuctionSystem(
            UpdateSystem updateSystem,
            AuctionRepository auctionRepository,
            UserRepository userRepository
    ) {
        this.updateSystem = updateSystem;
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
        this.logger = JBayLogger.getLogger(AuctionSystem.class);
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        auctionRepository.loadActiveAuctions();
        startHeartbeat();
    }

    // Singleton accessor via ApplicationContext
    public static AuctionSystem getInstance() {
        return ApplicationContext.getInstance().getDependency(AuctionSystem.class);
    }

    /**
     * @deprecated
     */
    //create auction and store to database
//    public boolean createAuction(Item item, int sellerId, LocalDateTime start, LocalDateTime end) {
//        return createAuction(item, sellerId, 0.0, start, end);
//    }

    public boolean createAuction(Item item, int sellerId, double minIncrement,
                                 LocalDateTime start, LocalDateTime end) {
        logger.info("Creating auction for item: " + item.getName() + " by seller: " + sellerId);

        int auctionId = auctionRepository.insertItemAndAuction(item, sellerId, minIncrement, start, end);
        if (auctionId == -1) {
            logger.error("Failed to create auction for item: " + item.getName());
            return false;
        }

        UserData seller = userRepository.findByUserId(sellerId);
        Auction auction = new Auction(auctionId, item, seller, start, end);
        auction.setMinIncrement(minIncrement);
        auctionRepository.storeActiveAuction(auction);
        auction.subscribe(sellerId);
        updateSystem.broadcastToAll(new Response(true, "AUCTION_UPDATE", auction));

        logger.info("Auction created successfully: ID=" + auctionId + ", Item=" + item.getName());
        return true;
    }

    //place bid and validate bid - delegate to BidSystem
    public synchronized boolean placeBid(int userId, int auctionId, double amount) {
        logger.debug("Bid attempt: User=" + userId + ", Auction=" + auctionId + ", Amount=" + amount);
        boolean bidPlaced = BidSystem.getInstance().placeBid(userId, auctionId, amount);

        if (bidPlaced) {
            logger.info("Bid placed successfully: User=" + userId + ", Auction=" + auctionId + ", Amount=" + amount);
        } else {
            logger.warn("Bid failed: User=" + userId + ", Auction=" + auctionId + ", Amount=" + amount);
        }

        return bidPlaced;
    }

    public boolean confirmPayment(int auctionId) {
        Auction auction = auctionRepository.getActiveAuctionById(auctionId);
        if (auction == null) {
            logger.warn("Cancel requested for unknown auction: " + auctionId);
            return false;
        }

        boolean confirmed = auctionRepository.setAuctionState(auctionId, AuctionState.PAID);
        if (confirmed) {
            auction = auctionRepository.getAuctionById(auctionId);
            publishAuctionUpdate(auction);
            auctionRepository.removeActiveAuction(auctionId);

            updateSystem.sendToUsers(
                    Set.of(
                            auction.getWinnerId(),
                            auction.getSellerId()
                    ),
                    new Response(true, "CONFIRM_PAYMENT_SUCCESS", auction)
            );

            logger.info("Auction payment confirmed: " + auctionId);
        } else {
            logger.error("Failed to confirm payment for auction in DB: " + auctionId);
        }

        return confirmed;
    }

    //cancel auction
    //ONLY ADMIN CAN CALL THIS
    public boolean cancelAuction(int auctionId) {
        Auction auction = auctionRepository.getActiveAuctionById(auctionId);
        if (auction == null) {
            logger.warn("Cancel requested for unknown auction: " + auctionId);
            return false;
        }

        if (!(auction.getAuctionState() == AuctionState.OPENING || auction.getAuctionState() == AuctionState.RUNNING))
            return false;

        boolean canceled = auctionRepository.setAuctionState(auctionId, AuctionState.CANCELED);
        if (canceled) {
            auction = auctionRepository.getAuctionById(auctionId);
            publishAuctionUpdate(auction);
            auctionRepository.removeActiveAuction(auctionId);
            logger.info("Auction canceled: " + auctionId);
        } else {
            logger.error("Failed to cancel auction in DB: " + auctionId);
        }
        return canceled;
    }

    public boolean cancelAuctionsBySellerId(int sellerId) {
        for (Auction auction : auctionRepository.getAuctionsBySellerId(sellerId)) {
            cancelAuction(auction.getId());
        }
        return true;
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

    public void updateSellerAuctions(int userId) {
        updateSystem.sendToUser(
                userId,
                new Response(true, "SELLER_AUCTION_LIST", auctionRepository.getAuctionsBySellerId(userId))
        );
    }

    public void updateBidderAuctions(int userId) {
        updateSystem.sendToUser(
                userId,
                new Response(true, "BIDDER_AUCTION_LIST", auctionRepository.getAuctionsByWinnerId(userId))
        );
    }

    public void updateActiveAuctions(int userId) {
        updateSystem.sendToUser(
                userId,
                new Response(
                        true,
                        "ACTIVE_AUCTION_LIST",
                        auctionRepository.getActiveAuctionListExceptForSeller(userId)
                )
        );
    }

    public void updateAllAuctions(int userId) {
        updateActiveAuctions(userId);
        updateBidderAuctions(userId);
        updateSellerAuctions(userId);
    }

    public void updateAdminAuctions(int adminId) {
        updateSystem.sendToUser(
                adminId,
                new Response(true, "ADMIN_AUCTION_LIST", getAllAuctionsForAdmin())
        );
    }

    public void reloadSystem() {
        auctionRepository.loadActiveAuctions();
        //broadcast every auction to everyone
        for (Auction auction : auctionRepository.getAllActiveAuctions()) {
            updateSystem.broadcastAuctionUpdate(auction);
        }
    }

    public void unsubscribeUserFromAllAuctions(int userId) {
        for (Auction auction : auctionRepository.getAllActiveAuctions()) {
            auction.unsubscribe(userId);
        }
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

    // Lấy toàn bộ Auction phục vụ cho Admin
    public List<Auction> getAllAuctionsForAdmin() {
        // Đá qua AuctionRepository.
        // (Bên trong hàm này của repo, team bạn đã dùng DAO lấy Data và convert thành Object)
        return auctionRepository.getAllAuctionsForAdmin();
    }

    /*
    code section for handling auction state transitions
     */
    private void startHeartbeat() {
        scheduler.scheduleAtFixedRate(this::checkAuctionTransitions, 0, 1, TimeUnit.SECONDS);
    }

    private void checkAuctionTransitions() {
        List<Integer> ended = tickAuctions();
        ended.forEach(auctionRepository::removeActiveAuction);
    }

    private List<Integer> tickAuctions() {
        logger.info("Checking auction transitions...");

        LocalDateTime now = LocalDateTime.now();
        List<Integer> ended = new ArrayList<>();

        for (Auction auction : auctionRepository.getAllActiveAuctions()) {
            if (auction.tick(now)) {
                auctionRepository.setAuctionState(auction.getId(), auction.getAuctionState());
                publishAuctionUpdate(auction);
                logger.debug("State changed for auction " + auction.getId() + " to " + auction.getAuctionState());
            }
            if (auction.getAuctionState() == AuctionState.CANCELED
                    || auction.getAuctionState() == AuctionState.FINISHED) {
                ended.add(auction.getId());
                logger.info("Auction ended: " + auction.getId() + " - " + auction.getAuctionState());
            }
        }
        return ended;
    }

    private void publishAuctionUpdate(Auction auction) {
        updateSystem.notifyAuctionSubscribers(auction);
        updateSystem.broadcastAuctionUpdate(auction);
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
