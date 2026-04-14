package a88.jbay.model.event.auctionstate;

import a88.jbay.model.event.Auction;
import a88.jbay.model.event.BidTransaction;

public class FinishedState implements AuctionState {
    @Override
    public void placeBid(Auction auction, BidTransaction bidTransaction) {
        throw new IllegalStateException("This Auction has finished!");
    }

    @Override
    public void start(Auction auction) {
        throw new IllegalStateException("This Auction has finished, cannot start auction!");
    }

    @Override
    public void end(Auction auction) {
        throw new IllegalStateException("This Auction has finished, cannot end auction!");
    }

    @Override
    public void cancel(Auction auction) {
        throw new IllegalStateException("This Auction has finished, cannot cancel auction!");
    }
}
