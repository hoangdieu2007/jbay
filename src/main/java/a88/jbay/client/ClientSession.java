package a88.jbay.client;

import a88.jbay.model.entity.user.User;
import a88.jbay.model.event.Auction;

import java.util.ArrayList;
import java.util.List;

public class ClientSession {
    private static ClientSession instance;

    User user;
    List<Auction> sellerAuctions;
    List<Auction> buyerAuctions;

    private ClientSession() {
        user = new User();
        sellerAuctions = new ArrayList<>();
        buyerAuctions = new ArrayList<>();
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

    public void addSellerAuction(Auction auction) {
        sellerAuctions.add(auction);
    }

    public void addBuyerAuction(Auction auction) {
        buyerAuctions.add(auction);
    }
}
