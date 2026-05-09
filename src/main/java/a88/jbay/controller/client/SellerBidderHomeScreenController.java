package a88.jbay.controller.client;

import a88.jbay.client.ClientSession;
import a88.jbay.client.ServerConnection;
import a88.jbay.controller.ControllerProvider;
import a88.jbay.model.event.Auction;
import a88.jbay.model.network.Request;
import a88.jbay.model.network.RequestType;
import a88.jbay.util.JBayLogger;
import a88.jbay.view.ViewManager;
import javafx.collections.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.*;

public class SellerBidderHomeScreenController {
    private final JBayLogger logger = JBayLogger.getInstance();

    // LOG OUT
    @FXML private Button btnLogOut;

    @FXML
    private void handleLogOut(){
        try {
            ServerConnection.getInstance().send(new Request(RequestType.LOGOUT)
                    .put("sessionId", ClientSession.getInstance().getUser().getSessionId()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //UserName
    @FXML private Label lblUserName;



    //Xử lí phần quay trở lại vào màn hình
    @FXML private TabPane mainTabPane;
    public static int targetTabIndex = 0;
    /*
     * index = 0: Tab Seller
     * index = 1: Tab Bidder
     */
    @FXML
    public void initialize() {
        // Đăng ký chính nó vào Provider
        ControllerProvider.getInstance().registerController(this);

        // Tự động chọn tab dựa trên biến targetTabIndex ngay khi vừa load xong
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().select(targetTabIndex);
        }

        lblUserName.setText(ClientSession.getInstance().getUser().getUsername());

        initializeSellerUI();
        initializeBidderUI();
    }

    /** ====SELLER==== **/

    @FXML
    private FlowPane sellerFlowPane;

    private Map<Integer, VBox> sellerCardBox = new HashMap<>();

    public void handleCreateListing(ActionEvent actionEvent) {
        try {
            ViewManager.displayScene("client/client-seller-item-view.fxml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private VBox createCardSeller(Auction auction) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/a88/jbay/view/client/seller-item-card.fxml"));
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
    public void initializeSellerUI() {
        sellerFlowPane.getChildren().clear();
        ObservableMap<Integer, Auction> sellerMap = ClientSession.getInstance().getSellerAuctions();
        logger.debug("Seller UI initialized with " + sellerMap.size() + " auctions");

        int initIndex = 0;
        for (Auction auction : sellerMap.values()) {
            VBox newCard = createCardSeller(auction);
            if (newCard != null) {
                sellerCardBox.put(auction.getId(), newCard);
                sellerFlowPane.getChildren().add(initIndex++, newCard);
            }
        }

        ClientSession.getInstance().getSellerAuctions().addListener((MapChangeListener<Integer, Auction>) change -> {
            // nếu còn phần tử, trả về true --> chạy tiếp

            Integer id = change.getKey();

                if (change.wasAdded() && !change.wasRemoved()) {
                        Auction newAuction = change.getValueAdded();
                        VBox newCard = createCardSeller(newAuction);
                        if (newCard != null) {
                            sellerCardBox.put(id, newCard);
                            int targetIdx = new ArrayList<>(sellerMap.keySet()).indexOf(id); // find the exact idx to display card
                            sellerFlowPane.getChildren().add(targetIdx, newCard);
                        }

                } else if (change.wasAdded() && change.wasRemoved()) { //ObservableMap doesnt have wasReplaced()
                    Auction updateAuction = change.getValueAdded();
                    VBox oldCard = sellerCardBox.get(id);
                    if (oldCard != null) {
                        int index = sellerFlowPane.getChildren().indexOf(oldCard);
                        VBox updatedCard = createCardSeller(updateAuction);
                        if (updatedCard != null) {
                            sellerFlowPane.getChildren().set(index, updatedCard);
                        }
                    }

                }else if (change.wasRemoved() && !change.wasAdded()){
                    VBox oldCard = sellerCardBox.get(id);
                    if (oldCard != null) {
                        sellerFlowPane.getChildren().remove(oldCard); // delete from UI
                        sellerCardBox.remove(id);
                    }
                }

        });

    }


    /** ====BIDDER==== **/
    @FXML private FlowPane bidderFlowPane;

    @FXML
    private VBox createCardBidder(Auction auction){
        try{
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/a88/jbay/view/client/bidder-item-card.fxml"));
            // load root node
            VBox cardBox = loader.load();

            BidderItemCardController controller = loader.getController();
            controller.setItemData(auction);

            return cardBox;
        }catch (IOException e){
                        return null;
        }
    }

    private Map<Integer, VBox> bidderCardBox = new HashMap<>();

    @FXML
    public void  initializeBidderUI(){
        bidderFlowPane.getChildren().clear();
        ObservableMap<Integer, Auction> bidderMap = ClientSession.getInstance().getBidderAuctions();
        logger.debug("Bidder UI initialized with " + bidderMap.size() + " auctions");

        int initIndex = 0;
        for (Auction auction : bidderMap.values()) {
            VBox newCard = createCardBidder(auction);
            if (newCard != null) {
                bidderCardBox.put(auction.getId(), newCard);
                bidderFlowPane.getChildren().add(initIndex++, newCard);
            }
        }

        ClientSession.getInstance().getBidderAuctions().addListener((MapChangeListener< Integer, Auction>) change -> {
            // nếu còn phần tử, trả về true --> chạy tiếp
            Integer id = change.getKey();

            if(change.wasAdded() && !change.wasRemoved()){
                    Auction newAuction = change.getValueAdded();
                    VBox newCard =  createCardBidder(newAuction);

                    if (newCard != null){
                        bidderCardBox.put(id, newCard);
                        int targetIndex = new ArrayList<>(ClientSession.getInstance().getBidderAuctions().keySet()).indexOf(id); // find index of the ID in the List of keys
                        bidderFlowPane.getChildren().add(targetIndex, newCard);
                    }


                } else if(change.wasAdded() && change.wasRemoved()){
                    VBox oldCard = bidderCardBox.get(id);

                    if (oldCard != null) {
                        int index = bidderFlowPane.getChildren().indexOf(oldCard);
                        Auction updateAuction = change.getValueAdded();
                        VBox updatedCard = createCardBidder(updateAuction);

                        if (updatedCard != null) {
                            bidderFlowPane.getChildren().set(index, updatedCard);
                        }
                    }
                }

        });
    }
}
