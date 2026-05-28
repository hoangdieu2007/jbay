package a88.jbay.system.update;

import a88.jbay.common.auction.Auction;
import a88.jbay.common.network.Response;
import a88.jbay.di.ApplicationContext;

import java.util.Set;

/**
 * Sends prepared update responses to connected clients.
 */
public class UpdateSystem {

    private final ConnectionSystem connectionSystem;

    public UpdateSystem(ConnectionSystem connectionSystem) {
        this.connectionSystem = connectionSystem;
    }

    public static UpdateSystem getInstance() {
        return ApplicationContext.getInstance().getDependency(UpdateSystem.class);
    }

    /**
     * Notify all subscribers of an auction update.
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
     * Broadcast an auction update to all connected users.
     */
    public void broadcastAuctionUpdate(Auction auction) {
        Response response = new Response(
                true,
                "AUCTION_UPDATE",
                auction
        );

        connectionSystem.broadcast(response);
    }

    public void sendToUser(int userId, Response response) {
        connectionSystem.sendToUser(userId, response);
    }

    public void sendToUsers(Set<Integer> userIds, Response response) {
        connectionSystem.sendToUsers(userIds, response);
    }

    public void broadcastToAll(Response response) {
        connectionSystem.broadcast(response);
    }
}
