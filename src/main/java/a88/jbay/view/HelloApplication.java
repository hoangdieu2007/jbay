package a88.jbay.view;

import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.BidTransaction;
import a88.jbay.common.item.Item;
import a88.jbay.common.user.UserData;
import a88.jbay.di.ApplicationContext;
import a88.jbay.server.DatabaseController;
import a88.jbay.util.JBayLogger;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Scanner;

public class HelloApplication {
    private static JBayLogger logger;

    public static void main(String[] args) {
        LocalDateTime BASE_TIME = LocalDateTime.of(2026, 5, 15, 10, 0);
        Item item = new Item(0, "Test Item", "Generic", "A test item", 100.0);
        Auction auction = new Auction(0, item, new UserData(0, "seller", "USER", "idk"), BASE_TIME, BASE_TIME.plusDays(30));

        auction.start();

        BidTransaction transaction = new BidTransaction(5, "bidder", 150.0, BASE_TIME);
        auction.addBid(150.0, transaction);
    }
}
