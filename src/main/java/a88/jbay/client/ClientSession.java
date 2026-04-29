package a88.jbay.client;

import a88.jbay.model.entity.user.User;
import a88.jbay.model.event.Auction;
import javafx.collections.FXCollections;

import java.util.List;

public class ClientSession {
    private static ClientSession instance;

    private User user;
    private List<Auction> sellerAuctions;
    private List<Auction> bidderAuctions;

    private ClientSession() {
        user = new User();
        sellerAuctions = FXCollections.observableArrayList();
        bidderAuctions = FXCollections.observableArrayList();
    }

    public synchronized static ClientSession getInstance() {
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

    public List<Auction> getSellerAuctions() {
        return sellerAuctions;
    }

    public List<Auction> getBidderAuctions() {
        return bidderAuctions;
    }

    public void addSellerAuction(Auction auction) {
        sellerAuctions.add(auction);
    }

    public void addBidderAuction(Auction auction) {
        bidderAuctions.add(auction);
    }

    // after each logout, call this to erase session
    public void resetSession() {
        this.user = new User();
        this.bidderAuctions = FXCollections.observableArrayList();
        this.sellerAuctions = FXCollections.observableArrayList();
    }
}
