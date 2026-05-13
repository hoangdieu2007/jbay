package a88.jbay.system.update;

import a88.jbay.common.auction.Auction;
import a88.jbay.common.network.Response;
import a88.jbay.di.ApplicationContext;
import a88.jbay.repository.AuctionRepository;

import java.util.Set;

/**
 * high-level notification orchestration service.
 *
 * responsibilities:
 * - prepare responses
 * - determine recipients
 * - coordinate updates
 *
 * this class contains BUSINESS notification workflows.
 *
 * actual message delivery is delegated to ConnectionSystem.
 */
public class UpdateSystem {

    private final ConnectionSystem connectionSystem;
    private final AuctionRepository auctionRepository;

    public UpdateSystem(
            ConnectionSystem connectionSystem,
            AuctionRepository auctionRepository
    ) {
        this.connectionSystem = connectionSystem;
        this.auctionRepository = auctionRepository;
    }

    public static UpdateSystem getInstance() {
        return ApplicationContext.getInstance().getDependency(UpdateSystem.class);
    }

    /**
     * notify all subscribers of an auction update.
     */
    public void notifyAuctionSubscribers(Auction auction) {
        Response response = new Response(
                true,
                "AUCTION_UPDATE_NOTIFY",
                auction
        );

        Set<Integer> subscribers = auction.getSubscribers();

        connectionSystem.sendToUsers(subscribers, response);
    }

    /**
     * broadcast auction update to all users.
     */
    public void broadcastAuctionUpdate(Auction auction) {
        Response response = new Response(
                true,
                "AUCTION_UPDATE",
                auction
        );

        connectionSystem.broadcast(response);
    }

    /**
     * send seller auctions to a user.
     */
    public void updateSellerAuctions(int userId) {
        Response response = new Response(
                true,
                "SELLER_AUCTION_LIST",
                auctionRepository.getAuctionsBySellerId(userId)
        );

        connectionSystem.sendToUser(userId, response);
    }

    /**
     * send won auctions to a user.
     */
    public void updateBidderAuctions(int userId) {
        Response response = new Response(
                true,
                "BIDDER_AUCTION_LIST",
                auctionRepository.getAuctionsByWinnerId(userId)
        );

        connectionSystem.sendToUser(userId, response);
    }

    /**
     * send active auctions excluding seller-owned auctions.
     */
    public void updateActiveAuctions(int userId) {
        Response response = new Response(
                true,
                "ACTIVE_AUCTION_LIST",
                auctionRepository.getActiveAuctionListExceptForSeller(userId)
        );

        connectionSystem.sendToUser(userId, response);
    }

    /**
     * refresh all auction-related views for a user.
     */
    public void updateAllAuctions(int userId) {
        updateActiveAuctions(userId);
        updateBidderAuctions(userId);
        updateSellerAuctions(userId);
    }

    /**
     * remove a user from all auction subscriptions.
     */
    public void unsubscribeUserFromAllAuctions(int userId) {
        for (Auction auction : auctionRepository.getAllActiveAuctions()) {
            auction.unsubscribe(userId);
        }
    }
}