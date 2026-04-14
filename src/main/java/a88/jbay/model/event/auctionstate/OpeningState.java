package a88.jbay.model.event.auctionstate;

import a88.jbay.model.event.Auction;
import a88.jbay.model.event.BidTransaction;

public class OpeningState implements AuctionState {
    @Override
    public void placeBid(Auction auction, BidTransaction bidTransaction) {
        throw new IllegalStateException("This Auction has not started yet, cannot place a bid!");
    }

    @Override




















































































































































































    public void start(Auction auction) {
        System.out.println("Opening auction...");
        auction.setAuctionState(new RunningState());
    }

    @Override
    public void end(Auction auction) {
        throw new IllegalStateException("This Auction has not reached its end time yet!");
    }

    @Override
    public void cancel(Auction auction) {
        System.out.println("Canceling auction...");
        auction.setAuctionState(new CanceledState());
    }
}
