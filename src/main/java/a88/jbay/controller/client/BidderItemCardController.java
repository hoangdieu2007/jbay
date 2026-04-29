package a88.jbay.controller.client;

import a88.jbay.model.event.Auction;
import a88.jbay.model.event.AuctionState;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public class BidderItemCardController {

    // khai báo fx:id
    @FXML private ImageView itemImage;
    @FXML private Label descriptionLabel;
    @FXML private Label titleLabel;
    @FXML private Label statusLabel;
    @FXML private Label bidsIDLabel;
    @FXML private Label currentBidLabel;
    @FXML private Label sellerIDLabel;
    @FXML private Button bidButton;
    private long remainingSeconds;

    //Bắt đầu load UI --> chạy luôn sk
    @FXML
    // Cho vào initialize() để đảm bảo bidButton khác null
    public void initialize(){
        bidButton.setOnAction(event -> {
            // cho code chuyển trang hoặc popup vào đây
            System.out.println("Button pressed for: " + titleLabel.getText());
        });
    }

    // Hàm chờ back-end/ database truyền data vàoa
    public void setItemData(Auction auction){
        titleLabel.setText(auction.getItem().getName());
        descriptionLabel.setText(auction.getItem().getDescription());
        sellerIDLabel.setText(auction.getSellerName());
        currentBidLabel.setText(String.valueOf(auction.getCurrentPrice()) + "USD");
        bidsIDLabel.setText(String.valueOf(auction.getId()));
        remainingSeconds = java.time.Duration.between(java.time.LocalDateTime.now(), auction.getEndTime()).getSeconds();
        updateStatusUI(auction.getAuctionState());

        // Xử lý ảnh (Bọc trong try-catch phòng trường hợp link ảnh lỗi do database truyền bậy)
       /* try {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Image image = new Image(imageUrl);
                itemImage.setImage(image);
            }
        } catch (Exception e) {
            System.out.println("Lỗi load ảnh cho item: " + title);
        }*/
    }

    // Hàm phụ trợ xử lý UI linh hoạt
    private void updateStatusUI(AuctionState status) {
        if (status == AuctionState.FINISHED || status == AuctionState.CANCELED) {
            statusLabel.setText("Ended");
            statusLabel.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-background-radius: 5; -fx-padding: 5 10 5 10;");
            bidButton.setDisable(true); // Nếu kết thúc rồi thì khóa nút Bid lại, logic cơ bản.
        } else {
            startCountdown();
            statusLabel.setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #2b5fe8; -fx-background-radius: 5; -fx-padding: 5 10 5 10;");
            bidButton.setDisable(false);
        }
    }

    public void startCountdown(){
        if (remainingSeconds <= 0){
            updateStatusUI(AuctionState.FINISHED);
        }
        // run for remainingSeconds seconds; When done, automatically execute event
        //Timeline must go with KeyFrame
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(remainingSeconds), event -> {
            updateStatusUI(AuctionState.FINISHED);
            }));

        timeline.setCycleCount(1);  // run only once
        timeline.play(); // start the count

    }
}

