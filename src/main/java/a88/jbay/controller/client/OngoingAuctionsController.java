package a88.jbay.controller.client;

import a88.jbay.client.ClientSession;
import a88.jbay.common.auction.Auction;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import javax.imageio.IIOException;
import java.io.IOException;

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


    }

    @FXML
    private VBox createBidderCard(Auction auction){
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/a88/jbay/view/client/bidder-item-card.fxml"));

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
