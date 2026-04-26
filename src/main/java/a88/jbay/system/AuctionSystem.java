package a88.jbay.system;

import a88.jbay.dao.AuctionDAO;
import a88.jbay.model.entity.item.Item;
import a88.jbay.model.event.Auction;
import a88.jbay.model.event.AuctionState;
import a88.jbay.model.event.BidTransaction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/*
the code for operations on the auction data

features: real-time bidding, auction lifecycle management
 */

public class AuctionSystem {
    private static AuctionSystem instance;
    private final AuctionDAO auctionDAO;
    private final NotificationSystem notificationSystem;

    // Memory cache for active auctions to handle real-time bidding
    private final Map<Integer, Auction> activeAuctions;

    private final ScheduledExecutorService scheduler;

    private AuctionSystem() {
        this.auctionDAO = AuctionDAO.getInstance();
        this.notificationSystem = NotificationSystem.getInstance();
        this.activeAuctions = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        startHeartbeat();
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
        if (auctionId == -1) return false;

        Auction auction = new Auction(
                auctionId,
                item,
                sellerId,
                start,
                end
        );
        auction.registerObserver(notificationSystem);
        activeAuctions.put(auctionId, auction);
        notificationSystem.subscribe(sellerId, auctionId);
        return true;
    }

    //place bid and validate bid
    public synchronized boolean placeBid(int userId, int auctionId, double amount) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) return false;

        if (amount <= auction.getCurrentPrice()) {
            return false;
        }

        if (!(auction.getAuctionState() == AuctionState.RUNNING)) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        boolean bidInserted = auctionDAO.insertBid(userId, auctionId, amount, now);

        if (bidInserted && auctionDAO.updateCurrentPrice(auctionId, amount, userId)) {
            // SYNC MEMORY: Update the object and trigger observers
            BidTransaction tx = new BidTransaction(userId, amount, now);
            auction.updatePrice(amount, tx);
            notificationSystem.subscribe(userId, auctionId);
            return true;
        }

        return false;
    }

    //cancel auction
    //ONLY ADMIN CAN CALL THIS
    public boolean cancelAuction(int auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction != null) {
            auction.cancel();
        }
        boolean closed = auctionDAO.setAuctionState(auctionId, AuctionState.CANCELED);
        if (closed) {
            activeAuctions.remove(auctionId);
            notificationSystem.clearAuctionSubscribers(auctionId);
        }
        return closed;
    }

    public boolean isAuctionActive(int auctionId) {
        return activeAuctions.containsKey(auctionId);
    }

    public List<Auction> getActiveAuctions() {
        return new ArrayList<>(activeAuctions.values());
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

        for (Auction auction : activeAuctions.values()) {
            // check and change state
            if (auction.tick(now)) {
                System.out.println("Heartbeat: State changed for auction " + auction.getId());
                auctionDAO.setAuctionState(auction.getId(), auction.getAuctionState());
            }

            // Add canceled/finsihed auctions to ended list to remove
            if (auction.getAuctionState() == AuctionState.CANCELED || auction.getAuctionState() == AuctionState.FINISHED) {
                ended.add(auction.getId());
            }
        }

        //remove ended auctions from memory
        for (int auctionId : ended) {
            activeAuctions.remove(auctionId);
            notificationSystem.clearAuctionSubscribers(auctionId);
        }
    }

    //stopping the heartbeat, WARNING: no automatic auction lifecycle management after stopping
    //do NOT call this method unless for testing purpose
    public void stopSystem() {
        scheduler.shutdown();
    }
}
