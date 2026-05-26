package a88.jbay.controller.app.AuctionUI;

import a88.jbay.common.auction.Auction;
import a88.jbay.controller.ControllerProvider;
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

    // Biến tạm để nhớ món hàng cần quay về
    private Auction auctionContext;

    /**
     * Hàm nạp dữ liệu từ ResponseHandler hoặc Controller khác
     * @param qrCode Mảng byte của ảnh QR nhận từ Server
     * @param sellerName Tên người bán
     * @param auction Đối tượng Auction hiện tại để tí nữa quay lại
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
            // 1. Load lại màn hình chi tiết món hàng dành cho người mua (Bidder)
            ViewManager.getInstance().loadIntoMainScene("AuctionUI/client-bidder-item-view.fxml");

            // 2. Lấy Controller vừa được khởi tạo ra
            ClientBidderItemController controller = ControllerProvider.getInstance()
                    .getController(ClientBidderItemController.class);

            // 3. Đổ lại dữ liệu cũ vào để người dùng tiếp tục xem món hàng đó
            if (controller != null && auctionContext != null) {
                controller.setCurrentAuction(auctionContext);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
