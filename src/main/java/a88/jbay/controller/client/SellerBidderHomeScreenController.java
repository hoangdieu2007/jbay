package a88.jbay.controller.client;

import a88.jbay.client.ClientSession;
import a88.jbay.model.event.Auction;
import a88.jbay.server.ClientHandler;
import a88.jbay.system.UserSystem;
import a88.jbay.view.ViewManager;
import com.almasb.fxgl.cutscene.CutsceneScene;
import javafx.collections.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;

public class SellerBidderHomeScreenController {
    // khởi tạo dsach động
    private ObservableList<Auction> auctionObservableList = FXCollections.observableArrayList();
    private ObservableList<Auction> sellerAuctionObservableList = FXCollections.observableArrayList();

    // ==== Xử lý thông tin từ Server qua Socket===
    // 1. Nạp toàn bộ dữ liệu lần đầu (Chạy lúc vừa mở form)
    // Thằng Socket sau khi query Database lấy List<Auction> sẽ gọi hàm này
    public void loadInitialData(List<Auction> initialList) {
        javafx.application.Platform.runLater(() -> {
            auctionObservableList.clear(); // Xóa rác cũ nếu có
            auctionObservableList.addAll(initialList); // Kích nổ wasAdded() hàng loạt
        });
    }

    // 2. Hứng 1 phiên đấu giá mới toanh (Real-time)
    // Thằng Socket nhận tin báo có Seller vừa đăng bài sẽ ném Object vào đây
    public void onNewAuctionReceived(Auction newAuction) {
        javafx.application.Platform.runLater(() -> {
            auctionObservableList.add(newAuction); // Kích nổ wasAdded() tạo 1 thẻ nối đuôi
        });
    }

    // 3. Hứng dữ liệu cập nhật: Lên giá hoặc Hết giờ (Real-time)
    // Thằng Socket nhận tin báo có người Bid, hoặc Server báo Ended sẽ ném Object đã update vào đây
    public void onAuctionUpdated(Auction updatedAuction) {
        javafx.application.Platform.runLater(() -> {
            // Quét tìm cái thẻ cũ đang nằm ở vị trí nào
            for (int i = 0; i < auctionObservableList.size(); i++) {
                if (auctionObservableList.get(i).getId() == updatedAuction.getId()) {

                    // Ghi đè Object mới vào vị trí cũ -> Kích nổ wasReplaced()
                    auctionObservableList.set(i, updatedAuction);

                    // Tìm thấy và đè xong rồi thì thoát vòng lặp luôn cho tối ưu
                    break;
                }
            }
        });
    }

    // LOG OUT
    @FXML private Button btnLogOut;

    @FXML
    private void handleLogOut(){
        try {
            ViewManager.displayScene("client/client-login-register-view.fxml");
            ClientSession.getInstance().resetSession();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //Xử lí phần quay trở lại vào màn hình
    @FXML private TabPane mainTabPane;

    /*
     * index = 0: Tab Seller
     * index = 1: Tab Bidder
     */
    public void selectTab(int index) {
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().select(index);
        }
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

    private VBox createCardSeller(Auction auction) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("client/seller-item-card.fxml"));
            // load root node
            VBox cardBox = loader.load();

            SellerItemCardController controller = loader.getController();
            controller.setData(auction);

            return cardBox;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @FXML
    public void initializeSellerUI() {
        sellerFlowPane.getChildren().clear();
        ObservableMap<Integer, Auction> sellerMap = ClientSession.getInstance().getSellerAuctions();
        System.out.println("Seller UI initialized with " + sellerMap.size() + " auctions");

        int initIndex = 0;
        for (Auction auction : sellerMap.values()) {
            VBox newCard = createCardBidder(auction);
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


    private VBox createCardBidder(Auction auction){
        try{
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("client/bidder-item-card.fxml"));
            // load root node
            VBox cardBox = loader.load();

            BidderItemCardController controller = loader.getController();
            controller.setItemData(auction);

            return cardBox;
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

    private Map<Integer, VBox> bidderCardBox = new HashMap<>();

    @FXML
    public void  initializeBidderUI(){
        bidderFlowPane.getChildren().clear();
        ObservableMap<Integer, Auction> bidderMap = ClientSession.getInstance().getBidderAuctions();
        System.out.println("Bidder UI initialized with " + bidderMap.size() + " auctions");

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
