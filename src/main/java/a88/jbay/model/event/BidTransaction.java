package a88.jbay.model.event;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BidTransaction implements Serializable {
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

    public int compareTo(BidTransaction other) {
        if  (this.amt < other.amt) {
            return -1;
        }
        else if (this.amt > other.amt) {
            return 1;
        }
        else if (this.timestamp.isBefore(other.timestamp)) {
            return 1;
        }
        return -1;
    }
}
