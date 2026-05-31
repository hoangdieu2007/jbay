package a88.jbay.common.auction;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Save data of a bid to DB
public class BidTransaction implements Comparable<BidTransaction>, Serializable {
    private int userID;
    private String username;
    private double amt;
    private LocalDateTime timestamp;

    public BidTransaction(int userID, String username, double amt, LocalDateTime timestamp) {
        this.userID = userID;
        this.username = username;
        this.amt = amt;
        this.timestamp = timestamp;
    }

    public int getUserID() {
        return userID;
    }
    public String getUsername() {
        return username;
    }
    public double getAmt() {
        return amt;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        // Tạo định dạng: Giờ Ngày/Tháng/Năm
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String formattedTime = this.timestamp.format(formatter);

        // Sử dụng String.format để căn lề:
        return String.format("Người dùng: %-12s | Giá: %,12.2f $ | Thời gian: %s",
                userID, amt, formattedTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BidTransaction that)) return false;
        return userID == that.userID &&
                Double.compare(that.amt, amt) == 0 &&
                timestamp.equals(that.timestamp);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = userID;
        result = 31 * result + Double.hashCode(amt);
        result = 31 * result + timestamp.hashCode();
        return result;
    }

    @Override
    public int compareTo(BidTransaction other) {
        if (this == other) return 0;
        int cmp = Double.compare(this.amt, other.amt);
        if (cmp != 0) return cmp;
        cmp = other.timestamp.compareTo(this.timestamp);
        if (cmp != 0) return cmp;
        return Integer.compare(this.userID, other.userID);
    }
}
