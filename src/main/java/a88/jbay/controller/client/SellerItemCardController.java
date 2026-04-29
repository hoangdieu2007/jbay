package a88.jbay.controller.client;

import a88.jbay.model.event.Auction;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import javax.swing.text.html.ImageView;

public class SellerItemCardController {
    @FXML
    private ImageView sellerImage;
    @FXML
    private Label lblName;
    @FXML
    private Label lblStatus;
    @FXML
    private Label lblPrice;
    @FXML
    private Label lblHighestBidderName;

    // Chờ Backend truyền vào Auction
    public void setData(Auction auction){
        lblName.setText(auction.getItem().getName());
        lblPrice.setText(String.valueOf(auction.getCurrentPrice()));
    }

}
