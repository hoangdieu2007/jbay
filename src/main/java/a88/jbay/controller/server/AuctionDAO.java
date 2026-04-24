package a88.jbay.controller.server;

import a88.jbay.model.entity.item.Item;
import a88.jbay.model.entity.user.Seller;
import a88.jbay.model.event.BidTransaction;

import java.sql.Connection;
import java.time.LocalDateTime;

// server app code, meant for auction sql data management
public class AuctionDAO {
    private static AuctionDAO instance;

    public static synchronized AuctionDAO getInstance() {
        if (instance == null) {
            instance = new AuctionDAO();
        }
        return instance;
    }

    // sell
    public String sell(Item item, Seller seller, LocalDateTime startTime, LocalDateTime endTime) {
        DatabaseController databaseController = new DatabaseController();
        Connection connection = databaseController.getConnection();

        return null;
    }

    // bid

}
