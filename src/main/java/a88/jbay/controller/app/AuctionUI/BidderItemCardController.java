package a88.jbay.controller.app.AuctionUI;

import a88.jbay.client.ClientSession;
import a88.jbay.client.ServerConnection;
import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import a88.jbay.controller.ControllerProvider;
import a88.jbay.util.ImageProcessor;
import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.view.ViewManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

import java.io.IOException;

public class BidderItemCardController {

    // khai báo fx:id
    @FXML private ImageView itemImage;
    @FXML private Label descriptionLabel;
    @FXML private Label titleLabel;
    @FXML private Label statusLabel;
    @FXML private Label lblTopBidder;
    @FXML private Label currentBidLabel;
    @FXML private Label sellerIDLabel;
    @FXML private Button bidButton;
    private long remainingSeconds;
    private Auction currentAuction;

    //Bắt đầu load UI --> chạy luôn sk

    // --- HÀM XỬ LÝ CLICK CHUỘT THÔNG MINH ---
    @FXML
    private void handlePlaceBid() {
        if (currentAuction == null) return;

        try {
            if (currentAuction.getAuctionState() == AuctionState.RUNNING ||
                    currentAuction.getAuctionState() == AuctionState.OPENING) {

                // LUỒNG 1: Phiên đang chạy -> Vào màn hình chi tiết để đặt giá
                ViewManager.getInstance().loadIntoMainScene("AuctionUI/client-bidder-item-view.fxml");
                ControllerProvider.getInstance().getController(ClientBidderItemController.class).setCurrentAuction(currentAuction);

            } else if (currentAuction.getAuctionState() == AuctionState.FINISHED) {

                // LUỒNG 2: Hết giờ & Nút đang mở (Tức là Winner bấm) -> Xin mã QR thẳng luôn
                ServerConnection.getInstance().send(
                        new Request(RequestType.PAY).put("auctionId", currentAuction.getId())
                );

                // Gói tin sẽ được ResponseHandler bắt và tự động load trang QR!
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    // Hàm chờ back-end/ database truyền data vàoa
    public void setItemData(Auction auction){
        currentAuction = auction;
        titleLabel.setText(auction.getItem().getName());
        descriptionLabel.setText(auction.getItem().getDescription());
        sellerIDLabel.setText(auction.getSellerName());
        currentBidLabel.setText(String.valueOf(auction.getCurrentPrice()) + "USD");
        lblTopBidder.setText(auction.getWinner());
        remainingSeconds = java.time.Duration.between(java.time.LocalDateTime.now(), auction.getEndTime()).getSeconds();

        updateStatusUI(auction);

        byte[] imageData = auction.getItem().getImage();
        itemImage.setImage(ImageProcessor.bytesToImage(imageData));

    }

    // --- HÀM PHỤ TRỢ XỬ LÝ UI LINH HOẠT ---
    private void updateStatusUI(Auction auction) {
        AuctionState status = auction.getAuctionState();

        // Lấy tên người dùng hiện tại để so sánh xem có phải Winner không
        String myUsername = ClientSession.getInstance().getUser().getUsername();
        boolean amIWinner = myUsername.equals(auction.getWinner());

        // 1. Cập nhật cái Nhãn trạng thái (Badge ở góc trên)
        if (status == AuctionState.OPENING || status == AuctionState.RUNNING) {
            statusLabel.setText("Active");
            statusLabel.setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #2b5fe8; -fx-background-radius: 5; -fx-padding: 5 10 5 10;");
        } else {
            statusLabel.setText("Ended");
            statusLabel.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-background-radius: 5; -fx-padding: 5 10 5 10;");
        }

        // 2. BIẾN HÌNH NÚT BẤM BÊN DƯỚI CARD
        if (bidButton != null) {
            switch (status) {
                case OPENING:
                case RUNNING:
                    // Đang đấu giá: Hiện nút Place Bid xanh dương
                    bidButton.setText("Place Bid");
                    bidButton.setDisable(false);
                    bidButton.getStyleClass().setAll("button", "bid-button");
                    break;

                case FINISHED:
                    if (amIWinner) {
                        // MÌNH THẮNG: Nút Pay Now màu xanh lá nhảy ra kêu gọi hành động
                        bidButton.setText("Pay Now");
                        bidButton.setDisable(false);
                        bidButton.getStyleClass().setAll("button", "btn-confirm");
                    } else {
                        // NGƯỜI KHÁC THẮNG: Khóa nút, biến thành màu xám nhạt nhòa
                        bidButton.setText("Ended");
                        bidButton.setDisable(true);
                        bidButton.getStyleClass().setAll("button", "btn-disabled");
                    }
                    break;

                case PAID:
                    bidButton.setText("PAID");
                    bidButton.setDisable(true);
                    bidButton.getStyleClass().setAll("button", "btn-disabled");
                    break;

                case CANCELED:
                    bidButton.setText("CANCELED");
                    bidButton.setDisable(true);
                    bidButton.getStyleClass().setAll("button", "btn-disabled");
                    break;
            }
        }
    }
}

