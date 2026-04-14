package a88.jbay.model.event.auctionstate;

import a88.jbay.model.event.Auction;
import a88.jbay.model.event.BidTransaction;

public class CanceledState implements AuctionState {
    @Override
    public void placeBid(Auction auction, BidTransaction bidTransaction) {
        throw new IllegalStateException("This Auction has already canceled, cannot place a bid!");
    }

    @Override
    public void start(Auction auction) {
        throw new IllegalStateException("This Auction has already canceled, cannot start auction!");
    }
    @Override
    public void end(Auction auction) {
        throw new IllegalStateException("This Auction has already canceled, cannot end auction!");
    }

    @Override
    public void cancel(Auction auction) {
        throw new IllegalStateException("This Auction has already canceled!");
    }
}
