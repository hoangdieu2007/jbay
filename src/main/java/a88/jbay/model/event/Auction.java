package a88.jbay.model.event;

import a88.jbay.model.Subject;
import a88.jbay.model.entity.item.Item;
import a88.jbay.system.UpdateSystem;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.Collections;
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
    private double startPrice;
    private double currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // realtime
    private AuctionState auctionState;
    private List<BidTransaction> bidHistory;
    private final Set<Integer> observers;

    public Auction(int id, Item item, String seller, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.item = item;
        this.seller = seller;
        this.winner = "";
        this.startPrice = item.getInitPrice();
        this.currentPrice = item.getInitPrice();
        this.startTime = startTime;
        this.endTime = endTime;

        this.auctionState = AuctionState.OPENING;
        this.bidHistory = new CopyOnWriteArrayList<>();
        this.observers = new CopyOnWriteArraySet<>();
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

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public String toString() {
        return Integer.toString(id) + " - " + item.toString() + seller + " - " + startPrice + " - " + currentPrice + " - " + winner + " - " + startTime.toString() + endTime.toString() + auctionState.name();
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
        this.bidHistory.add(tx);
        this.notifyObservers();
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
        UpdateSystem.getInstance().broadcastAuctionUpdate(this, observers);
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
