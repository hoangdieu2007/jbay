package a88.jbay.model.event;

//all of the states:
//opening: auction can be viewed, no bidding can be done
//running: auction can be viewed and freely bidded
//finished: auction can be viewed, no bidding can be done, show winner, in the future: winner sees payment method
//paid: in the future: after verified transaction from both party, auction automatically switch to this state
//canceled: auction can be viewed, no bidding can be done, the winner section says this auction is cancelled
public enum AuctionState {
    OPENING,
    RUNNING,
    FINISHED,
    PAID,
    CANCELED;
}
