package a88.jbay.model.event;

import a88.jbay.model.UniqueID;
import a88.jbay.model.entity.item.Item;
import a88.jbay.model.entity.user.Seller;
import a88.jbay.model.event.auctionstate.AuctionState;
import a88.jbay.model.event.auctionstate.OpeningState;

import java.time.LocalDateTime;
import java.util.Observer;
import java.util.PriorityQueue;

public class Auction implements Subject {
    private String id;
    private Item item;
    private Seller seller;
    private double startPrice;
    private double currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    //realtime
    private AuctionState auctionState;
    private PriorityQueue<BidTransaction> bidPQ;

    public Auction(Item item, Seller seller, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = UniqueID.genAID();
        this.item = item;
        this.seller = seller;
        this.startPrice = item.getInitPrice();
        this.currentPrice = item.getInitPrice();
        this.startTime = startTime;
        this.endTime = endTime;

        this.auctionState = new OpeningState();
        this.bidPQ = new PriorityQueue<>();
    }

    public String getId() {
        return id;
    }
    public Item getItem() {
        return item;
    }

    public void start() {this.auctionState.start();}
    public void end() {
        this.auctionState.end();
    }

    public void registerObserver(Observer observer) {
        //add observer
    }

    public void removeObserver(Observer observer) {
        //remove observer
    }

    public void notifyObservers() {
        //notify observers
    }

    public void placeBid(BidTransaction bidTransaction) {
        // some logic stuff
        this.auctionState.placeBid();
        if (!this.bidPQ.isEmpty() && this.currentPrice < this.bidPQ.peek().getAmt()) this.currentPrice = this.bidPQ.poll().getAmt();
    }
}
