package a88.jbay.common.auction;

import a88.jbay.common.item.Item;
import a88.jbay.common.user.User;
import a88.jbay.common.user.UserData;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
public class Auction implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private Item item;
    private String seller;
    private Integer sellerId;
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

    public Auction(int id, Item item, UserData seller, LocalDateTime startTime, LocalDateTime endTime) {
        if (id <= 0) {
            throw new IllegalArgumentException("Auction id must be positive");
        }
        Objects.requireNonNull(item, "item");
        requireNonBlank(seller.username(), "seller");
        Objects.requireNonNull(startTime, "startTime");
        Objects.requireNonNull(endTime, "endTime");
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("Auction end time cannot be before start time");
        }
        if (!Double.isFinite(item.getInitPrice()) || item.getInitPrice() < 0) {
            throw new IllegalArgumentException("Start price must be a non-negative finite number");
        }

        this.id = id;
        this.item = item;
        this.seller = seller.username();
        this.sellerId = seller.id();
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

    public Integer getSellerId() {return sellerId;}

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
        return Collections.unmodifiableList(bidHistory);
    }

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
        if (auctionState != AuctionState.OPENING) {
            throw new IllegalStateException("Only opening auctions can be started");
        }
        this.auctionState = AuctionState.RUNNING;
        this.notifyObservers();
    }

    public void end() {
        if (auctionState != AuctionState.RUNNING) {
            throw new IllegalStateException("Only running auctions can be ended");
        }
        this.auctionState = AuctionState.FINISHED;
        this.notifyObservers();
    }

    public void confirmPayment() {
        if (auctionState != AuctionState.FINISHED) {
            throw new IllegalStateException("Only finished auctions can be confirmed");
        }
        this.auctionState = AuctionState.PAID;
        this.notifyObservers();
    }

    public void cancel() {
        if (auctionState == AuctionState.FINISHED || auctionState == AuctionState.PAID) {
            throw new IllegalStateException("Closed auctions cannot be canceled");
        }
        this.auctionState = AuctionState.CANCELED;
        this.notifyObservers();
    }

    public AuctionState getAuctionState() {
        return auctionState;
    }

    public void setAuctionState(AuctionState auctionState) {
        this.auctionState = Objects.requireNonNull(auctionState, "auctionState");
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public double getMinIncrement() {
        return minIncrement;
    }

    public void setMinIncrement(double minIncrement) {
        if (!Double.isFinite(minIncrement) || minIncrement < 0) {
            throw new IllegalArgumentException("Minimum increment must be a non-negative finite number");
        }
        this.minIncrement = minIncrement;
    }

    public void addBid(double newPrice, BidTransaction tx) {
        validateBidTransaction(tx);
        if (!Double.isFinite(newPrice) || newPrice < 0) {
            throw new IllegalArgumentException("New price must be a non-negative finite number");
        }
        if (Double.compare(newPrice, tx.getAmt()) != 0) {
            throw new IllegalArgumentException("New price must match bid transaction amount");
        }

        this.currentPrice = newPrice;
        this.winner = tx.getUsername();
        this.winnerId = tx.getUserID();

        this.bidHistory.add(tx);
    }

    public void updatePrice(double newPrice, BidTransaction tx) {
        validateLiveBid(newPrice, tx);
        addBid(newPrice, tx);
        notifyObservers();
    }

    public void setEndTime(LocalDateTime newEndTime) {
        Objects.requireNonNull(newEndTime, "newEndTime");
        if (newEndTime.isBefore(startTime)) {
            throw new IllegalArgumentException("Auction end time cannot be before start time");
        }
        if (auctionState == AuctionState.FINISHED || auctionState == AuctionState.PAID || auctionState == AuctionState.CANCELED) {
            throw new IllegalStateException("Cannot change end time after auction is closed");
        }
        this.endTime = newEndTime;
    }

    //subscriber management
    public void subscribe(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("Subscriber id must be positive");
        }
        observers.add(userId);
    }

    public void unsubscribe(int userId) {
        if (userId <= 0) {
            return;
        }
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
        // already handled by UpdateSystem
    }

    //check and change state
    public boolean tick(LocalDateTime now) {
        Objects.requireNonNull(now, "now");
        if (auctionState == AuctionState.OPENING && !now.isBefore(startTime)) {
            start();
            return true;
        }
        if (auctionState == AuctionState.RUNNING && !now.isBefore(endTime)) {
            end();
            return true;
        }
        return false;
    }

    private void validateLiveBid(double newPrice, BidTransaction tx) {
        validateBidTransaction(tx);
        if (auctionState != AuctionState.RUNNING) {
            throw new IllegalStateException("Cannot update price unless auction is running");
        }
        double requiredPrice = winnerId == null ? currentPrice : currentPrice + minIncrement;
        if (newPrice < requiredPrice) {
            throw new IllegalArgumentException("Bid is lower than the required price");
        }
    }

    private void validateBidTransaction(BidTransaction tx) {
        Objects.requireNonNull(tx, "bid transaction");
        if (tx.getUserID() <= 0) {
            throw new IllegalArgumentException("Bidder id must be positive");
        }
        requireNonBlank(tx.getUsername(), "bidder username");
        if (!Double.isFinite(tx.getAmt()) || tx.getAmt() < 0) {
            throw new IllegalArgumentException("Bid amount must be a non-negative finite number");
        }
        Objects.requireNonNull(tx.getTimestamp(), "bid timestamp");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

}
