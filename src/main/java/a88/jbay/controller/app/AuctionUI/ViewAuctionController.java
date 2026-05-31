package a88.jbay.controller.app.AuctionUI;

import a88.jbay.client.ServerConnection;
import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import a88.jbay.util.ImageProcessor;
import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionState;
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

    private static final String STYLE_BTN_CANCEL = "btn-cancel";
    private static final String STYLE_BTN_CONFIRM = "btn-confirm";
    private static final String STYLE_BTN_DISABLED = "btn-disabled";
    private static final String STYLE_BASE_CLASS = "button";

    private final DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @FXML private ImageView itemImageView;
    @FXML private TextArea itemDescription;
    @FXML private Label lblItemName, lblAuctionTime, lblBidderName, lblSellerName, lblCurrentPrice, lblMinIncrement;
    @FXML private LineChart<String, Number> priceChart;
    @FXML private Button actionButton;

    private XYChart.Series<String, Number> priceSeries;
    private Auction currAuction;
    private ObservableMap<Integer, Auction> dataSourceMap;
    private Runnable onGoBackAction;
    private MapChangeListener<Integer, Auction> auctionListener;

    private boolean canCancel;
    private boolean canConfirm;

    // INITIALIZATION (Khởi tạo)
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
            if (auctionListener != null) {
                this.dataSourceMap.removeListener(auctionListener);
            }
            auctionListener = change -> {
                if (change.wasAdded() && change.getKey() == currAuction.getId()) {
                    javafx.application.Platform.runLater(() -> updateUI(change.getValueAdded()));
                }
            };
            this.dataSourceMap.addListener(auctionListener);
        }
    }

    // UI UPDATES (Cập nhật giao diện)
    private void updateUI(Auction auction) {
        this.currAuction = auction;

        itemDescription.setText(auction.getItem().getDescription());
        lblItemName.setText(auction.getItem().getName());
        lblAuctionTime.setText(auction.getStartTime().format(displayFormatter) + " - " + auction.getEndTime().format(displayFormatter));

        String winner = auction.getWinner();
        lblBidderName.setText((winner == null || winner.trim().isEmpty()) ? "No bids yet" : winner);

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
                        actionButton.getStyleClass().setAll(STYLE_BASE_CLASS, STYLE_BTN_CANCEL);
                    } else {
                        actionButton.setVisible(false);
                    }
                    break;

                case FINISHED:
                    if (this.canConfirm) {
                        actionButton.setText("Confirm Payment");
                        actionButton.setDisable(false);
                        actionButton.getStyleClass().setAll(STYLE_BASE_CLASS, STYLE_BTN_CONFIRM);
                    } else {
                        actionButton.setText("WAITING PAYMENT");
                        actionButton.setDisable(true);
                        actionButton.getStyleClass().setAll(STYLE_BASE_CLASS, STYLE_BTN_DISABLED);
                    }
                    break;

                case PAID:
                    actionButton.setText("PAID");
                    actionButton.setDisable(true);
                    actionButton.getStyleClass().setAll(STYLE_BASE_CLASS, STYLE_BTN_DISABLED);
                    break;

                case CANCELED:
                    actionButton.setText("CANCELED");
                    actionButton.setDisable(true);
                    actionButton.getStyleClass().setAll(STYLE_BASE_CLASS, STYLE_BTN_DISABLED);
                    break;
            }
        }
    }

    // ACTION HANDLERS (Xử lý sự kiện nút)
    @FXML
    private void handleActionButton() {
        if (currAuction == null) return;

        try {
            AuctionState state = currAuction.getAuctionState();

            if ((state == AuctionState.RUNNING || state == AuctionState.OPENING) && this.canCancel) {
                ServerConnection.getInstance().send(new Request(RequestType.CANCEL).put("auctionId", currAuction.getId()));
                if (onGoBackAction != null) onGoBackAction.run();

            } else if (state == AuctionState.FINISHED && this.canConfirm) {
                ServerConnection.getInstance().send(new Request(RequestType.CONFIRM_PAYMENT).put("auctionId", currAuction.getId()));
            }
        } catch (IOException e) {
            System.err.println("Network error: Could not send request to server.");
        }
    }

    @FXML
    private void handleBack() {
        if (onGoBackAction != null) onGoBackAction.run();
    }
}