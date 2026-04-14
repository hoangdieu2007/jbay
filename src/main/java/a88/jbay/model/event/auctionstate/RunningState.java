package a88.jbay.model.event.auctionstate;

import a88.jbay.model.event.Auction;
import a88.jbay.model.event.BidTransaction;

public class RunningState implements AuctionState {
    @Override
    public void placeBid(Auction auction, BidTransaction bidTransaction) {
    }

    @Override
    public void start(Auction auction) {
        throw new IllegalStateException("This auction has already been started");
    }

    @Override
    public void end(Auction auction) {
        System.out.println("This auction has reached its end time!");
        auction.setAuctionState(new ClosedState());
    }
    @Override
    public void cancel(Auction auction) {
        System.out.println("Canceling auction...");
        auction.setAuctionState(new CanceledState());
    }
}
