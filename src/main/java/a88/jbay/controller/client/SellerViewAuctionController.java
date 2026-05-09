package a88.jbay.controller.client;

import a88.jbay.client.ClientSession;
import a88.jbay.client.ServerConnection;
import a88.jbay.util.ImageProcessor;
import a88.jbay.model.event.Auction;
import a88.jbay.model.event.BidTransaction;
import a88.jbay.view.ViewManager;
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


public class SellerViewAuctionController {
    @FXML private ImageView itemImageView;
    @FXML private TextArea itemDescription;
    @FXML private Label lblItemName, lblAuctionTime, lblBidderName, lblCurrentPrice;
    @FXML private LineChart<String, Number> priceChart;

    private XYChart.Series<String, Number> priceSeries;
    private Auction currAuction;


    public void setSellerViewData(Auction auction){
        currAuction = auction;
        itemDescription.setText(auction.getItem().getDescription());
        lblItemName.setText(auction.getItem().getName());
        lblAuctionTime.setText(auction.getStartTime() + " - " + auction.getEndTime());
        lblBidderName.setText(auction.getWinner());
        lblCurrentPrice.setText(String.format("%.2f USD", auction.getCurrentPrice()));

        // Vẽ biểu đồ
        for (BidTransaction bid : auction.getBidHistory()) {
            String time = bid.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            priceSeries.getData().add(new XYChart.Data<>(time, bid.getAmt()));
            if (priceSeries.getData().size() > 15) priceSeries.getData().remove(0);
        }

        // Cập nhật ảnh
        if (auction.getItem().getImage() != null) {
            itemImageView.setImage(ImageProcessor.bytesToImage(auction.getItem().getImage()));
        }

    }
    private void setupLineChart() {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Bid Price Development");

        //Gắn series vào biểu đồ đã khai báo trong FXML
        priceChart.getData().add(priceSeries);

        //Tắt animation để biểu đồ cập nhật mượt mà hơn
        priceChart.setAnimated(false);
    }

    private void setupSellerViewListener() {
        // Lấy Map từ ClientSession
        ObservableMap<Integer, Auction> auctions = ClientSession.getInstance().getBidderAuctions();

        // Đăng ký listener
        auctions.addListener((MapChangeListener<Integer, Auction>) change -> {
            //Chỉ xử lí nếu có dữ liệu mới thêm vào hoặc cập nhật
            if (change.wasAdded()) {
                int auctionId = change.getKey();
                Auction updatedAuction = change.getValueAdded();

                //Chỉ cập nhật nếu ID trùng với món hàng đang xem
                if (auctionId == currAuction.getId() ) {
                    setSellerViewData(updatedAuction);
                }
            }
        });
    }

    @FXML
    public void initialize(){
        setupLineChart();
        setupSellerViewListener();
    }

    @FXML
    private void handleCancel() {
        currAuction.cancel();
    }

    @FXML
    private void handleBack(){
        // Chỉ định tab cần mở khi quay về là Bidder
        SellerBidderHomeScreenController.targetTabIndex = 0;

        try {
            ViewManager.displayScene("client/Seller-Bidder-HomeScreens.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

