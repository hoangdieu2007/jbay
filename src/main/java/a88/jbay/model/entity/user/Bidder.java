package a88.jbay.model.entity.user;

import a88.jbay.model.entity.item.Item;
import a88.jbay.model.event.Auction;

import java.util.HashMap;

public class Bidder extends User {
    HashMap<String, Item> bids;
    HashMap<String, Auction> activeAuctions;

    public void placeBid(Auction auction, double amount) {
        //send placeBid request to Auction
    }

    public void joinAuctionById(long id) {
        //change auction participation state to True in activeAuctions
    }

    public void leaveAuctionById(long id){
        //change auction participation state to False in activeAuctions
    }

    public void setChanged() {}

    public void notifyObservers() {}
}
