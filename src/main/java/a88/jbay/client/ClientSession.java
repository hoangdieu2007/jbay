package a88.jbay.client;

import a88.jbay.controller.app.AuctionUI.SellerItemCardController;
import a88.jbay.common.user.User;
import a88.jbay.common.auction.Auction;
import a88.jbay.di.ClientApplicationContext;
import a88.jbay.util.JBayLogger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import java.util.Collections;
import java.util.TreeMap;

public class ClientSession {
    private SellerItemCardController controller;
    private final JBayLogger logger;

    private User user;
    private ObservableMap<Integer, Auction> sellerAuctions;
    private ObservableMap<Integer, Auction> bidderAuctions;
    private ObservableMap<Integer, Auction> wonAuctions;
    private ObservableMap<Integer, User> adminUsers = FXCollections.observableHashMap();
    private ObservableMap<Integer, Auction> adminAuctions = FXCollections.observableHashMap();

    public ClientSession() {
        this.logger = JBayLogger.getLogger(ClientSession.class);
        user = new User();
        sellerAuctions = FXCollections.observableMap(new TreeMap<>(Collections.reverseOrder()));
        bidderAuctions = FXCollections.observableMap(new TreeMap<>(Collections.reverseOrder()));
        wonAuctions = FXCollections.observableMap(new TreeMap<>(Collections.reverseOrder()));

    }

    public static ClientSession getInstance() {
        return ClientApplicationContext.getInstance().getDependency(ClientSession.class);
    }



    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public ObservableMap<Integer ,Auction> getSellerAuctions() {
        return sellerAuctions;
    }

    public ObservableMap<Integer, Auction> getBidderAuctions() {
        return bidderAuctions;
    }

    public ObservableMap<Integer, Auction> getWonAuctions(){
        return wonAuctions;
    }

    public ObservableMap<Integer, User> getAdminUsers() { return adminUsers; }

    public ObservableMap<Integer, Auction> getAdminAuctions() { return adminAuctions; }

    // after each logout, call this to erase session
    public void resetSession() {
        //Reset ttin user
        this.user = new User();

        // Create new map instances to remove all listeners
        this.sellerAuctions = FXCollections.observableMap(new TreeMap<>(Collections.reverseOrder()));
        this.bidderAuctions = FXCollections.observableMap(new TreeMap<>(Collections.reverseOrder()));
        this.wonAuctions  = FXCollections.observableMap(new TreeMap<>(Collections.reverseOrder()));

        // Clear admin maps
        this.adminUsers.clear();
        this.adminAuctions.clear();

        logger.info("Session has been cleared");
    }
}
