package a88.jbay.controller.app.AuctionUI;

import a88.jbay.client.ClientSession;
import a88.jbay.client.ServerConnection;
import a88.jbay.common.auction.AutoBidConfig;
import a88.jbay.util.ImageProcessor;
import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.BidTransaction;
import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import a88.jbay.view.ViewManager;
import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class ClientBidderItemController {
    @FXML
    private Label sellerLabel, auctionTimeLabel, itemNameLabel, currentPriceLabel, minIncrementLabel, errorLabel, autoBidErrorLabel;
    @FXML
    private TextField bidInput;
    @FXML
    private TextField autoBidIncrement;
    @FXML
    private TextField autoBidMaxAmount;
    @FXML
    private Button placeBidButton;
    @FXML
    private Button autoBidButton;
    @FXML
    private LineChart<String, Number> priceChart;
    @FXML
    private ImageView itemImageView;
    @FXML
    private TextArea itemDescription;

    private int currentAuctionId;
    private XYChart.Series<String, Number> priceSeries;
    private final DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private boolean autoBidActive = false;
    private static final String AUTO_BID_ENABLED_STYLE = "-fx-background-color: #4CAF50; -fx-text-fill: white;";
    private static final String AUTO_BID_DISABLED_STYLE = "-fx-background-color: #9E9E9E; -fx-text-fill: white;";

    //Xử lí mục ID cho auction đang hoạt động
    public void setCurrentAuction(Auction auction) {
        currentAuctionId = auction.getId();
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
            String startTime = auction.getStartTime().format(displayFormatter);
            String endTime = auction.getEndTime().format(displayFormatter);
            auctionTimeLabel.setText(startTime + " - " + endTime);
            itemDescription.setText(auction.getItem().getDescription());

            // Cập nhật giá
            currentPriceLabel.setText(String.format("%.2f USD", auction.getCurrentPrice()));
            minIncrementLabel.setText(String.format("%.2f USD", auction.getMinIncrement()));

            //Vẽ biểu đồ
            a88.jbay.util.ChartHelper.updatePriceChart(priceSeries, auction.getBidHistory());

            // Cập nhật ảnh
            if (auction.getItem().getImage() != null) {
                itemImageView.setImage(ImageProcessor.bytesToImage(auction.getItem().getImage()));
            }

            applyAutoBidState(auction);
        });
    }

    private void applyAutoBidState(Auction auction) {
        hideAutoBidError();

        int userId = ClientSession.getInstance().getUser().getId();
        AutoBidConfig autoBidConfig = auction.getAutoBidConfig(userId);
        boolean enabled = autoBidConfig != null;

        autoBidIncrement.setDisable(enabled);
        autoBidMaxAmount.setDisable(enabled);
        autoBidButton.setDisable(enabled);
        autoBidButton.setStyle(enabled ? AUTO_BID_DISABLED_STYLE : AUTO_BID_ENABLED_STYLE);
        bidInput.setDisable(enabled);
        placeBidButton.setDisable(enabled);

        if (enabled) {
            autoBidIncrement.setText(String.valueOf(autoBidConfig.getIncrement()));
            autoBidMaxAmount.setText(String.valueOf(autoBidConfig.getMaxAmount()));
            bidInput.setPromptText("Currently in auto-bidding mode");
        } else {
            if (autoBidActive) {
                autoBidIncrement.clear();
                autoBidMaxAmount.clear();
            }
            bidInput.setPromptText("Enter Your Bid(USD)");
        }

        autoBidActive = enabled;
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
        priceSeries = a88.jbay.util.ChartHelper.setupPriceChart(priceChart, "Bid Price Development");
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
            double bidAmount = Double.parseDouble(rawInput.trim());

            // LẤY GIÁ TỪ MODEL (Không lấy từ Label để tránh lỗi chữ "USD")
            Auction currentAuction = ClientSession.getInstance().getBidderAuctions().get(currentAuctionId);

            if (currentAuction == null) {
                errorLabel.setText("Auction no longer exists!");
                errorLabel.setVisible(true);
                return;
            }

            double currentPrice = currentAuction.getCurrentPrice();
            double minIncrement = currentAuction.getMinIncrement();
            double minimumBid = currentPrice + minIncrement;

            // SO SÁNH LOGIC
            if (bidAmount < minimumBid && !"".equals(currentAuction.getWinner())) {
                errorLabel.setText(String.format("Bid must be raised by at least %.2f USD", minIncrement));
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
        try {
            ViewManager.getInstance().loadIntoMainScene("UserHomeScreenUI/ongoing-Auctions.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAutoBid() {
        errorLabel.setVisible(false);
        hideAutoBidError();

        String rawIncrement = autoBidIncrement.getText();
        String rawMaxAmount = autoBidMaxAmount.getText();

        if (rawIncrement == null || rawIncrement.trim().isEmpty()) {
            errorLabel.setText("Please enter increment!");
            errorLabel.setVisible(true);
            return;
        }

        if (rawMaxAmount == null || rawMaxAmount.trim().isEmpty()) {
            errorLabel.setText("Please enter max amount!");
            errorLabel.setVisible(true);
            return;
        }

        try {
            double increment = Double.parseDouble(rawIncrement);
            double maxAmount = Double.parseDouble(rawMaxAmount);

            if (increment <= 0) {
                errorLabel.setText("Increment must be positive!");
                errorLabel.setVisible(true);
                return;
            }

            if (maxAmount <= 0) {
                errorLabel.setText("Max amount must be positive!");
                errorLabel.setVisible(true);
                return;
            }

            Auction currentAuction = ClientSession.getInstance().getBidderAuctions().get(currentAuctionId);

            if (currentAuction == null) {
                errorLabel.setText("Auction no longer exists!");
                errorLabel.setVisible(true);
                return;
            }

            double minIncrement = currentAuction.getMinIncrement();
            if (increment < minIncrement) {
                autoBidErrorLabel.setText(String.format("Bid must be raised by at least %.2f USD", minIncrement));
                autoBidErrorLabel.setManaged(true);
                autoBidErrorLabel.setVisible(true);
                return;
            }

            double currentPrice = currentAuction.getCurrentPrice();

            if (maxAmount <= currentPrice) {
                errorLabel.setText("Max amount must be higher than current price!");
                errorLabel.setVisible(true);
                return;
            }

            Request req = new Request(RequestType.AUTO_BID);
            req.put("userId", ClientSession.getInstance().getUser().getId());
            req.put("auctionId", currentAuctionId);
            req.put("max_amount", maxAmount);
            req.put("increment", increment);

            ServerConnection.getInstance().send(req);

            // Keep values in text boxes and disable them
            autoBidIncrement.setDisable(true);
            autoBidMaxAmount.setDisable(true);
            autoBidButton.setDisable(true);
            autoBidButton.setStyle(AUTO_BID_DISABLED_STYLE);
            // Also disable bid input and place bid button
            bidInput.setDisable(true);
            placeBidButton.setDisable(true);
            bidInput.setPromptText("Currently in auto-bidding mode");
            autoBidActive = true;

        } catch (NumberFormatException e) {
            errorLabel.setText("Please enter valid numbers!");
            errorLabel.setVisible(true);
        } catch (IOException e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Connection Error");
                alert.setHeaderText("Could not send auto-bid request to server");
            });
        }
    }

    @FXML
    private void handleCancelAutoBid() {
        errorLabel.setVisible(false);
        hideAutoBidError();

        Auction currentAuction = ClientSession.getInstance().getBidderAuctions().get(currentAuctionId);

        if (currentAuction == null) {
            errorLabel.setText("Auction no longer exists!");
            errorLabel.setVisible(true);
            return;
        }

        try {
            Request req = new Request(RequestType.CANCEL_AUTO_BID);
            req.put("userId", ClientSession.getInstance().getUser().getId());
            req.put("auctionId", currentAuctionId);

            ServerConnection.getInstance().send(req);

            // Re-enable text fields and clear values
            autoBidIncrement.setDisable(false);
            autoBidMaxAmount.setDisable(false);
            autoBidButton.setDisable(false);
            autoBidButton.setStyle(AUTO_BID_ENABLED_STYLE);
            autoBidIncrement.clear();
            autoBidMaxAmount.clear();
            // Also re-enable bid input and place bid button
            bidInput.setDisable(false);
            placeBidButton.setDisable(false);
            bidInput.setPromptText("Enter Your Bid(USD)");
            autoBidActive = false;

        } catch (IOException e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Connection Error");
                alert.setHeaderText("Could not send cancel auto-bid request to server");
            });
        }
    }

    private void hideAutoBidError() {
        autoBidErrorLabel.setVisible(false);
        autoBidErrorLabel.setManaged(false);
    }
}
