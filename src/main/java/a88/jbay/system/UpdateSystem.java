package a88.jbay.system;

import a88.jbay.server.ClientConnection;
import a88.jbay.model.event.Auction;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
    simplified notification system for the server
    manages client connections and broadcasts notifications
    delegates actual networking to ClientConnection
 */

public class UpdateSystem {
    private static UpdateSystem instance;
    private final Map<Integer, List<ClientConnection>> userConnections = new ConcurrentHashMap<>();

    private UpdateSystem() {}

    public static synchronized UpdateSystem getInstance() {
        if (instance == null) {
            instance = new UpdateSystem();
        }
        return instance;
    }

    /**
     * registers a client connection for notifications
     * @param userId the user id
     * @param connection the client connection
     */
    public synchronized void registerConnection(int userId, ClientConnection connection) {
        userConnections.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>())
                      .add(connection);
    }

    /**
     * unregisters a client connection from notifications
     * @param userId the user id
     * @param connection the client connection
     */
    public synchronized void unregisterConnection(int userId, ClientConnection connection) {
        List<ClientConnection> connections = userConnections.get(userId);
        if (connections != null) {
            connections.remove(connection);
            if (connections.isEmpty()) {
                userConnections.remove(userId);
            }
        }
    }

    /**
     * broadcasts an auction update to all subscribed users
     * @param auction the auction to broadcast
     * @param subscriberIds the user ids to notify
     */
    public void broadcastAuctionUpdate(Auction auction, Set<Integer> subscriberIds) {
        if (subscriberIds == null || subscriberIds.isEmpty()) {
            return;
        }

        subscriberIds.forEach(userId -> {
            List<ClientConnection> connections = userConnections.get(userId);
            if (connections != null) {
                connections.forEach(connection -> {
                    if (connection.isConnected()) {
                        connection.sendAuctionUpdate(auction);
                    }
                });
            }
        });
    }

    /**
     * sends auction list updates to a specific user
     * @param userId the user id
     * @param auctionType type of auction list (ACTIVE, SELLER, BIDDER)
     */
    public void sendAuctionListUpdate(int userId, String auctionType) {
        List<ClientConnection> connections = userConnections.get(userId);
        if (connections == null) {
            return;
        }

        switch (auctionType) {
            case "ACTIVE" -> connections.forEach(conn -> 
                conn.sendAuctionList("ACTIVE", AuctionSystem.getInstance().getActiveAuctionListExceptForSeller(userId)));
            case "SELLER" -> connections.forEach(conn -> 
                conn.sendAuctionList("SELLER", AuctionSystem.getInstance().getAuctionsBySellerId(userId)));
            case "BIDDER" -> connections.forEach(conn -> 
                conn.sendAuctionList("BIDDER", AuctionSystem.getInstance().getAuctionsByWinnerId(userId)));
        }
    }

    /**
     * sends all auction list updates to a user (for initial login)
     * @param userId the user id
     */
    public void sendAllAuctionUpdates(int userId) {
        sendAuctionListUpdate(userId, "ACTIVE");
        sendAuctionListUpdate(userId, "SELLER");
        sendAuctionListUpdate(userId, "BIDDER");
    }

    /**
     * unsubscribes a specific connection from all auction notifications
     * @param userId the user id
     * @param connection the specific connection to unsubscribe
     */
    public void unsubscribeConnectionFromAllAuctions(int userId, ClientConnection connection) {
        // Only remove this specific connection, not all connections for the user
        unregisterConnection(userId, connection);
    }

    /**
     * cleans up all connections during graceful shutdown
     */
    public synchronized void cleanupAllConnections() {
        userConnections.clear();
        System.out.println("All client connections cleaned up");
    }
}
