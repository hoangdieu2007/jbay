package a88.jbay.system.update;

import a88.jbay.common.auction.Auction;
import a88.jbay.common.network.Response;
import a88.jbay.server.ClientConnection;
import a88.jbay.system.AuctionSystem;
import a88.jbay.util.JBayLogger;
import a88.jbay.di.ApplicationContext;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
    the notification system for the server

    simplified version - manages user sessions and sends notifications
    auction subscriber management is now handled by individual Auction objects

    features:
        + manage each user session with an object output stream so the server can send response to the client (register/unregister)
 */

public class ConnectionSystem {

    public static ConnectionSystem getInstance() {
        return ApplicationContext.getInstance().getDependency(ConnectionSystem.class);
    }

    private final Map<Integer, Set<ClientConnection>> connections =
            new ConcurrentHashMap<>();
    private final ExecutorService senderExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public Map<Integer, Set<ClientConnection>> getConnections() {
        return Collections.unmodifiableMap(connections);
    }

    public void register(ClientConnection connection) {
        int userId = connection.getUserCache().getId();
        if (userId <= 0) {
            return;
        }
        connections
                .computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>())
                .add(connection);
    }

    public void unregister(ClientConnection connection) {
        int userId = connection.getUserCache().getId();
        if (userId <= 0) {
            return;
        }
        Set<ClientConnection> userConnections = connections.get(userId);

        if (userConnections != null) {
            userConnections.remove(connection);

            if (userConnections.isEmpty()) {
                connections.remove(userId);
            }
        }
    }

    public void unregister(int userId) {
        connections.remove(userId);
    }

    public void sendToUser(int userId, Response response) {
        Set<ClientConnection> userConnections = connections.get(userId);

        if (userConnections == null || response == null) {
            return;
        }

        for (ClientConnection connection : userConnections) {
            senderExecutor.submit(() -> {
                if (!connection.send(response)) {
                    unregister(connection);
                    connection.close();
                }
            });
        }
    }

    public void sendToUsers(Set<Integer> userIds, Response response) {
        for (Integer userId : userIds) {
            sendToUser(userId, response);
        }
    }

    public void broadcast(Response response) {
        connections.keySet()
                .forEach(userId -> sendToUser(userId, response));
    }
}
