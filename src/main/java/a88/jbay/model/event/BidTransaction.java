package a88.jbay.model.event;

import a88.jbay.model.UniqueID;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BidTransaction {
    private String id;
    private String userID;
    private double amt;
    private LocalDateTime timestamp;

    public BidTransaction(String userID, double amt, LocalDateTime timestamp) {
        this.id = UniqueID.genBID();
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

    @Override
    public String toString() {
        // Tạo định dạng: Giờ:Phút:Giây Ngày/Tháng/Năm
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
        String formattedTime = this.timestamp.format(formatter);

        // Sử dụng String.format để căn lề:
        return String.format("[ID: %-8s] | Người dùng: %-12s | Giá: %,12.2f $ | Thời gian: %s",
                id, userID, amt, formattedTime);
    }
}
