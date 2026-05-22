package a88.jbay.controller.app.AuctionUI;

import a88.jbay.client.ServerConnection;
import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import a88.jbay.util.ImageProcessor;
import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.BidTransaction;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.time.format.DateTimeFormatter;


public class ViewAuctionController {
    @FXML private ImageView itemImageView;
    @FXML private TextArea itemDescription;
    @FXML private Label lblItemName, lblAuctionTime, lblBidderName,lblSellerName, lblCurrentPrice, lblMinIncrement;
    @FXML private LineChart<String, Number> priceChart;

    private XYChart.Series<String, Number> priceSeries;
    private Auction currAuction;
    private final DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private ObservableMap<Integer, Auction> dataSourceMap;
    private Runnable onGoBackAction;

    @FXML
    public void initialize() {
        setupLineChart(); // Giữ nguyên khởi tạo biểu đồ
    }

    // HÀM THIẾT LẬP DÙNG CHUNG: Độc lập hoàn toàn, không phân biệt Role
    public void setAuctionData(Auction auction, ObservableMap<Integer, Auction> map, Runnable backAction) {
        this.currAuction = auction;
        this.dataSourceMap = map;
        this.onGoBackAction = backAction;

        // 1. Đổ chữ lên UI lần đầu tiên
        updateUI(auction);

        // 2. Kích hoạt cảm biến lắng nghe Real-time dựa theo kho dữ liệu được truyền vào
        if (this.dataSourceMap != null) {
            this.dataSourceMap.addListener((MapChangeListener<Integer, Auction>) change -> {
                if (change.wasAdded() && change.getKey() == currAuction.getId()) {
                    // Khi dữ liệu của món hàng này trên mạng thay đổi -> tự vẽ lại UI
                    javafx.application.Platform.runLater(() -> updateUI(change.getValueAdded()));
                }
            });
        }
    }

    private void updateUI(Auction auction) {
        itemDescription.setText(auction.getItem().getDescription());
        lblItemName.setText(auction.getItem().getName());
        lblAuctionTime.setText(auction.getStartTime().format(displayFormatter) + " - " + auction.getEndTime().format(displayFormatter));
        lblBidderName.setText(auction.getWinner());
        lblCurrentPrice.setText(String.format("%.2f USD", auction.getCurrentPrice()));
        lblMinIncrement.setText(String.format("%.2f USD", auction.getMinIncrement()));
        lblSellerName.setText(auction.getSellerName()); // Đổ tên Seller lên màn hình công khai

        // Vẽ biểu đồ giá
        priceSeries.getData().clear();
        for (BidTransaction bid : auction.getBidHistory()) {
            String time = bid.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            priceSeries.getData().add(new XYChart.Data<>(time, bid.getAmt()));
        }

        if (priceSeries.getData().size() > 15) {
            priceSeries.getData().remove(0, priceSeries.getData().size() - 15);
        }

        // Đổ ảnh sản phẩm
        if (auction.getItem().getImage() != null && auction.getItem().getImage().length > 0) {
            itemImageView.setImage(ImageProcessor.bytesToImage(auction.getItem().getImage()));
        } else {
            itemImageView.setImage(null);
        }
    }

    private void setupLineChart() {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Bid Price Development");
        priceChart.getData().add(priceSeries);
        priceChart.setAnimated(false);
    }

    @FXML
    private void handleCancel() throws IOException {
        // Gửi lệnh hủy lên mạng
        ServerConnection.getInstance().send(new Request(RequestType.CANCEL).put("auctionId", currAuction.getId()));

        // Gọi lệnh quay xe điều hướng ra màn hình cha
        if (onGoBackAction != null) onGoBackAction.run();
    }

    @FXML
    private void handleBack() {
        // Gọi lệnh quay xe điều hướng ra màn hình cha
        if (onGoBackAction != null) onGoBackAction.run();
    }
}

