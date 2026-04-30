package a88.jbay.controller.client;

import a88.jbay.client.ServerConnection;
import a88.jbay.model.network.Request;
import a88.jbay.model.network.RequestType;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

import java.io.IOException;

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
            double bidAmount = Double.parseDouble(bidInput.getText());
            double currentPrice = Double.parseDouble(currentPriceLabel.getText());

            if (bidAmount <= currentPrice) {
                errorLabel.setText("Need higher bid!");
                errorLabel.setVisible(true);
                return;
            }

            Request req = new Request(RequestType.BID);
            req.put("auctionID", currentAuctionId);
            req.put("amount", bidAmount);
            ServerConnection.getInstance().send(req);
        } catch (NumberFormatException e) {
            errorLabel.setText("Bid need to be a number!");
            errorLabel.setVisible(true);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).show();
        }
    }
}
