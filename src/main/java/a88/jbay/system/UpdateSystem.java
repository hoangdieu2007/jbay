package a88.jbay.system;

import a88.jbay.model.event.Auction;
import a88.jbay.model.network.Response;
import a88.jbay.server.ClientConnection;
import a88.jbay.util.JBayLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
    the notification system for the server

    simplified version - manages user sessions and sends notifications
    auction subscriber management is now handled by individual Auction objects

    features:
        + manage each user session with an object output stream so the server can send response to the client (register/unregister)
 */

public class UpdateSystem {
    private static UpdateSystem instance;
    private final Map<Integer, List<ClientConnection>> connections = new ConcurrentHashMap<>();
    private final JBayLogger logger;

    private UpdateSystem() {
        this.logger = JBayLogger.getInstance();
    }

    public static synchronized UpdateSystem getInstance() {
        if (instance == null) {
            instance = new UpdateSystem();
        }
        return instance;
    }

    public Map<Integer, List<ClientConnection>> getConnections() {
        return connections;
    }

    //register client connection
    public void register(ClientConnection connection) {
        logger.debug("Registering connection: " + connection.getConnectionId());
        connections.computeIfAbsent(connection.getUserCache().getId(), k -> new ArrayList<>()).add(connection);
        logger.debug("Total registered connections: " + connections.size());
    }

    //unregister client connection
    public void unregister(ClientConnection connection) {
        logger.debug("Unregistering connection: " + connection.getConnectionId());
        connections.get(connection.getUserCache().getId()).remove(connection);
        logger.debug("Remaining connections: " + connections.size());
    }

    public void unregister(int userId) {
        logger.debug("Unregistering user: " + userId);
        connections.remove(userId);
        logger.debug("Remaining connections: " + connections.size());
    }

    //unsub from all auctions
    public void unsubscribeUserFromAllAuctions(int userId) {
        logger.info("Unsubscribing user from all auctions: " + userId);
        AuctionSystem.getInstance().getActiveAuctionList().forEach(auction -> auction.unsubscribe(userId));
    }

    //new notification method - receives subscriber list from Auction
    public void notifySubscribers(Auction auction, Set<Integer> subscriberIds) {
        Response response = new Response(true, "AUCTION_UPDATE_NOTIFY", auction);
        
        // Update all active connections
        connections.forEach((userId, connections) -> {
            if (subscriberIds.contains(userId)) {
                try {
                    connections.forEach(connection -> connection.send(response));
                } catch (Exception e) {
                    logger.error("Failed to send update: " + e.getMessage(), e);
                }
            }
        });
    }

    public void updateAuctionToAllUsers(Auction auction) {
        Response response = new Response(true, "AUCTION_UPDATE", auction);
        updateAllUsers(response);
    }

    //update seller auction list to user by id
    public void updateSellerAuctions(int userId) {
        Response response = new Response(true, "SELLER_AUCTION_LIST", AuctionSystem.getInstance().getAuctionsBySellerId(userId));

        updateByUserId(userId, response);
    }

    //update won auction list to user by id
    public void updateBidderAuctions(int userId) {
        Response response = new Response(true, "BIDDER_AUCTION_LIST", AuctionSystem.getInstance().getAuctionsByWinnerId(userId));

        updateByUserId(userId, response);
    }

    //update active auction list to user by id
    public void updateActiveAuctions(int userId) {
        Response response = new Response(true, "ACTIVE_AUCTION_LIST", AuctionSystem.getInstance().getActiveAuctionListExceptForSeller(userId));

        updateByUserId(userId, response);
    }

    //update all auctions for an userid
    public void updateAllAuctions(int userId) {
        updateActiveAuctions(userId);
        updateBidderAuctions(userId);
        updateSellerAuctions(userId);
    }

    //update for a specific user
    public void updateByUserId(int userId, Response response) {
        connections.get(userId).forEach(connection -> {
            try {
                connection.send(response);
            } catch (Exception e) {
                logger.error("Failed to send update: " + e.getMessage(), e);
            }
        });
    }

    //update all users
    public void updateAllUsers(Response response) {
        connections.forEach((userId, connections) -> {
            try {
                connections.forEach(connection -> connection.send(response));
            } catch (Exception e) {
                logger.error("Failed to send update: " + e.getMessage(), e);
            }
        });
    }
}
