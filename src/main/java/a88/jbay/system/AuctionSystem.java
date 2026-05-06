package a88.jbay.system;

import a88.jbay.dao.AuctionDAO;
import a88.jbay.dao.AuctionDAO.AuctionData;
import a88.jbay.dao.BidDAO;
import a88.jbay.dao.UserDAO;
import a88.jbay.model.entity.item.Item;
import a88.jbay.model.event.Auction;
import a88.jbay.model.event.AuctionState;
import a88.jbay.model.event.BidTransaction;
import a88.jbay.model.network.Response;

import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/*
the code for operations on the auction data

features: real-time bidding, auction lifecycle management
 */

public class AuctionSystem {
    private static AuctionSystem instance;
    private final AuctionDAO auctionDAO;
    private final UserDAO userDAO;
    private final BidDAO bidDAO;
    private final UpdateSystem updateSystem;

    // Memory cache for active auctions to handle real-time bidding
    private final Map<Integer, Auction> activeAuctions;

    private final ScheduledExecutorService scheduler;

    private AuctionSystem() {
        this.auctionDAO = AuctionDAO.getInstance();
        this.userDAO = UserDAO.getInstance();
        this.bidDAO = BidDAO.getInstance();
        this.updateSystem = UpdateSystem.getInstance();
        this.activeAuctions = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        loadActiveAuctions();
        startHeartbeat();
    }

    public static synchronized AuctionSystem getInstance() {
        if (instance == null) {
            instance = new AuctionSystem();
        }
        return instance;
    }

    private void loadActiveAuctions() {
        java.util.List<AuctionData> activeAuctionData = auctionDAO.findAllActiveAuctions();
        System.out.println("Loading " + activeAuctionData.size() + " active auctions from database");

        for (AuctionData auctionData : activeAuctionData) {
            System.out.println("Loading auction: " + auctionData.id() + " - " + auctionData.item().getName() + " - State: " + auctionData.state());
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
            java.util.Set<Integer> bidders = new java.util.HashSet<>();
            
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
                System.err.println("Failed to load auction " + auctionData.id() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
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
                userDAO.findByUserId(sellerId).username(),
                start,
                end
        );
        activeAuctions.put(auctionId, auction);
        auction.subscribe(sellerId); // Seller is automatically subscribed

        //update everyone about this auction
        UpdateSystem.getInstance().updateAllUsers(
                new Response(true, "AUCTION_UPDATE", auction)
        );

        return true;
    }

    //place bid and validate bid - delegate to BidSystem
    public synchronized boolean placeBid(int userId, int auctionId, double amount) {
        boolean bidPlaced = BidSystem.getInstance().placeBid(userId, auctionId, amount);
        
        if (bidPlaced) {
            // anti-sniping check upon successful bid
            Auction auction = activeAuctions.get(auctionId);
            if (auction != null) {
                extendEndTime(LocalDateTime.now(), auction);
            }
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

        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) {
            System.out.println("Auction " + auctionId + " not found or not active.");
            return;
        }

        // Store auto-bid configuration for this user and auction
        auction.setAutoBidConfig(userId, max_amount, increment);

        // Subscribe user to auction to receive price change notifications
        auction.subscribe(userId);

        System.out.println("Auto-bid enabled for user " + userId + " on auction " + auctionId + 
                          " with max_amount=" + max_amount + ", increment=" + increment);
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
        java.util.Set<Integer> bidders = new java.util.HashSet<>();
        
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
