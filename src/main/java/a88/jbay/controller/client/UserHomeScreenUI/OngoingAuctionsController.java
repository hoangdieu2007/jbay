package a88.jbay.controller.client.UserHomeScreenUI;

import a88.jbay.client.ClientSession;
import a88.jbay.common.auction.Auction;
import a88.jbay.controller.client.AuctionUI.BidderItemCardController;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;


import java.io.IOException;
import java.util.Comparator;

public class OngoingAuctionsController {

    @FXML
    private TextField bidderSearchField;
    @FXML
    private TilePane bidderTilePane;

    ObservableMap<Integer, Auction> bidderMap = ClientSession.getInstance().getBidderAuctions();

    ObservableList<Auction> bidderList = FXCollections.observableArrayList();

    FilteredList<Auction> filteredList = new FilteredList<>(bidderList, auction -> true);

    @FXML
    private void initialize(){
        bidderList.setAll(bidderMap.values());

        bidderMap.addListener((MapChangeListener<Integer, Auction>) change -> {

            if(change.wasAdded() && change.wasRemoved()){
                int idx = bidderList.indexOf(change.getValueRemoved());

                if (idx >= 0){
                    bidderList.set(idx, change.getValueAdded());
                }
            } else if (change.wasAdded()){
                bidderList.add(change.getValueAdded());

            } else if (change.wasRemoved()) {
                bidderList.remove(change.getValueRemoved());

            }

            bidderList.sort(Comparator.comparingInt(Auction:: getId));
        });

        filteredList.addListener((ListChangeListener<Auction>) change -> {
            refreshBidderList(filteredList);
        });

        bidderSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredList.setPredicate(auction -> {

                if(newValue == null || newValue.isEmpty()){
                    return true;
                }

                String lowerNewValue = newValue.toLowerCase();
                return auction.getItem().getName().toLowerCase().contains(lowerNewValue);
            });
        });

        refreshBidderList(filteredList);


    }

    @FXML
    private VBox createBidderCard(Auction auction){
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/a88/jbay/view/client/AuctionUI/bidder-item-card.fxml"));

            VBox cardBox = loader.load();

            BidderItemCardController controller = loader.getController();
            controller.setItemData(auction);

            return cardBox;
        } catch (IOException e){
            return null;
        }
    }

    @FXML
    private void refreshBidderList(FilteredList<Auction> filteredList){
        bidderTilePane.getChildren().clear();

        for (Auction auction : filteredList){
            VBox cardBox = createBidderCard(auction);
            if (cardBox != null){
                bidderTilePane.getChildren().add(cardBox);
            }
        }
    }

}
