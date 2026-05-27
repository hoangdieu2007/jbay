package a88.jbay.controller.app.UserHomeScreenUI;

import a88.jbay.client.ClientSession;
import a88.jbay.common.auction.Auction;
import a88.jbay.controller.app.AuctionUI.SellerItemCardController;
import a88.jbay.util.JBayLogger;
import a88.jbay.view.ViewManager;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class MyListingsController {

    @FXML
    private TextField sellerSearchField;
    @FXML
    private Button btnCreateListing;
    @FXML
    private TilePane sellerTilePane;

    private ObservableList<Auction> sellerList = FXCollections.observableArrayList();

    Map<Integer, VBox> sellerCard = new HashMap<>();
    Map<Integer, SellerItemCardController> controllerMap = new HashMap<>();

    private static JBayLogger logger;

    @FXML
    private void initialize(){
        sellerTilePane.getChildren().clear();

        ObservableMap<Integer, Auction> sellerMap = ClientSession.getInstance().getSellerAuctions();

        FilteredList<Auction> filteredList = new FilteredList<>(sellerList, auction -> true);

        filteredList.addListener((ListChangeListener<Auction>) change ->{
            refreshSellerList(filteredList);
        });

        for (Auction auction : sellerMap.values()){
            if(!sellerCard.containsKey(auction.getId())){
                createCardSeller(auction);
                sellerList.add(auction);

            }
        }

        sellerMap.addListener((MapChangeListener<Integer, Auction>) change -> {

            if(change.wasAdded() && change.wasRemoved()){
                int idx = sellerList.indexOf(change.getValueRemoved()); // if cant find auction --> return -1
                SellerItemCardController controller = controllerMap.get(change.getKey());
                if(controller != null) {
                    controller.setData(change.getValueAdded());
                }
                if(idx >= 0){
                    sellerList.set(idx, change.getValueAdded()); // if idx = -1 --> crash
                }
            }
            else if (change.wasAdded()){
                sellerList.add(change.getValueAdded());
                createCardSeller(change.getValueAdded());
            }
            else if(change.wasRemoved()){
                sellerList.remove(change.getValueRemoved());
                sellerCard.remove(change.getKey());
                controllerMap.remove(change.getKey());
            }

            sellerList.sort(Comparator.comparingInt(Auction :: getId).reversed());

        } );


        sellerSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
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
    private void createCardSeller(Auction auction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/a88/jbay/view/app/AuctionUI/seller-item-card.fxml"));
            // load root node
            VBox cardBox = loader.load();

            SellerItemCardController controller = loader.getController();
            controller.setData(auction);

            sellerCard.put(auction.getId(), cardBox);
            controllerMap.put(auction.getId(), controller);


        } catch (IOException e) {
            logger.info("Cannot create Seller card!");
        }
    }

    @FXML
    public void refreshSellerList(FilteredList<Auction> filteredList) {
        sellerTilePane.getChildren().clear();
        for (Auction auction : filteredList) {
            int id = auction.getId();
            if (sellerCard.containsKey(id)) {
                VBox card = sellerCard.get(id);
                if (card != null) {
                    sellerTilePane.getChildren().add(card);
                }
            }
        }
    }

    @FXML
    private void handleCreateListing(){
        try {
            ViewManager.getInstance().loadIntoMainScene("AuctionUI/client-seller-item-view.fxml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
