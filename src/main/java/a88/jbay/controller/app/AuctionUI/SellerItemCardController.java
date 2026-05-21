package a88.jbay.controller.app.AuctionUI;

import a88.jbay.controller.ControllerProvider;
import a88.jbay.util.ImageProcessor;
import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.view.ViewManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import javafx.scene.image.ImageView;

import java.io.IOException;

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
        lblPrice.setText(String.valueOf(auction.getCurrentPrice()) + "USD");
        lblHighestBidderName.setText(auction.getWinner());
        updateStatusUI(auction.getAuctionState());

        byte[] imageData = auction.getItem().getImage();
        sellerImage.setImage(ImageProcessor.bytesToImage(imageData));
    }

    private void updateStatusUI(AuctionState status) {
        if (status == AuctionState.OPENING || status == AuctionState.RUNNING) {
            lblStatus.setText("Active");
            lblStatus.setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #2b5fe8; -fx-background-radius: 5; -fx-padding: 5 10 5 10;");
        } else {
            lblStatus.setText("Ended");
            lblStatus.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-background-radius: 5; -fx-padding: 5 10 5 10;");
        }
    }


    @FXML
    private void handleView(){
        try {
            ViewManager.getInstance().loadIntoMainScene("AuctionUI/seller-viewAuction-view.fxml");
            ControllerProvider.getInstance().getController(SellerViewAuctionController.class).setSellerViewData(currentAuction);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }




}
