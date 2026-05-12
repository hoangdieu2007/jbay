package a88.jbay.common;

import a88.jbay.common.auction.Auction;

public interface Observer {
    void update (Auction auction);
}