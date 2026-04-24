package a88.jbay.model.event.auctionstate;

import a88.jbay.model.event.Auction;
import a88.jbay.model.event.BidTransaction;

public interface AuctionState {
    void placeBid(Auction auction, BidTransaction bidTransaction);
    void start(Auction auction);
    void end(Auction auction);
    void cancel(Auction auction);
}
