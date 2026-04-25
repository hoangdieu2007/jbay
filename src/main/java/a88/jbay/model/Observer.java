package a88.jbay.model;

import a88.jbay.model.event.Auction;

public interface Observer {
    void update (Auction auction);
}