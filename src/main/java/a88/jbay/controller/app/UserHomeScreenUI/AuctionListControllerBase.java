package a88.jbay.controller.app.UserHomeScreenUI;

import a88.jbay.common.auction.Auction;
import a88.jbay.util.JBayLogger;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public abstract class AuctionListControllerBase<CardController> {

    @FXML
    protected TextField auctionSearchField;

    @FXML
    protected TilePane auctionTilePane;

    protected ObservableList<Auction> auctionList = FXCollections.observableArrayList();
    protected FilteredList<Auction> filteredList;

    protected Map<Integer, VBox> cardBox = new HashMap<>();
    protected Map<Integer, CardController> controllerMap = new HashMap<>();

    private MapChangeListener<Integer, Auction> auctionMapListener;
    private final JBayLogger logger = JBayLogger.getLogger(getClass());

    protected void initializeAuctionList() {
        auctionTilePane.getChildren().clear();

        filteredList = new FilteredList<>(auctionList, auction -> true);

        filteredList.addListener((ListChangeListener<Auction>) change -> refreshAuctionList());

        ObservableMap<Integer, Auction> auctionMap = getAuctionMap();

        for (Auction auction : auctionMap.values()) {
            addOrUpdateAuction(auction);
        }

        if (auctionMapListener != null) {
            auctionMap.removeListener(auctionMapListener);
        }
        auctionMapListener = change -> {
            if (change.wasAdded()) {
                addOrUpdateAuction(change.getValueAdded());
            } else if (change.wasRemoved()) {
                removeAuction(change.getKey());
            }

            auctionList.sort(getAuctionComparator());
            refreshAuctionList();
        };
        auctionMap.addListener(auctionMapListener);

        auctionSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredList.setPredicate(auction -> matchesSearch(auction, newValue));
        });

        auctionList.sort(getAuctionComparator());
        refreshAuctionList();
    }

    //subclasses get their own auctionMap
    protected abstract ObservableMap<Integer, Auction> getAuctionMap();

    // for creating cards
    protected abstract String getCardFxmlPath();

    // subclasses set their card data themselves
    protected abstract void setCardData(CardController controller, Auction auction);

    protected boolean shouldShowAuction(Auction auction) {
        return true;
    }

    protected Comparator<Auction> getAuctionComparator() {
        return Comparator.comparingInt(Auction::getId);
    }

    protected String getCreateCardErrorMessage() {
        return "Cannot create auction card!";
    }

    // find auction cards matching the search
    protected boolean matchesSearch(Auction auction, String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return true;
        }

        String lowerNewValue = searchText.toLowerCase();
        return auction.getItem().getName().toLowerCase().contains(lowerNewValue);
    }

    protected void addOrUpdateAuction(Auction auction) {
        if (auction == null) {
            return;
        }

        int auctionId = auction.getId();

        if (!shouldShowAuction(auction)) { // if the controller doesn't override this method --> always true
            removeAuction(auctionId);
            return;
        }

        CardController controller = controllerMap.get(auctionId);
        if (controller == null) { // if it is a new auction
            createAuctionCard(auction);
        } else {
            setCardData(controller, auction); // set new data for auction cards if they are already created
        }

        if (!cardBox.containsKey(auctionId)) {
            return;
        }

        int idx = findAuctionIndex(auctionId);
        if (idx >= 0) {
            auctionList.set(idx, auction);
        } else {
            auctionList.add(auction);
        }
    }

    protected void removeAuction(int auctionId) {
        auctionList.removeIf(auction -> auction.getId() == auctionId);
        cardBox.remove(auctionId);
        controllerMap.remove(auctionId);
    }

    @FXML
    protected void createAuctionCard(Auction auction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(getCardFxmlPath()));

            VBox newCard = loader.load();

            CardController controller = loader.getController();
            setCardData(controller, auction);

            cardBox.put(auction.getId(), newCard);
            controllerMap.put(auction.getId(), controller);

        } catch (IOException e) {
            logger.error(getCreateCardErrorMessage(), e);
        }
    }

    @FXML
    protected void refreshAuctionList() {
        auctionTilePane.getChildren().clear();

        for (Auction auction : filteredList) {
            VBox card = cardBox.get(auction.getId());
            if (card != null) {
                auctionTilePane.getChildren().add(card);
            }
        }
    }

    private int findAuctionIndex(int auctionId) {
        for (int i = 0; i < auctionList.size(); i++) {
            if (auctionList.get(i).getId() == auctionId) {
                return i;
            }
        }
        return -1;
    }
}
