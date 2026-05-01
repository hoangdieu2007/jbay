package a88.jbay.client;

import a88.jbay.controller.client.BidderItemCardController;
import a88.jbay.controller.client.SellerItemCardController;
import a88.jbay.model.entity.user.User;
import a88.jbay.model.event.Auction;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;
import java.util.TreeMap;

public class ClientSession {
    private static ClientSession instance;
    private SellerItemCardController controller;

    private User user;
    private ObservableMap<Integer, Auction> sellerAuctions;
    private ObservableMap<Integer, Auction> bidderAuctions;

    private ClientSession() {
        user = new User();
        sellerAuctions = FXCollections.observableMap(new TreeMap<>());
        bidderAuctions = FXCollections.observableMap(new TreeMap<>());

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

        //Làm sac Map nhưng giữ nguyên thực thể và không làm đứt kết nối với Listener
        if (this.bidderAuctions != null) {
            this.bidderAuctions.clear();
        }

        if (this.sellerAuctions != null) {
            this.sellerAuctions.clear();
        }

        System.out.println("Session has been cleared");
    }
}
