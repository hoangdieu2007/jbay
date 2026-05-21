package a88.jbay.controller.app.UserHomeScreenUI;

import a88.jbay.client.ClientSession;
import a88.jbay.common.auction.Auction;
import a88.jbay.controller.app.AuctionUI.SellerItemCardController;
import a88.jbay.view.ViewManager;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.util.Comparator;

public class MyListingsController {

    @FXML
    private TextField sellerSearchField;
    @FXML
    private Button btnCreateListing;
    @FXML
    private TilePane sellerTilePane;

    private ObservableMap<Integer, Auction> sellerMap = ClientSession.getInstance().getSellerAuctions();

    private ObservableList<Auction> sellerList = FXCollections.observableArrayList();

    @FXML
    private void initialize(){
        sellerTilePane.getChildren().clear();

        sellerList.setAll(sellerMap.values());

        FilteredList<Auction> filteredList = new FilteredList<>(sellerList, auction -> true);

        sellerMap.addListener((MapChangeListener<Integer, Auction>) change -> {

            if(change.wasAdded() && change.wasRemoved()){
                int idx = sellerList.indexOf(change.getValueRemoved()); // if cant find auction --> return -1

                if(idx >= 0){
                    sellerList.set(idx, change.getValueAdded()); // if idx = -1 --> crash
                }
            }
            else if (change.wasAdded()){
                sellerList.add(change.getValueAdded());
            }
            else if(change.wasRemoved()){
                sellerList.remove(change.getValueRemoved());
            }

            sellerList.sort(Comparator.comparingInt(Auction :: getId).reversed());

        } );

        filteredList.addListener((ListChangeListener<Auction>) change ->{
            refreshSellerList(filteredList);
        });

        sellerSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredList.setPredicate(auction -> {

                if(newValue == null || newValue.isEmpty()){
                    return true;
                }

                String lowerNewValue = newValue.toLowerCase();
                return auction.getItem().getName().toLowerCase().contains(lowerNewValue);
            });
        });

        refreshSellerList(filteredList);

    }

    @FXML
    private VBox createCardSeller(Auction auction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/a88/jbay/view/app/AuctionUI/seller-item-card.fxml"));
            // load root node
            VBox cardBox = loader.load();

            SellerItemCardController controller = loader.getController();
            controller.setData(auction);

            return cardBox;
        } catch (IOException e) {
            return null;
        }
    }

    @FXML
    public void refreshSellerList(FilteredList<Auction> filteredList) {
        sellerTilePane.getChildren().clear();
        for (Auction auction : filteredList) {
            VBox card = createCardSeller(auction);
            if (card != null) {
                sellerTilePane.getChildren().add(card);
            }
        }
    }

    @FXML
    private void handleCreateListing(){
        try {
            ViewManager.getInstance().loadIntoMainScene("client/client-seller-item-view.fxml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
