package a88.jbay.model.event;

import a88.jbay.model.UniqueID;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BidTransaction {
    private int userID;
    private double amt;
    private LocalDateTime timestamp;

    public BidTransaction(int userID, double amt, LocalDateTime timestamp) {
        this.userID = userID;
        this.amt = amt;
        this.timestamp = timestamp;
    }

    public int getUserID() {
        return userID;
    }
    public double getAmt() {
        return amt;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        // Tạo định dạng: Giờ:Phút:Giây Ngày/Tháng/Năm
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
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
