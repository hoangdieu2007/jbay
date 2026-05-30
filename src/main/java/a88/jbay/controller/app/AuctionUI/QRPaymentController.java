package a88.jbay.controller.app.AuctionUI;

import a88.jbay.common.auction.Auction;
import a88.jbay.util.ImageProcessor;
import a88.jbay.view.ViewManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import java.io.IOException;

public class QRPaymentController {

    private static final String FXML_WON_AUCTIONS = "UserHomeScreenUI/won-Auctions.fxml";

    @FXML private Label lblSellerName;
    @FXML private ImageView qrImageView;

    private Auction auctionContext;

    /**
     * Hàm nạp dữ liệu từ ResponseHandler hoặc Controller khác
     */
    public void setData(byte[] qrCode, String sellerName, Auction auction) {
        this.auctionContext = auction;

        lblSellerName.setText("👤 Seller Name: " + sellerName);

        if (qrCode != null && qrCode.length > 0) {
            qrImageView.setImage(ImageProcessor.bytesToImage(qrCode));
        } else {
            qrImageView.setImage(null);
        }
    }

    @FXML
    private void handleClose() {
        try {
            ViewManager.getInstance().loadIntoMainScene(FXML_WON_AUCTIONS);
        } catch (IOException e) {
            System.err.println("Error loading won auctions view: " + e.getMessage());
        }
    }
}