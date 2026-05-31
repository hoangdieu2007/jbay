package a88.jbay.controller.app.AuctionUI;

import a88.jbay.client.ClientSession;
import a88.jbay.client.ServerConnection;
import a88.jbay.common.auction.AutoBidConfig;
import a88.jbay.util.ImageProcessor;
import a88.jbay.common.auction.Auction;
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

    private static final String AUTO_BID_ENABLED_STYLE = "-fx-background-color: #4CAF50; -fx-text-fill: white;";
    private static final String AUTO_BID_DISABLED_STYLE = "-fx-background-color: #9E9E9E; -fx-text-fill: white;";
    private static final String PROMPT_AUTO_BIDDING = "Currently in auto-bidding mode";
    private static final String PROMPT_NORMAL_BID = "Enter Your Bid(USD)";
    private final DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");


    @FXML private Label sellerLabel, auctionTimeLabel, itemNameLabel, currentPriceLabel, minIncrementLabel, errorLabel, autoBidErrorLabel;
    @FXML private TextField bidInput, autoBidIncrement, autoBidMaxAmount;
    @FXML private Button placeBidButton, autoBidButton;
    @FXML private LineChart<String, Number> priceChart;
    @FXML private ImageView itemImageView;
    @FXML private TextArea itemDescription;

    private int currentAuctionId;
    private XYChart.Series<String, Number> priceSeries;
    private boolean autoBidActive = false;
    private MapChangeListener<Integer, Auction> auctionListener;

    // INITIALIZATION & LISTENERS (Khởi tạo)
    @FXML
    public void initialize() {
        // Sử dụng Helper cực gọn nhẹ
        priceSeries = a88.jbay.util.ChartHelper.setupPriceChart(priceChart, "Bid Price Development");
        setupAuctionListener();
    }

    public void setCurrentAuction(Auction auction) {
        currentAuctionId = auction.getId();
        if (auction != null) {
            updateBidderUI(auction);
        }
    }

    private void setupAuctionListener() {
        ObservableMap<Integer, Auction> auctions = ClientSession.getInstance().getBidderAuctions();
        if (auctionListener != null) {
            auctions.removeListener(auctionListener);
        }
        auctionListener = change -> {
            if (change.wasAdded() && change.getKey() == this.currentAuctionId) {
                updateBidderUI(change.getValueAdded());
            }
        };
        auctions.addListener(auctionListener);
    }

    // UI UPDATES (Cập nhật giao diện)
    private void updateBidderUI(Auction auction) {
        Platform.runLater(() -> {
            itemNameLabel.setText(auction.getItem().getName());
            sellerLabel.setText(auction.getSellerName());
            auctionTimeLabel.setText(auction.getStartTime().format(displayFormatter) + " - " + auction.getEndTime().format(displayFormatter));
            itemDescription.setText(auction.getItem().getDescription());

            currentPriceLabel.setText(String.format("%.2f USD", auction.getCurrentPrice()));
            minIncrementLabel.setText(String.format("%.2f USD", auction.getMinIncrement()));

            // Vẽ biểu đồ bằng Helper
            a88.jbay.util.ChartHelper.updatePriceChart(priceSeries, auction.getBidHistory());

            if (auction.getItem().getImage() != null) {
                itemImageView.setImage(ImageProcessor.bytesToImage(auction.getItem().getImage()));
            }

            applyAutoBidState(auction);
        });
    }

    private void applyAutoBidState(Auction auction) {
        hideAutoBidError();
        errorLabel.setVisible(false);

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
            bidInput.setPromptText(PROMPT_AUTO_BIDDING);
        } else {
            if (autoBidActive) {
                autoBidIncrement.clear();
                autoBidMaxAmount.clear();
            }
            bidInput.setPromptText(PROMPT_NORMAL_BID);
        }
        autoBidActive = enabled;
    }

    // ACTION HANDLERS
    @FXML
    private void handlePlaceBid() {
        errorLabel.setVisible(false);
        String rawInput = bidInput.getText();

        if (rawInput == null || rawInput.trim().isEmpty()) {
            showInlineError("Please enter a bid!");
            return;
        }

        try {
            double bidAmount = Double.parseDouble(rawInput.trim());
            Auction currentAuction = ClientSession.getInstance().getBidderAuctions().get(currentAuctionId);

            if (currentAuction == null) {
                showInlineError("Auction no longer exists!");
                return;
            }

            double minIncrement = currentAuction.getMinIncrement();
            double minimumBid = currentAuction.getCurrentPrice() + minIncrement;

            if (bidAmount < minimumBid && !"".equals(currentAuction.getWinner())) {
                showInlineError(String.format("Bid must be raised by at least %.2f USD", minIncrement));
                return;
            }

            ServerConnection.getInstance().send(new Request(RequestType.BID)
                    .put("userId", ClientSession.getInstance().getUser().getId())
                    .put("auctionId", currentAuctionId)
                    .put("amount", bidAmount));

            bidInput.clear();

        } catch (NumberFormatException e) {
            showInlineError("Please enter a valid number!");
        } catch (IOException e) {
            showNetworkError("Could not send bid to server");
        }
    }

    @FXML
    private void handleAutoBid() {
        hideAutoBidError();
        errorLabel.setVisible(false);

        String rawIncrement = autoBidIncrement.getText();
        String rawMaxAmount = autoBidMaxAmount.getText();

        if (rawIncrement == null || rawIncrement.trim().isEmpty()) {
            showInlineError("Please enter increment!");
            return;
        }

        if (rawMaxAmount == null || rawMaxAmount.trim().isEmpty()) {
            showInlineError("Please enter max amount!");
            return;
        }

        try {
            double increment = Double.parseDouble(rawIncrement);
            double maxAmount = Double.parseDouble(rawMaxAmount);

            if (increment <= 0) { showInlineError("Increment must be positive!"); return; }
            if (maxAmount <= 0) { showInlineError("Max amount must be positive!"); return; }

            Auction currentAuction = ClientSession.getInstance().getBidderAuctions().get(currentAuctionId);
            if (currentAuction == null) { showInlineError("Auction no longer exists!"); return; }

            double minIncrement = currentAuction.getMinIncrement();
            if (increment < minIncrement) {
                autoBidErrorLabel.setText(String.format("Bid must be raised by at least %.2f USD", minIncrement));
                autoBidErrorLabel.setManaged(true);
                autoBidErrorLabel.setVisible(true);
                return;
            }

            if (maxAmount <= currentAuction.getCurrentPrice()) {
                showInlineError("Max amount must be higher than current price!");
                return;
            }

            ServerConnection.getInstance().send(new Request(RequestType.AUTO_BID)
                    .put("userId", ClientSession.getInstance().getUser().getId())
                    .put("auctionId", currentAuctionId)
                    .put("max_amount", maxAmount)
                    .put("increment", increment));

            // Khóa UI ngay lập tức để tạo cảm giác phản hồi nhanh
            autoBidIncrement.setDisable(true);
            autoBidMaxAmount.setDisable(true);
            autoBidButton.setDisable(true);
            autoBidButton.setStyle(AUTO_BID_DISABLED_STYLE);
            bidInput.setDisable(true);
            placeBidButton.setDisable(true);
            bidInput.setPromptText(PROMPT_AUTO_BIDDING);
            autoBidActive = true;

        } catch (NumberFormatException e) {
            showInlineError("Please enter valid numbers!");
        } catch (IOException e) {
            showNetworkError("Could not send auto-bid request to server");
        }
    }

    @FXML
    private void handleCancelAutoBid() {
        hideAutoBidError();
        errorLabel.setVisible(false);

        if (ClientSession.getInstance().getBidderAuctions().get(currentAuctionId) == null) {
            showInlineError("Auction no longer exists!");
            return;
        }

        try {
            ServerConnection.getInstance().send(new Request(RequestType.CANCEL_AUTO_BID)
                    .put("userId", ClientSession.getInstance().getUser().getId())
                    .put("auctionId", currentAuctionId));

            // Mở khóa UI
            autoBidIncrement.setDisable(false);
            autoBidMaxAmount.setDisable(false);
            autoBidButton.setDisable(false);
            autoBidButton.setStyle(AUTO_BID_ENABLED_STYLE);
            autoBidIncrement.clear();
            autoBidMaxAmount.clear();
            bidInput.setDisable(false);
            placeBidButton.setDisable(false);
            bidInput.setPromptText(PROMPT_NORMAL_BID);
            autoBidActive = false;

        } catch (IOException e) {
            showNetworkError("Could not send cancel auto-bid request to server");
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

    // HELPER METHODS
    private void showInlineError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void hideAutoBidError() {
        autoBidErrorLabel.setVisible(false);
        autoBidErrorLabel.setManaged(false);
    }

    private void showNetworkError(String headerMessage) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Connection Error");
            alert.setHeaderText(headerMessage);
            alert.show();
        });
    }
}