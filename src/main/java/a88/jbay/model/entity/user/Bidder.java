package a88.jbay.model.entity.user;

import a88.jbay.model.entity.item.Item;
import a88.jbay.model.event.Auction;
import a88.jbay.model.event.BidTransaction;

import java.time.LocalDateTime;
import java.util.HashMap;

public class Bidder extends User {
    HashMap<String, Item> bids;
    HashMap<String, Auction> auctions;

    public Bidder() {
        bids = new HashMap<>();
        auctions = new HashMap<>();
    }

    public void placeBid(Auction auction, double price) {
        auction.placeBid(new BidTransaction(this.id, price, LocalDateTime.now()));
    }
}
