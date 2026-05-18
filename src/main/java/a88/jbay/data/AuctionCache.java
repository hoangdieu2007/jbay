package a88.jbay.data;

import a88.jbay.common.auction.Auction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionCache {

    private final Map<Integer, Auction> auctions = new ConcurrentHashMap<>();

    public void store(Auction auction) {
        auctions.put(auction.getId(), auction);
    }

    public void remove(int auctionId) {
        auctions.remove(auctionId);
    }

    public Auction get(int auctionId) {
        return auctions.get(auctionId);
    }

    public boolean contains(int auctionId) {
        return auctions.containsKey(auctionId);
    }

    public Collection<Auction> getAll() {
        return auctions.values();
    }

    public List<Auction> getAllAsList() {
        return new ArrayList<>(auctions.values());
    }

    public List<Auction> getAllExceptSeller(String sellerName) {
        List<Auction> result = new ArrayList<>();
        for (Auction auction : auctions.values()) {
            if (!auction.getSellerName().equals(sellerName)) {
                result.add(auction);
            }
        }
        return result;
    }

    public String summarize() {
        StringBuilder sb = new StringBuilder();
        for (Auction auction : auctions.values()) {
            sb.append(auction).append("\n\n");
        }
        return sb.toString();
    }
}