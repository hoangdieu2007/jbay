package a88.jbay.model.event.auctionstate;

public class ClosedState implements AuctionState {
    @Override
    public void placeBid() {
        // cannot place bid
    }

    @Override
    public void start() {
        // cannot start
    }

    @Override
    public void end() {
        // cannot end
    }
}
