package a88.jbay.model.system;

import a88.jbay.model.entity.item.Item;
import a88.jbay.model.entity.user.Seller;
import a88.jbay.model.event.Auction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

public class AuctionSystem {
    private HashMap<String, Auction> auctions;
    private static AuctionSystem instance;

    private AuctionSystem() {
        auctions = new HashMap<>();
    }

    public static class SingletonHolder {
        private static final AuctionSystem INSTANCE = new AuctionSystem();
    }

    public static AuctionSystem getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public Auction createAuction(Item item, Seller seller, LocalDateTime start, LocalDateTime end) {
        Auction auction = new Auction(item, seller, start, end);
        this.auctions.put(auction.getId(), auction);
        return auction;
    }

    public void endAuction(String id) {
        Auction auction = auctions.get(id);
        if (auction != null) {
            auction.end();
        }
    }
}
