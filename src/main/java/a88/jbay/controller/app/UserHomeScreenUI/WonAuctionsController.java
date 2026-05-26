package a88.jbay.controller.app.UserHomeScreenUI;

import a88.jbay.client.ClientSession;
import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionState;
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

public class WonAuctionsController {

    @FXML private TilePane winningTilePane;
    @FXML private TextField winningSearchField;

    private JBayLogger logger;
    Map<Integer, VBox> cardBox = new HashMap<>();


    String userName = ClientSession.getInstance().getUser().getUsername();

    ObservableList<Auction> winningList = FXCollections.observableArrayList();
    FilteredList<Auction> filteredList = new FilteredList<>(winningList, auction -> true);

    @FXML
    private void initialize(){
        winningTilePane.getChildren().clear();

        filteredList.addListener((ListChangeListener<Auction>) change -> {
            refreshCard(filteredList);
        });

        ObservableMap<Integer, Auction> auctionMap = ClientSession.getInstance().getBidderAuctions();

        for (Auction auction : auctionMap.values()){
            if (checkWinner(auction)){
                winningList.add(auction);
                createWinningCard(auction); //create cards
            }
        }

        auctionMap.addListener((MapChangeListener< Integer,Auction>) change ->{

            if(change.wasAdded()){
                Auction addedAuction = change.getValueAdded();
                if (checkWinner(addedAuction)){
                    createWinningCard(addedAuction);
                    winningList.add(addedAuction);
                }
            }else if(change.wasRemoved()){
                winningList.remove(change.getValueRemoved());
                cardBox.remove(change.getKey());
            }

            winningList.sort(Comparator.comparingInt(Auction::getId));
        });

        winningSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredList.setPredicate(auction -> {
                if(newValue == null || newValue.isEmpty() ){
                    return true; // display all the cards
                }
                String lowerNewValue = newValue.toLowerCase();

                return auction.getItem().getName().contains(lowerNewValue); // display the cards that their item's name matched the search
            });
        });


    }

    // check if there is an auction winner
    private boolean checkWinner(Auction auction){
        if(auction.getAuctionState() != AuctionState.RUNNING && auction.getAuctionState() != AuctionState.OPENING) {
            return auction.getWinner().equals(userName);
        }
        return false;
    }

    @FXML
    private void createWinningCard(Auction auction){
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/a88/jbay/view/app/AuctionUI/bidder-item-card.fxml"));
        try {
            VBox newCard = loader.load();
            BidderItemCardController controller = loader.getController();

            controller.setItemData(auction);

            cardBox.put(auction.getId(), newCard);


        } catch (IOException e) {
            logger.error("Cannot create Winning card");
            throw new RuntimeException(e);
        }

    }

    @FXML
    private void refreshCard(FilteredList<Auction> filteredList){
        winningTilePane.getChildren().clear();
        for (Auction auction : filteredList){
            if(cardBox.containsKey(auction.getId())){
                VBox card = cardBox.get(auction.getId());
                winningTilePane.getChildren().add(card);
            }
        }
    }



}
