package a88.jbay.system;

import a88.jbay.system.update.ConnectionSystem;
import a88.jbay.util.JBayLogger;
import a88.jbay.common.item.Item;
import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.common.network.Response;
import a88.jbay.di.ApplicationContext;
import a88.jbay.repository.AuctionRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/*
the code for operations on the auction data

features: real-time bidding, auction lifecycle management
 */

public class AuctionSystem {
    private final ConnectionSystem connectionSystem;
    private final AuctionRepository auctionRepository;
    private final JBayLogger logger;

    private final ScheduledExecutorService scheduler;

    // Constructor for dependency injection
    public AuctionSystem(ConnectionSystem connectionSystem, AuctionRepository auctionRepository) {
        this.connectionSystem = connectionSystem;
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
        connectionSystem.broadcast(
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
        } else {
            logger.warn("Bid failed: User=" + userId + ", Auction=" + auctionId + ", Amount=" + amount);
        }

        return bidPlaced;
    }

    //cancel auction
    //ONLY ADMIN CAN CALL THIS
    public boolean cancelAuction(int auctionId) {
        Auction auction = auctionRepository.getActiveAuctionById(auctionId);
        if (auction == null) {
            logger.warn("Cancel requested for unknown auction: " + auctionId);
            return false;
        }

        auction.cancel();
        boolean canceled = auctionRepository.setAuctionState(auctionId, AuctionState.CANCELED);
        if (canceled) {
            auctionRepository.removeActiveAuction(auctionId);
            logger.info("Auction canceled: " + auctionId);
        } else {
            logger.error("Failed to cancel auction in DB: " + auctionId);
        }
        return canceled;
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
        LocalDateTime now = LocalDateTime.now();
        List<Integer> ended = new ArrayList<>();

        for (Auction auction : auctionRepository.getAllActiveAuctions()) {
            if (auction.tick(now)) {
                auctionRepository.setAuctionState(auction.getId(), auction.getAuctionState());
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
