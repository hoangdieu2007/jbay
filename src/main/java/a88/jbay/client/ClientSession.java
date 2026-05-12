package a88.jbay.client;

import a88.jbay.controller.client.SellerItemCardController;
import a88.jbay.common.user.User;
import a88.jbay.common.auction.Auction;
import a88.jbay.util.JBayLogger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import java.util.Collections;
import java.util.TreeMap;

public class ClientSession {
    private static ClientSession instance;
    private SellerItemCardController controller;
    private final JBayLogger logger;

    private User user;
    private ObservableMap<Integer, Auction> sellerAuctions;
    private ObservableMap<Integer, Auction> bidderAuctions;

    private ClientSession() {
        this.logger = JBayLogger.getLogger(ClientSession.class);
        user = new User();
        sellerAuctions = FXCollections.observableMap(new TreeMap<>(Collections.reverseOrder()));
        bidderAuctions = FXCollections.observableMap(new TreeMap<>(Collections.reverseOrder()));

    }

    public static ClientSession getInstance() {
        if (instance == null) {
            instance = new ClientSession();
        }
        return instance;
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

    // after each logout, call this to erase session
    public void resetSession() {
        //Reset ttin user
        this.user = new User();

        // Create new map instances to remove all listeners
        this.sellerAuctions = FXCollections.observableMap(new TreeMap<>(Collections.reverseOrder()));
        this.bidderAuctions = FXCollections.observableMap(new TreeMap<>(Collections.reverseOrder()));

        logger.info("Session has been cleared");
    }
}
