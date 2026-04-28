package a88.jbay.client;

import a88.jbay.model.entity.user.User;
import a88.jbay.model.event.Auction;
import javafx.collections.FXCollections;

import java.util.ArrayList;
import java.util.List;

public class ClientSession {
    private static ClientSession instance;

    User user;
    List<Auction> sellerAuctions;
    List<Auction> bidderAuctions;

    private ClientSession() {
        user = new User();
        sellerAuctions = FXCollections.observableArrayList();
        bidderAuctions = FXCollections.observableArrayList();
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

    public List getSellerAuctions() {
        return sellerAuctions;
    }

    public List getBidderAuctions() {
        return bidderAuctions;
    }

    public void addSellerAuction(Auction auction) {
        sellerAuctions.add(auction);
    }

    public void addBuyerAuction(Auction auction) {
        bidderAuctions.add(auction);
    }

    // after each logout, call this to erase session
    public void resetSession() {
        this.user = new User();
        this.bidderAuctions = FXCollections.observableArrayList();
        this.sellerAuctions = FXCollections.observableArrayList();
    }
}
