package a88.jbay.model.event;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

public class BidTransaction {
    private String id;
    private String userID;
    private double amt;
    private LocalDateTime timestamp;

    public BidTransaction(String id, String userID, double amt, LocalDateTime timestamp) {
        this.id = id;
        this.userID = userID;
        this.amt = amt;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }
    public String getUserID() {
        return userID;
    }
    public double getAmt() {
        return amt;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int compareTo(BidTransaction bidTransaction) {
        if (this.amt < bidTransaction.amt) return -1;
        if (this.amt > bidTransaction.amt) return 1;
        if (this.timestamp.isBefore(bidTransaction.timestamp)) return -1;
        if (this.timestamp.isAfter(bidTransaction.timestamp)) return 1;
        return 1;
    }
}
