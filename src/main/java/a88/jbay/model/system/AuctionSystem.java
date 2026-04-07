package a88.jbay.model.system;

import a88.jbay.model.entity.item.Item;
import a88.jbay.model.entity.user.Seller;
import a88.jbay.model.event.Auction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

// server app code
// singleton auction system
// containing logics for handling auction events
public class AuctionSystem {
    private HashMap<String, Auction> auctions;

    private AuctionSystem() {
        auctions = new HashMap<>();
    }

    private static class SingletonHolder {
        private static final AuctionSystem INSTANCE = new AuctionSystem();
    }

    public static AuctionSystem getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public String createAuction(Item item, Seller seller, LocalDateTime start, LocalDateTime end) {
        //auction dao connection

        return null;
    }

    public String endAuction(String id) {
        //auctiondao connection

        return null;
    }
}
