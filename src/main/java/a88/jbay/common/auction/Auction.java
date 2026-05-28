package a88.jbay.common.auction;

import a88.jbay.common.item.Item;
import a88.jbay.common.user.UserData;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Holds all auction data and manages state transitions.
 * Business logic (who can bid, when auto-bid fires) belongs in AuctionSystem.
 *
 * State machine: OPENING → RUNNING → FINISHED → PAID
 *                         ↘              ↙
 *                           CANCELED
 */
public class Auction implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final Item item;
    private final String seller;
    private final int sellerId;
    private final LocalDateTime startTime;

    private volatile AuctionState auctionState;
    private volatile double currentPrice;
    private double minIncrement;
    private LocalDateTime endTime;

    private String winner;
    private Integer winnerId;

    private final List<BidTransaction> bidHistory;
    private final Set<Integer> observers;
    private AutoBidConfig currAutoBidConfig;

    public Auction(int id, Item item, UserData seller, LocalDateTime startTime, LocalDateTime endTime) {
        if (id <= 0)                           throw new IllegalArgumentException("id must be positive");
        Objects.requireNonNull(item,           "item");
        Objects.requireNonNull(seller,         "seller");
        requireNonBlank(seller.username(),     "seller.username");
        if (seller.id() <= 0)                  throw new IllegalArgumentException("seller.id must be positive");
        Objects.requireNonNull(startTime,      "startTime");
        Objects.requireNonNull(endTime,        "endTime");
        if (endTime.isBefore(startTime))       throw new IllegalArgumentException("endTime cannot be before startTime");
        requireFiniteNonNegative(item.getInitPrice(), "item.initPrice");

        this.id           = id;
        this.item         = item;
        this.seller       = seller.username();
        this.sellerId     = seller.id();
        this.startTime    = startTime;
        this.endTime      = endTime;
        this.currentPrice = item.getInitPrice();
        this.minIncrement = 0.0;
        this.winner       = "";
        this.winnerId     = null;

        this.auctionState  = AuctionState.OPENING;
        this.bidHistory    = new CopyOnWriteArrayList<>();
        this.observers     = new CopyOnWriteArraySet<>();
    }

    // -------------------------------------------------------------------------
    // State transitions — the only way state changes (except setAuctionState)
    // -------------------------------------------------------------------------

    public void start() {
        requireState(AuctionState.OPENING, "start");
        this.auctionState = AuctionState.RUNNING;
        notifyObservers();
    }

    public void end() {
        requireState(AuctionState.RUNNING, "end");
        this.auctionState = AuctionState.FINISHED;
        notifyObservers();
    }

    public void confirmPayment() {
        requireState(AuctionState.FINISHED, "confirmPayment");
        this.auctionState = AuctionState.PAID;
        notifyObservers();
    }

    public void cancel() {
        if (auctionState == AuctionState.FINISHED || auctionState == AuctionState.PAID) {
            throw new IllegalStateException("Cannot cancel a closed auction (state=" + auctionState + ")");
        }
        if (auctionState == AuctionState.CANCELED) {
            throw new IllegalStateException("Auction is already canceled");
        }
        this.auctionState = AuctionState.CANCELED;
        notifyObservers();
    }

    public AuctionState getAuctionState() {
        return auctionState;
    }

    public void setAuctionState(AuctionState auctionState) {
        this.auctionState = Objects.requireNonNull(auctionState, "auctionState");
    }

    /**
     * Advances state based on wall-clock time. Returns true if a transition occurred.
     * Call periodically from a scheduler.
     */
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

    // -------------------------------------------------------------------------
    // Bidding — single entry point, explicit success/failure
    // -------------------------------------------------------------------------

    public void placeBid(double amount, BidTransaction tx) {
        validateTxOrThrow(tx);
        if (Double.compare(amount, tx.getAmt()) != 0) {
            throw new IllegalArgumentException(
                    "amount (" + amount + ") does not match tx.amt (" + tx.getAmt() + ")");
        }
        currentPrice = amount;
        winner       = tx.getUsername();
        winnerId     = tx.getUserID();
        bidHistory.add(tx);
        notifyObservers();
    }

    public void addBid(double amount, BidTransaction tx) {
        placeBid(amount, tx);
    }

    // -------------------------------------------------------------------------
    // Subscribers
    // -------------------------------------------------------------------------

    public void subscribe(int userId) {
        if (userId <= 0) throw new IllegalArgumentException("userId must be positive");
        observers.add(userId);
    }

    public void unsubscribe(int userId) {
        if (userId > 0) observers.remove(userId);
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

    // -------------------------------------------------------------------------
    // End-time extension (allowed while not closed)
    // -------------------------------------------------------------------------

    public void setEndTime(LocalDateTime newEndTime) {
        Objects.requireNonNull(newEndTime, "newEndTime");
        if (newEndTime.isBefore(startTime)) {
            throw new IllegalArgumentException("newEndTime cannot be before startTime");
        }
        if (auctionState == AuctionState.FINISHED
                || auctionState == AuctionState.PAID
                || auctionState == AuctionState.CANCELED) {
            throw new IllegalStateException("Cannot change endTime once auction is closed");
        }
        this.endTime = newEndTime;
    }

    // -------------------------------------------------------------------------
    // Auto-bid config
    // -------------------------------------------------------------------------

    public void setCurrAutoBidConfig(AutoBidConfig config)  { this.currAutoBidConfig = config; }
    public AutoBidConfig getCurrAutoBidConfig()             { return currAutoBidConfig; }

    public boolean hasAutoBidConfig(int userId) {
        return currAutoBidConfig != null && currAutoBidConfig.getUserId() == userId;
    }

    /** Returns the config only if it belongs to userId, else null. */
    public AutoBidConfig getAutoBidConfig(int userId) {
        return hasAutoBidConfig(userId) ? currAutoBidConfig : null;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public int            getId()           { return id; }
    public Item           getItem()         { return item; }
    public String         getSellerName()   { return seller; }
    public int            getSellerId()     { return sellerId; }
    public String         getWinner()       { return winner; }
    public Integer        getWinnerId()     { return winnerId; }
    public LocalDateTime  getStartTime()    { return startTime; }
    public LocalDateTime  getEndTime()      { return endTime; }
    public double         getCurrentPrice() { return currentPrice; }
    public double         getMinIncrement() { return minIncrement; }

    public void setMinIncrement(double v) {
        requireFiniteNonNegative(v, "minIncrement");
        this.minIncrement = v;
    }

    public List<BidTransaction> getBidHistory() {
        return Collections.unmodifiableList(bidHistory);
    }

    @Override
    public String toString() {
        return String.format("Auction{id=%d, item=%s, seller=%s, price=%.2f, state=%s, end=%s}",
                id, item, seller, currentPrice, auctionState, endTime);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void requireState(AuctionState required, String operation) {
        if (auctionState != required) {
            throw new IllegalStateException(
                    operation + " requires state=" + required + " but current state=" + auctionState);
        }
    }

    private static void validateTxOrThrow(BidTransaction tx) {
        Objects.requireNonNull(tx, "tx");
        if (tx.getUserID() <= 0)                  throw new IllegalArgumentException("tx.userId must be positive");
        requireNonBlank(tx.getUsername(),          "tx.username");
        requireFiniteNonNegative(tx.getAmt(),      "tx.amt");
        Objects.requireNonNull(tx.getTimestamp(),  "tx.timestamp");
    }

    private static void requireFiniteNonNegative(double v, String field) {
        if (!Double.isFinite(v) || v < 0) {
            throw new IllegalArgumentException(
                    field + " must be a finite non-negative number, got " + v);
        }
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
