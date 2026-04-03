package a88.jbay.model.event;

import a88.jbay.model.UniqueID;
import a88.jbay.model.entity.item.Item;
import a88.jbay.model.entity.user.Seller;

import java.time.LocalDateTime;
import java.util.PriorityQueue;

public class Auction {
    private String id;
    private Item item;
    private Seller seller;
    private double startPrice;
    private double currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    //realtime
    private boolean active;
    private PriorityQueue<BidTransaction> bidPQ;

    public Auction(Item item, Seller seller, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = UniqueID.genAID();
        this.item = item;
        this.seller = seller;
        this.startPrice = item.getInitPrice();
        this.currentPrice = item.getInitPrice();
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getId() {
        return id;
    }
    public Item getItem() {
        return item;
    }

    public void end() {
        this.active = false;
    }

    public void addBid(BidTransaction bidTransaction) {
        if (!this.active) return;
        bidPQ.add(bidTransaction);
    }

    public void getMaxBid() {
        if (!this.active) return;
        if (bidPQ.isEmpty()) {
            return;
        }
        BidTransaction bidTransaction = bidPQ.poll();
        this.currentPrice = bidTransaction.getAmt();
    }
}
