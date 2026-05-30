package a88.jbay.controller.app.UserHomeScreenUI;

import a88.jbay.client.ClientSession;
import a88.jbay.common.auction.Auction;
import a88.jbay.controller.app.AuctionUI.SellerItemCardController;
import a88.jbay.view.ViewManager;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.io.IOException;
import java.util.Comparator;

public class MyListingsController extends AuctionListControllerBase<SellerItemCardController> {

    @FXML
    private Button btnCreateListing;

    @FXML
    private void initialize() {
        initializeAuctionList();
    }

    @Override
    protected ObservableMap<Integer, Auction> getAuctionMap() {
        return ClientSession.getInstance().getSellerAuctions();
    }

    @Override
    protected String getCardFxmlPath() {
        return "/a88/jbay/view/app/AuctionUI/seller-item-card.fxml";
    }

    @Override
    protected void setCardData(SellerItemCardController controller, Auction auction) {
        controller.setData(auction);
    }

    @Override
    protected Comparator<Auction> getAuctionComparator() {
        return Comparator.comparingInt(Auction::getId).reversed();
    }

    @Override
    protected String getCreateCardErrorMessage() {
        return "Cannot create Seller card!";
    }

    @FXML
    private void handleCreateListing() {
        try {
            ViewManager.getInstance().loadIntoMainScene("AuctionUI/client-seller-item-view.fxml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
