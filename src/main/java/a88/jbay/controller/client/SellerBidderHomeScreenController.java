package a88.jbay.controller.client;

import a88.jbay.model.event.Auction;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import java.io.IOException;

public class SellerBidderHomeScreenController {

    // =====SELLER=====
    public void handleCreateListing(ActionEvent actionEvent) {
    }


    // =====BIDDER=====

    @FXML
    private FlowPane bidderFlowPane;

    // khởi tạo dsach động
    private ObservableList<Auction> auctionObservableList = FXCollections.observableArrayList();

    private void createAndAddCardToUI(Auction auction){
        try{
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/a88/jbay/view/ItemCard.fxml"));
            // load root node
            VBox cardBox = loader.load();

            BidderItemCardController controller = loader.getController();
            controller.setItemData(auction);

            //Gắn ô vừa tạo vào FlowPane
            // getChildren() trả về ObservableList; xog mới add
            bidderFlowPane.getChildren().add(cardBox);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize(){
        bidderFlowPane.getChildren().clear();

        // cài Listener cho ObservableList
        //Tránh gọi nhầm hàm addListener
        auctionObservableList.addListener((ListChangeListener<Auction>) change -> {
            // nếu còn phần tử, trả về true --> chạy tiếp
            while (change.next()){
                if(change.wasAdded()){
                    for(Auction auction : change.getAddedSubList()){
                        createAndAddCardToUI(auction);
                    }
                }
            }
        });

    }
}
