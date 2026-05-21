package a88.jbay.controller.client.UserHomeScreenUI;

import a88.jbay.client.ClientSession;
import a88.jbay.client.ServerConnection;
import a88.jbay.controller.ControllerProvider;
import a88.jbay.common.auction.Auction;
import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import a88.jbay.controller.client.AuctionUI.BidderItemCardController;
import a88.jbay.controller.client.AuctionUI.SellerItemCardController;
import a88.jbay.util.JBayLogger;
import a88.jbay.view.ViewManager;
import javafx.animation.TranslateTransition;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.*;

public class SellerBidderHomeScreenController {
    private final JBayLogger logger = JBayLogger.getLogger(SellerBidderHomeScreenController.class);

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

    @FXML
    private AnchorPane sliderMenu;

    private boolean isMenuOpen = false;

    @FXML
    public void handleMenuToggle() {
        // Set animation duration to 0.3 seconds
        TranslateTransition transition = new TranslateTransition(Duration.seconds(0.3), sliderMenu);

        if (isMenuOpen) {
            // Menu is open, slide it back out of view to the left
            transition.setToX(-sliderMenu.getPrefWidth());
            isMenuOpen = false;
        } else {
            // Menu is closed, slide it into view to the 0 position
            transition.setToX(0);
            isMenuOpen = true;
        }

        transition.play();
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
    private TilePane sellerTilePane;

    private Map<Integer, VBox> sellerCardBox = new HashMap<>();

    public void handleCreateListing(ActionEvent actionEvent) {
        try {
            ViewManager.getInstance().loadIntoMainScene("AuctionUI/client-seller-item-view.fxml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private VBox createCardSeller(Auction auction) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/a88/jbay/view/client/AuctionUI/seller-item-card.fxml"));
            // load root node
            VBox cardBox = loader.load();

            SellerItemCardController controller = loader.getController();
            controller.setData(auction);

            return cardBox;
        } catch (IOException e) {
            return null;
        }
    }

    @FXML private TextField sellerSearchField;

    @FXML
    public void initializeSellerUI() {
        sellerTilePane.getChildren().clear();

        ObservableMap<Integer, Auction> sellerMap = ClientSession.getInstance().getSellerAuctions();

        ObservableList<Auction> sellerList = FXCollections.observableList(new ArrayList<>(sellerMap.values()));

        FilteredList<Auction> filteredList = new FilteredList<>(sellerList, auction -> true);

        logger.debug("Seller UI initialized with " + sellerMap.size() + " auctions");


        ClientSession.getInstance().getSellerAuctions().addListener((MapChangeListener<Integer, Auction>) change -> {
            // nếu còn phần tử, trả về true --> chạy tiếp
            if(change.wasAdded() && !change.wasRemoved()){
                sellerList.add(change.getValueAdded());
            }
            else if(change.wasAdded() && change.wasRemoved()){
                int idx = sellerList.indexOf(change.getValueRemoved());

                if (idx >= 0) {
                    sellerList.set(idx, change.getValueAdded());
                }
            }
            else if(change.wasRemoved()) {
                sellerList.remove(change.getValueRemoved());
            }

            sellerList.sort(Comparator.comparingInt(Auction:: getId).reversed());


        });

        /** xử lý thay đổi from sellerList và textField*/
        filteredList.addListener((ListChangeListener<Auction>) change -> {
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


    /** ====BIDDER==== **/
    @FXML private TilePane bidderTilePane;

    @FXML
    private VBox createCardBidder(Auction auction){
        try{
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/a88/jbay/view/client/AuctionUI/bidder-item-card.fxml"));
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
    TextField bidderSearchField;

    @FXML
    public void  initializeBidderUI(){
        bidderTilePane.getChildren().clear();

        ObservableMap<Integer, Auction> bidderMap = ClientSession.getInstance().getBidderAuctions();

        ObservableList<Auction> bidderList = FXCollections.observableList(new ArrayList<>(bidderMap.values()));

        FilteredList<Auction> filteredList = new FilteredList<>(bidderList, auction -> true);

        logger.debug("Bidder UI initialized with " + bidderMap.size() + " auctions");

        /**bidderList.sort(Comparator.comparingInt(Auction::getId).reversed());
         refreshBidderList(filteredList);*/

        ClientSession.getInstance().getBidderAuctions().addListener((MapChangeListener< Integer, Auction>) change -> {
            // nếu còn phần tử, trả về true --> chạy tiếp
            if(change.wasAdded() && !change.wasRemoved()){
                bidderList.add(change.getValueAdded());

            }
            else if (change.wasAdded() && change.wasRemoved()) {

                int idx = bidderList.indexOf(change.getValueRemoved()); // Nếu ko tìm thấy --> trả về -1

                if (idx >= 0) {
                    bidderList.set(idx, change.getValueAdded()); // nhận idx = -1 --> crash
                }
            }
            else{
                bidderList.remove(change.getValueRemoved());
            }

            bidderList.sort(Comparator.comparingInt(Auction::getId).reversed());


        });
        /** xử lý thay đổi from bidderList và textField*/
        filteredList.addListener((ListChangeListener<Auction>) change -> {
            refreshBidderList(filteredList);
        });

        bidderSearchField.textProperty().addListener(((observable, oldValue, newValue) -> {
            filteredList.setPredicate(auction -> {

                if (newValue == null || newValue.isEmpty()){ // display all auctions
                    return true;
                }

                String lowerNewValue = newValue.toLowerCase();
                return auction.getItem().getName().toLowerCase().contains(lowerNewValue); // display auctions that satisfy the condition
            });
        }));

        refreshBidderList(filteredList);
    }



    public void refreshBidderList(FilteredList<Auction> filteredList) {
        bidderTilePane.getChildren().clear();
        for (Auction auction : filteredList) {
            VBox card = createCardBidder(auction);
            if (card != null) {
                bidderTilePane.getChildren().add(card);
            }
        }
        // Cleanup cache for auctions that are no longer in the list
        bidderCardBox.keySet().removeIf(id -> filteredList.stream().noneMatch(a -> a.getId() == id));
    }


    public void refreshSellerList(FilteredList<Auction> filteredList) {
        sellerTilePane.getChildren().clear();
        for (Auction auction : filteredList) {
            VBox card = createCardSeller(auction);
            if (card != null) {
                sellerTilePane.getChildren().add(card);
            }
        }
        // Cleanup cache
        sellerCardBox.keySet().removeIf(id -> filteredList.stream().noneMatch(a -> a.getId() == id));
    }
}