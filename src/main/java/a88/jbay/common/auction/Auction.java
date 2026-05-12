package a88.jbay.common.auction;

import a88.jbay.common.Subject;
import a88.jbay.common.item.Item;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Comparator;
import java.util.stream.Collectors;

//manage auction data, state and subscribers, all business logic belong to auction system

/**
 * the auction class, hold all auction data and state
 * manages its own subscribers and notifies them through NotificationSystem
 * it will be sent to clients via network
 * when the notifyObservers method is called, it will send the auction data to all clients subscribed to this auction
 */
public class Auction implements Subject, Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private Item item;
    private String seller;
    private String winner;
    private Integer winnerId;
    private double startPrice;
    private double currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // realtime
    private AuctionState auctionState;
    private List<BidTransaction> bidHistory;
    private final Set<Integer> observers;

    // auto-bid configuration - supports multiple users
    private final Map<Integer, AutoBidConfig> autoBidConfigs;
    private final AtomicBoolean isAutoBidding = new AtomicBoolean(false);

    public Auction(int id, Item item, String seller, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.item = item;
        this.seller = seller;
        this.winner = "";
        this.winnerId = null;
        this.startPrice = item.getInitPrice();
        this.currentPrice = item.getInitPrice();
        this.startTime = startTime;
        this.endTime = endTime;

        this.auctionState = AuctionState.OPENING;
        this.bidHistory = new CopyOnWriteArrayList<>();
        this.observers = new CopyOnWriteArraySet<>();
        this.autoBidConfigs = new HashMap<>();
    }

    public int getId() {
        return id;
    }
    public Item getItem() {
        return item;
    }
    public String getSellerName(){return seller;}

    public String getWinner() {
        return winner;
    }

    public Integer getWinnerId() {
        return winnerId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }

    public String toString() {
        return Integer.toString(id) + " - " + item.toString() + " - " + seller + " - " + startPrice + " - " + currentPrice + " - " + winner + " - " + startTime.toString() + " - " + endTime.toString() + " - " + auctionState.name();
    }

    public void start() {
        this.auctionState = AuctionState.RUNNING;
        this.notifyObservers();
    }

    public void end() {
        this.auctionState = AuctionState.FINISHED;
        this.notifyObservers();
    }

    public void cancel() {
        this.auctionState = AuctionState.CANCELED;
        this.notifyObservers();
    }

    public AuctionState getAuctionState() {
        return auctionState;
    }

    public void setAuctionState(AuctionState auctionState) {
        this.auctionState = auctionState;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void updatePrice(double newPrice, BidTransaction tx) {
        this.currentPrice = newPrice;
        this.winner = tx.getUsername();
        this.winnerId = tx.getUserID();
        this.bidHistory.add(tx);
        this.notifyObservers();

        // Trigger auto-bid if configured
        triggerAutoBid();
    }

    public void setEndTime(LocalDateTime newEndTime) {
        this.endTime = newEndTime;
    }

    //subscriber management
    public void subscribe(int userId) {
        observers.add(userId);
    }

    public void unsubscribe(int userId) {
        observers.remove(userId);
    }

    public Set<Integer> getSubscribers() {
        return Collections.unmodifiableSet(observers);
    }

    public boolean hasSubscribers() {
        return !observers.isEmpty();
    }

    public void notifyObservers() {
        System.out.println("Update: " + this);
        // Note: UpdateSystem calls will be handled by the calling AuctionSystem
    }

    //check and change state
    public boolean tick(LocalDateTime now) {
        if (auctionState == AuctionState.OPENING && now.isAfter(startTime)) {
            start();
            return true;
        }
        if (auctionState == AuctionState.RUNNING && now.isAfter(endTime)) {
            end();
            return true;
        }
        return false;
    }

    // auto-bid methods
    public void setAutoBidConfig(int userId, double maxAmount, double increment) {
        autoBidConfigs.put(userId, new AutoBidConfig(maxAmount, increment));
    }

    public void clearAutoBidConfig(int userId) {
        autoBidConfigs.remove(userId);
    }

    public Map<Integer, AutoBidConfig> getAutoBidConfigs() {
        return new HashMap<>(autoBidConfigs);
    }

    public void clearAllAutoBidConfigs() {
        autoBidConfigs.clear();
    }

    public boolean hasAutoBidConfig(int userId) {
        return autoBidConfigs.containsKey(userId);
    }

    private void triggerAutoBid() {
        if (autoBidConfigs.isEmpty()) {
            return;
        }

        // Prevent recursive auto-bid calls
        if (!isAutoBidding.compareAndSet(false, true)) {
            return; // another thread is already processing auto-bid
        }

        // Filter out current winner from auto-bid candidates
        List<Map.Entry<Integer, AutoBidConfig>> candidates = autoBidConfigs.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(winnerId))
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

        double autoBidAmount = currentPrice + winningIncrement;

        // Check if auto-bid amount exceeds max_amount
        if (autoBidAmount > winningMaxAmount) {
            System.out.println("Auto-bid stopped for user " + winningUserId + " on auction " + id +
                              ": auto-bid amount (" + autoBidAmount + ") exceeds max_amount (" + winningMaxAmount + ")");
            clearAutoBidConfig(winningUserId);
            return;
        }

        // Check if auto-bid amount is higher than current price
        if (autoBidAmount <= currentPrice) {
            return;
        }

        // Place the auto-bid through AuctionSystem
        a88.jbay.system.AuctionSystem.getInstance().placeBid(winningUserId, id, autoBidAmount);

        // Reset flag after bid is placed
        isAutoBidding.set(false);
    }

}
