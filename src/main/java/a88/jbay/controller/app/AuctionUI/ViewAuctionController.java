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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class ViewAuctionController {
    @FXML private ImageView itemImageView;
    @FXML private TextArea itemDescription;
    @FXML private Label lblItemName, lblAuctionTime, lblBidderName, lblSellerName, lblCurrentPrice, lblMinIncrement;
    @FXML private LineChart<String, Number> priceChart;
    @FXML private Button actionButton;

    private XYChart.Series<String, Number> priceSeries;
    private Auction currAuction;
    private final DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private ObservableMap<Integer, Auction> dataSourceMap;
    private Runnable onGoBackAction;

    private boolean canCancel;
    private boolean canConfirm;

    @FXML
    public void initialize() {
        priceSeries = a88.jbay.util.ChartHelper.setupPriceChart(priceChart, "Bid Price Development");
    }

    public void setAuctionData(Auction auction, ObservableMap<Integer, Auction> map,
                               boolean canCancel, boolean canConfirm, Runnable backAction) {
        this.currAuction = auction;
        this.dataSourceMap = map;
        this.canCancel = canCancel;
        this.canConfirm = canConfirm;
        this.onGoBackAction = backAction;

        updateUI(auction);

        if (this.dataSourceMap != null) {
            this.dataSourceMap.addListener((MapChangeListener<Integer, Auction>) change -> {
                if (change.wasAdded() && change.getKey() == currAuction.getId()) {
                    javafx.application.Platform.runLater(() -> updateUI(change.getValueAdded()));
                }
            });
        }
    }

    private void updateUI(Auction auction) {
        this.currAuction = auction;

        itemDescription.setText(auction.getItem().getDescription());
        lblItemName.setText(auction.getItem().getName());
        lblAuctionTime.setText(auction.getStartTime().format(displayFormatter) + " - " + auction.getEndTime().format(displayFormatter));
        lblBidderName.setText(auction.getWinner());
        lblCurrentPrice.setText(String.format("%.2f USD", auction.getCurrentPrice()));
        lblMinIncrement.setText(String.format("%.2f USD", auction.getMinIncrement()));
        lblSellerName.setText(auction.getSellerName());

        a88.jbay.util.ChartHelper.updatePriceChart(priceSeries, auction.getBidHistory());

        if (auction.getItem().getImage() != null && auction.getItem().getImage().length > 0) {
            itemImageView.setImage(ImageProcessor.bytesToImage(auction.getItem().getImage()));
        } else {
            itemImageView.setImage(null);
        }

        if (actionButton != null) {
            actionButton.setVisible(true);

            switch (auction.getAuctionState()) {
                case OPENING:
                case RUNNING:
                    if (this.canCancel) {
                        actionButton.setText("End Auction");
                        actionButton.setDisable(false);
                        actionButton.getStyleClass().setAll("button", "btn-cancel");
                    } else {
                        actionButton.setVisible(false); // Ẩn luôn nếu không có quyền hủy
                    }
                    break;

                case FINISHED:
                    if (this.canConfirm) {
                        actionButton.setText("Confirm Payment");
                        actionButton.setDisable(false);
                        actionButton.getStyleClass().setAll("button", "btn-confirm");
                    } else {
                        actionButton.setText("WAITING PAYMENT");
                        actionButton.setDisable(true);
                        actionButton.getStyleClass().setAll("button", "btn-disabled");
                    }
                    break;

                case PAID:
                    actionButton.setText("PAID");
                    actionButton.setDisable(true);
                    actionButton.getStyleClass().setAll("button", "btn-disabled");
                    break;

                case CANCELED:
                    actionButton.setText("CANCELED");
                    actionButton.setDisable(true);
                    actionButton.getStyleClass().setAll("button", "btn-disabled");
                    break;
            }
        }
    }

    @FXML
    private void handleActionButton() throws IOException {
        if (currAuction == null) return;

        if ((currAuction.getAuctionState() == a88.jbay.common.auction.AuctionState.RUNNING ||
                currAuction.getAuctionState() == a88.jbay.common.auction.AuctionState.OPENING) && this.canCancel) {

            ServerConnection.getInstance().send(new Request(RequestType.CANCEL).put("auctionId", currAuction.getId()));
            if (onGoBackAction != null) onGoBackAction.run();

        } else if (currAuction.getAuctionState() == a88.jbay.common.auction.AuctionState.FINISHED && this.canConfirm) {

            ServerConnection.getInstance().send(new Request(RequestType.CONFIRM_PAYMENT).put("auctionId", currAuction.getId()));
        }
    }

    private void setupLineChart() {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Bid Price Development");
        priceChart.getData().add(priceSeries);
        priceChart.setAnimated(false);
    }

    @FXML
    private void handleBack() {
        if (onGoBackAction != null) onGoBackAction.run();
    }
}