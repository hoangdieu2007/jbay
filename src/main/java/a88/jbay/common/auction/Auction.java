package a88.jbay.common.auction;

import a88.jbay.common.Subject;
import a88.jbay.common.item.Item;
import a88.jbay.system.BidSystem;
import a88.jbay.system.update.UpdateSystem;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
//import java.util.HashMap;
import java.util.List;
//import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

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
    private double minIncrement;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // realtime
    private AuctionState auctionState;
    private List<BidTransaction> bidHistory;
    private final Set<Integer> observers;
    private AutoBidConfig currAutoBidConfig;

    public Auction(int id, Item item, String seller, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.item = item;
        this.seller = seller;
        this.winner = "";
        this.winnerId = null;
        this.startPrice = item.getInitPrice();
        this.currentPrice = item.getInitPrice();
        this.minIncrement = 0.0;
        this.startTime = startTime;
        this.endTime = endTime;

        this.auctionState = AuctionState.OPENING;
        this.bidHistory = new CopyOnWriteArrayList<>();
        this.observers = new CopyOnWriteArraySet<>();
        this.currAutoBidConfig = null;
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

    /**
     * @deprecated Use {@link #getCurrAutoBidConfig()} instead.
     */
//    public Map<Integer, AutoBidConfig> getAutoBidConfigs() {
//        Map<Integer, AutoBidConfig> autoBidConfigs = new HashMap<>();
//        if (currAutoBidConfig != null) {
//            autoBidConfigs.put(currAutoBidConfig.getUserId(), currAutoBidConfig);
//        }
//        return autoBidConfigs;
//    }

    /**
     * @deprecated Use {@link #setCurrAutoBidConfig(AutoBidConfig)} instead.
     */
//    public void setAutoBidConfigs(Map<Integer, AutoBidConfig> autoBidConfigs) {
//        this.currAutoBidConfig = null;
//        if (autoBidConfigs != null && !autoBidConfigs.isEmpty()) {
//            this.currAutoBidConfig = autoBidConfigs.values().iterator().next();
//        }
//    }

    public AutoBidConfig getCurrAutoBidConfig() {
        return currAutoBidConfig;
    }

    public void setCurrAutoBidConfig(AutoBidConfig currAutoBidConfig) {
        this.currAutoBidConfig = currAutoBidConfig;
    }

    public boolean hasAutoBidConfig(int userId) {
        return currAutoBidConfig != null && currAutoBidConfig.getUserId() == userId;
    }

    public AutoBidConfig getAutoBidConfig(int userId) {
        if (!hasAutoBidConfig(userId)) {
            return null;
        }
        return currAutoBidConfig;
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

    public double getMinIncrement() {
        return minIncrement;
    }

    public void setMinIncrement(double minIncrement) {
        this.minIncrement = minIncrement;
    }

    public void addBid(double newPrice, BidTransaction tx) {
        this.currentPrice = newPrice;
        this.winner = tx.getUsername();
        this.winnerId = tx.getUserID();

        this.bidHistory.add(tx);
    }

    public void updatePrice(double newPrice, BidTransaction tx) {
        addBid(newPrice, tx);
        notifyObservers();
        BidSystem.getInstance().triggerAutoBid(this);
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
        UpdateSystem.getInstance().notifyAuctionSubscribers(this);
        UpdateSystem.getInstance().broadcastAuctionUpdate(this);
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

}
