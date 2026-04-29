package a88.jbay.system;

import a88.jbay.dao.AuctionDAO;
import a88.jbay.dao.UserDAO;
import a88.jbay.model.entity.item.Item;
import a88.jbay.model.event.Auction;
import a88.jbay.model.event.AuctionState;
import a88.jbay.model.event.BidTransaction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/*
the code for operations on the auction data

features: real-time bidding, auction lifecycle management
 */

public class AuctionSystem {
    private static AuctionSystem instance;
    private final AuctionDAO auctionDAO;
    private final UserDAO userDAO;
    private final NotificationSystem notificationSystem;

    // Memory cache for active auctions to handle real-time bidding
    private final Map<Integer, Auction> activeAuctions;

    private final ScheduledExecutorService scheduler;

    private AuctionSystem() {
        this.auctionDAO = AuctionDAO.getInstance();
        this.userDAO = UserDAO.getInstance();
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
                userDAO.findByUserId(sellerId).get("username"),
                start,
                end
        );
        activeAuctions.put(auctionId, auction);
        auction.subscribe(sellerId); // Seller is automatically subscribed
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
            auction.subscribe(userId); // Bidder is automatically subscribed
            auction.updatePrice(amount, tx);

            extendEndTime(now, auction);
            return true;
        }

        return false;
    }

    public synchronized void placeBidAutomated(int userId, int auctionId, double amount, double max_amount, double increment, int intervalSeconds) {
        /*
        Phương thức dùng để tự động hoá quá trình placeBid
        Cứ cách mỗi "increment" giây, sẽ tự gọi phương thức placeBid(userId, auctionId, amount += increment) một lần
        Khi amount vượt qua max_amount, phương thức dừng
        */

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        AtomicReference<Double> amountRef = new AtomicReference<>(amount);
        AtomicReference<String> resultMessage = new AtomicReference<>();

        Callable<String> bidTask = new Callable<String>() {
            @Override
            public String call() {
                double currentAmount = amountRef.get();
                if (currentAmount > max_amount) {
                    String message = "Current placeBid (" + currentAmount + ") has exceeded max_amount (" + max_amount + "). Stop automated bidding.";
                    resultMessage.set(message);
                    scheduler.shutdown();
                    return message;
                }

                placeBid(userId, auctionId, currentAmount);
                amountRef.updateAndGet(current -> current + increment);
                return "Bid placed successfully for: " + currentAmount;
            }
        };

        scheduler.scheduleAtFixedRate(() -> {
            try {
                String message = bidTask.call();
                if (message.contains("exceeded")) {
                    System.out.println("Automated bidding stopped: " + message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, intervalSeconds, TimeUnit.SECONDS);

        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    public void extendEndTime(LocalDateTime now, Auction auction) {
        //use anti-sniping: automatically extend the duration of an auction
        //when it receives a placeBid request close to its end

        int ANTI_SNIPING_EXTENSION_SECONDS = 300;
        int ANTI_SNIPING_THRESHOLD_SECONDS = 30;

        long secondsUntilEnd = java.time.Duration.between(now, auction.getEndTime()).getSeconds();

        if (secondsUntilEnd <= ANTI_SNIPING_THRESHOLD_SECONDS && secondsUntilEnd > 0) {
            LocalDateTime newEndTime = auction.getEndTime().plusSeconds(ANTI_SNIPING_EXTENSION_SECONDS);
            auction.setEndTime(newEndTime);

            boolean updated = auctionDAO.updateEndTime(auction.getId(), newEndTime);

            if (updated) {
                // Notify all subscribers about the extension
                auction.notifyObservers();
                System.out.println("Auction " + auction.getId() + " extended due to anti-sniping. " +
                        "New end time: " + newEndTime);
            }
        }
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
            // Subscribers are automatically cleared when auction is removed from memory
        }
        return closed;
    }

    public boolean isAuctionActive(int auctionId) {
        return activeAuctions.containsKey(auctionId);
    }

    public List<Auction> getActiveAuctions() {
        return new ArrayList<>(activeAuctions.values());
    }

    public Auction getAuctionById(int auctionId) {
        return activeAuctions.get(auctionId);
    }

    public String listActiveAuctions() {
        String result = "";
        for (Auction auction : activeAuctions.values()) {
            result += auction.toString() + "\n\n";
        }
        return result;
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
            // Subscribers are automatically cleared when auction is removed from memory
        }
    }

    //stopping the heartbeat, WARNING: no automatic auction lifecycle management after stopping
    //do NOT call this method unless for testing purpose
    public void stopSystem() {
        scheduler.shutdown();
    }
}
