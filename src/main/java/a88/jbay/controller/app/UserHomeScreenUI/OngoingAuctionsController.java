package a88.jbay.controller.app.UserHomeScreenUI;

import a88.jbay.client.ClientSession;
import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.BidData;
import a88.jbay.controller.app.AuctionUI.BidderItemCardController;
import a88.jbay.util.JBayLogger;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;


import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class OngoingAuctionsController {

    @FXML
    private TextField bidderSearchField;
    @FXML
    private TilePane bidderTilePane;

    ObservableMap<Integer, Auction> bidderMap = ClientSession.getInstance().getBidderAuctions();

    ObservableList<Auction> bidderList = FXCollections.observableArrayList();

    FilteredList<Auction> filteredList = new FilteredList<>(bidderList, auction -> true);

    Map<Integer, VBox> bidderCard = new HashMap<>();
    Map<Integer, BidderItemCardController> controllerMap = new HashMap<>();

    private static JBayLogger logger;

    @FXML
    private void initialize(){

        filteredList.addListener((ListChangeListener<Auction>) change -> {
            refreshBidderList(filteredList);
        });

        for(Auction auction :  bidderMap.values()){
            if (!controllerMap.containsKey(auction.getId())) { // check if the auction's card is already created
                bidderList.add(auction);
                createBidderCard(auction);
            }
        }

        bidderMap.addListener((MapChangeListener<Integer, Auction>) change -> {

            int idx = change.getKey();

            if(change.wasAdded() && change.wasRemoved()){
                BidderItemCardController controller = controllerMap.get(idx);
                if(controller != null) {
                    controllerMap.get(idx).setItemData(change.getValueAdded());
                }

                if (idx >= 0){
                    bidderList.set(idx, change.getValueAdded());
                }
            } else if (change.wasAdded()){
                bidderList.add(change.getValueAdded());
                createBidderCard(change.getValueAdded());

            } else if (change.wasRemoved()) {
                bidderList.remove(change.getValueRemoved());
                controllerMap.remove(idx);
                bidderCard.remove(idx);

            }

            bidderList.sort(Comparator.comparingInt(Auction:: getId));
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
    }

    @FXML
    private void createBidderCard(Auction auction){
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/a88/jbay/view/app/AuctionUI/bidder-item-card.fxml"));

            VBox cardBox = loader.load();

            BidderItemCardController controller = loader.getController();
            controller.setItemData(auction);

            bidderCard.put(auction.getId(), cardBox);
            controllerMap.put(auction.getId(), controller);


        } catch (IOException e){
            logger.info("Cannot create Bidder card!");
        }
    }

    @FXML
    private void refreshBidderList(FilteredList<Auction> filteredList){
        bidderTilePane.getChildren().clear();

        for (Auction auction : filteredList){
           VBox cardBox = bidderCard.get(auction.getId());
            if (cardBox != null){
                bidderTilePane.getChildren().add(cardBox);
            }
        }
    }

}
