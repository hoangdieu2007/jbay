package a88.jbay.controller.client;

import a88.jbay.model.event.Auction;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class SellerBidderHomeScreenController {

    // khởi tạo dsach động
    private ObservableList<Auction> auctionObservableList = FXCollections.observableArrayList();

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

    // =====SELLER=====




    // =====BIDDER=====

    @FXML
    private FlowPane bidderFlowPane;



    // khi tạo 1 Auction mới phải gọi hàm này để cập nhật len UI
    private VBox createCard(Auction auction){
        try{
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/a88/jbay/view/ItemCard.fxml"));
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

    @FXML
    public void initialize(){
        bidderFlowPane.getChildren().clear();

        // cài Listener cho ObservableList
        //Tránh gọi nhầm hàm addListener
        auctionObservableList.addListener((ListChangeListener<Auction>) change -> {
            // nếu còn phần tử, trả về true --> chạy tiếp
            while (change.next()){
                if(change.wasAdded()){
                    for(Auction auction : change.getAddedSubList()){ // chỉ xét dsach các Auction mới dc thêm
                        VBox newCard =  createCard(auction);
                        if (newCard != null){
                            bidderFlowPane.getChildren().add(newCard);
                        }
                    }

                } else if(change.wasReplaced()){
                    for(int i = 0; i < change.getAddedSize(); i++){
                        int index = change.getFrom() + i;
                        Auction updateAuction = change.getAddedSubList().get(i);
                        VBox updatedCard =  createCard(updateAuction);

                        if(updatedCard != null){
                            bidderFlowPane.getChildren().set(index, updatedCard);
                        }
                    }
                }
            }
        });

    }
}
