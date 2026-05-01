package a88.jbay.controller.client;

import a88.jbay.model.event.Auction;
import a88.jbay.model.event.AuctionState;
import a88.jbay.view.ViewManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

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
    private int currentAuctionID;

    //Bắt đầu load UI --> chạy luôn sk
    // Cho vào initialize() để đảm bảo bidButton khác null
    public void handlePlaceBid() {
        // cho code chuyển trang hoặc popup vào đây
        FXMLLoader loader = new FXMLLoader(getClass().getResource("a88/jbay/controller/client/ClientBidderItemController.java"));
        try {
            Parent root = loader.load();
            ClientBidderItemController controller = loader.getController();
            controller.setCurrentAuctionId(currentAuctionID);

            Scene scene = new Scene(root);
            Stage stage = (Stage) bidButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    // Hàm chờ back-end/ database truyền data vàoa
    public void setItemData(Auction auction){
        currentAuctionID = auction.getId();
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
            statusLabel.setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #2b5fe8; -fx-background-radius: 5; -fx-padding: 5 10 5 10;");
            bidButton.setDisable(false);
        }
    }


}

