package a88.jbay.controller.client;

import a88.jbay.client.ClientSession;
import a88.jbay.client.ServerConnection;
import a88.jbay.controller.ControllerProvider;
import a88.jbay.model.ImageProcessor;
import a88.jbay.model.event.Auction;
import a88.jbay.model.network.Request;
import a88.jbay.model.network.RequestType;
import a88.jbay.view.ViewManager;
import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ClientBidderItemController {
    @FXML
    private Label sellerLabel, auctionTimeLabel, itemNameLabel, currentPriceLabel, errorLabel;
    @FXML
    private TextField bidInput;
    @FXML
    private LineChart<String, Number> priceChart;
    @FXML
    private ImageView itemImageView;
    @FXML
    private TextArea itemDescription;

    private int currentAuctionId;
    private XYChart.Series<String, Number> priceSeries;

    //Xử lí mục ID cho auction đang hoạt động
    public void setCurrentAuctionId(int id) {
        this.currentAuctionId = id;

        // Truyền auction vào setup
        Auction auction = ClientSession.getInstance().getBidderAuctions().get(id);
        if (auction != null) {
            updateBidderUI(auction);
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

    private void updateBidderUI(Auction auction) {
        Platform.runLater(() -> {
            // Cập nhật thông tin theo realtime
            itemNameLabel.setText(auction.getItem().getName());
            sellerLabel.setText(auction.getSellerName());
            auctionTimeLabel.setText(auction.getStartTime() + " - " + auction.getEndTime());
            itemDescription.setText(auction.getItem().getDescription()); // Cập nhật Description

            // Cập nhật giá
            currentPriceLabel.setText(String.format("%.2f USD", auction.getCurrentPrice()));

            // Vẽ biểu đồ
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            priceSeries.getData().add(new XYChart.Data<>(time, auction.getCurrentPrice()));
            if (priceSeries.getData().size() > 15) priceSeries.getData().remove(0);

            // Cập nhật ảnh
            if (auction.getItem().getImage() != null) {
                itemImageView.setImage(ImageProcessor.bytesToImage(auction.getItem().getImage()));
            }
        });
    }

    private void setupAuctionListener() {
        // Lấy Map từ ClientSession
        ObservableMap<Integer, Auction> auctions = ClientSession.getInstance().getBidderAuctions();

        // Đăng ký listener
        auctions.addListener((MapChangeListener<Integer, Auction>) change -> {
            //Chỉ xử lí nếu có dữ liệu mới thêm vào hoặc cập nhật
            if (change.wasAdded()) {
                int auctionId = change.getKey();
                Auction updatedAuction = change.getValueAdded();

                //Chỉ cập nhật nếu ID trùng với món hàng đang xem
                if (auctionId == this.currentAuctionId) {
                    updateBidderUI(updatedAuction);
                }
            }
        });
    }

    @FXML
    public void initialize() {
        //Chuẩn bị biểu đồ
        setupLineChart();
        //Chuẩn bị listener để nhận thông tin
        setupAuctionListener();
    }

    //Xử lí việc nhận bid và gửi bid về cho server
    @FXML
    private void handlePlaceBid() {
        errorLabel.setVisible(false);

        String rawInput = bidInput.getText();
        if (rawInput == null || rawInput.trim().isEmpty()) {
            errorLabel.setText("Please enter a bid!");
            errorLabel.setVisible(true);
            return;
        }

        try {
            double bidAmount = Double.parseDouble(rawInput);

            // LẤY GIÁ TỪ MODEL (Không lấy từ Label để tránh lỗi chữ "USD")
            Auction currentAuction = ClientSession.getInstance().getBidderAuctions().get(currentAuctionId);

            if (currentAuction == null) {
                errorLabel.setText("Auction no longer exists!");
                errorLabel.setVisible(true);
                return;
            }

            double currentPrice = currentAuction.getCurrentPrice();

            // SO SÁNH LOGIC
            if (bidAmount <= currentPrice) {
                errorLabel.setText("Bid must be higher than " + currentPrice);
                errorLabel.setVisible(true);
                return;
            }

            // GỬI REQUEST
            Request req = new Request(RequestType.BID);
            req.put("userId", ClientSession.getInstance().getUser().getId());
            req.put("auctionId", currentAuctionId);
            req.put("amount", bidAmount);

            ServerConnection.getInstance().send(req);

            // Xóa nội dung sau khi gửi
            bidInput.clear();

        } catch (NumberFormatException e) {
            errorLabel.setText("Please enter a valid number!");
            errorLabel.setVisible(true);
        } catch (IOException e) {
            // Hiển thị thông báo lỗi kết nối
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Connection Error");
                alert.setHeaderText("Could not send bid to server");
            });
        }
    }

    @FXML
    private void handleBack() {
        // Quay về màn hình Home
        try {
            ViewManager.getInstance().displayScene("client/Seller-Bidder-HomeScreens.fxml");

            Platform.runLater(() -> {
                System.out.println("Looking for SellerBidderHomeScreenController...");
                SellerBidderHomeScreenController home = ControllerProvider.getInstance()
                        .getController(SellerBidderHomeScreenController.class);

                if (home != null) {
                    System.out.println("Found controller, selecting tab 1...");
                    home.selectTab(1);
                } else {
                    System.out.println("Controller not found!");
                }
            });
        } catch (IOException e) { e.printStackTrace(); }
    }
}
