package a88.jbay.controller.app.AuctionUI;

import a88.jbay.common.auction.Auction;
import a88.jbay.util.ImageProcessor;
import a88.jbay.view.ViewManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import java.io.IOException;

public class QRPaymentController {

    @FXML
    private Label lblSellerName;

    @FXML
    private ImageView qrImageView;

    private Auction auctionContext;

    /**
     * Hàm nạp dữ liệu từ ResponseHandler hoặc Controller khác
     * @param qrCode Mảng byte của ảnh QR nhận từ Server
     * @param sellerName Tên người bán
     * @param auction Đối tượng Auction hiện tại
     */
    public void setData(byte[] qrCode, String sellerName, Auction auction) {
        this.auctionContext = auction;

        // Hiển thị tên người bán lên Badge góc trên
        lblSellerName.setText("👤 " + sellerName);

        // Chuyển byte[] thành Image để hiển thị lên ImageView
        if (qrCode != null && qrCode.length > 0) {
            qrImageView.setImage(ImageProcessor.bytesToImage(qrCode));
        } else {
            qrImageView.setImage(null);
        }
    }

    @FXML
    private void handleClose() {
        try {
            // Load thẳng lại màn hình danh sách Ongoing Auctions của trang chủ
            ViewManager.getInstance().loadIntoMainScene("UserHomeScreenUI/won-Auctions.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
