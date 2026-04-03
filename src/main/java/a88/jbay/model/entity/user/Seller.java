package a88.jbay.model.entity.user;

import a88.jbay.model.entity.item.Item;
import a88.jbay.model.event.Auction;
import a88.jbay.model.system.AuctionSystem;

import java.time.LocalDateTime;
import java.util.HashMap;

public class Seller extends User {
    HashMap<String, Item> items;
    HashMap<String, Auction> auctions;

    public Seller() {
        super();
        this.type = "seller";
        items = new HashMap<>();
    }

    public void addItem(Item item) {
        this.items.put(item.getName(), item);
    }

    public void removeItem(String id) {
        this.items.remove(id);
    }

    public void createAuction(Item item, LocalDateTime start, LocalDateTime end) {
        Auction auction = AuctionSystem.getInstance().createAuction(item, this, start, end);
        this.auctions.put(auction.getId(), auction);
    }
}
