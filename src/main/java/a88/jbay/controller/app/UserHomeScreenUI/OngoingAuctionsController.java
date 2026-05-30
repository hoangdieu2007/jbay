package a88.jbay.controller.app.UserHomeScreenUI;

import a88.jbay.client.ClientSession;
import a88.jbay.common.auction.Auction;
import a88.jbay.controller.app.AuctionUI.BidderItemCardController;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;

public class OngoingAuctionsController extends AuctionListControllerBase<BidderItemCardController> {

    @FXML
    private void initialize() {
        initializeAuctionList();
    }

    @Override
    protected ObservableMap<Integer, Auction> getAuctionMap() {
        return ClientSession.getInstance().getBidderAuctions();
    }

    @Override
    protected String getCardFxmlPath() {
        return "/a88/jbay/view/app/AuctionUI/bidder-item-card.fxml";
    }

    @Override
    protected void setCardData(BidderItemCardController controller, Auction auction) {
        controller.setItemData(auction);
    }

    @Override
    protected String getCreateCardErrorMessage() {
        return "Cannot create Bidder card!";
    }
}
