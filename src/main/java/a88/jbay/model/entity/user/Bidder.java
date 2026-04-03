package a88.jbay.model.entity.user;

import a88.jbay.model.entity.item.Item;
import a88.jbay.model.event.Auction;

import java.util.HashMap;

public class Bidder {
    HashMap<String, Item> bids;
    HashMap<String, Auction> auctions;
}
