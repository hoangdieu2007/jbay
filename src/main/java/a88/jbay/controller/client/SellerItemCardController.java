package a88.jbay.controller.client;

import a88.jbay.model.ImageProcessor;
import a88.jbay.model.event.Auction;
import a88.jbay.model.event.AuctionState;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import javafx.scene.image.ImageView;

public class SellerItemCardController {
    private Auction currentAuction;
    @FXML private ImageView sellerImage;
    @FXML private Label lblName;
    @FXML private Label lblStatus;
    @FXML private Label lblPrice;
    @FXML private Label lblHighestBidderName;


    // Chờ Backend truyền vào Auction
    public void setData(Auction auction) {
        currentAuction = auction;
        lblName.setText(auction.getItem().getName());
        lblPrice.setText(String.valueOf(auction.getCurrentPrice()));
        lblHighestBidderName.setText(auction.getWinner());
        updateStatusUI(auction.getAuctionState());

        byte[] imageData = auction.getItem().getImage();
        sellerImage.setImage(ImageProcessor.bytesToImage(imageData));
    }

    private void updateStatusUI(AuctionState status) {
        if (status == AuctionState.OPENING || status == AuctionState.RUNNING) {
            lblStatus.setText("Active");
            lblStatus.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-background-radius: 5; -fx-padding: 5 10 5 10;");
        } else {
            lblStatus.setText("Ended");
            lblStatus.setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #2b5fe8; -fx-background-radius: 5; -fx-padding: 5 10 5 10;");
        }
    }




}
