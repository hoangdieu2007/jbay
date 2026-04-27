package a88.jbay.system;

import a88.jbay.model.event.Auction;
import a88.jbay.model.network.Response;

import java.io.IOException;
import java.io.ObjectOutputStream;
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
        + send notifications to specific users based on auction subscriber lists
 */

public class NotificationSystem {
    private static NotificationSystem instance;
    private final Map<Integer, List<ObjectOutputStream>> userSessions = new ConcurrentHashMap<>();

    private NotificationSystem() {}

    public static synchronized NotificationSystem getInstance() {
        if (instance == null) {
            instance = new NotificationSystem();
        }
        return instance;
    }

    //register user session, coupled with the user output stream
    public void register(int userId, ObjectOutputStream out) {
        List<ObjectOutputStream> sessions =
                userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());

        sessions.add(out);
    }

    //unregister user session, remove the user output stream from the list
    public void unregister(int userId, ObjectOutputStream out) {
        List<ObjectOutputStream> streams = userSessions.get(userId);
        if (streams != null) {
            streams.remove(out);
            if (streams.isEmpty()) {
                userSessions.remove(userId);
            }
        }
    }

    //unsub from all auctions
    public void unsubscribeUserFromAllAuctions(int userId) {
        AuctionSystem.getInstance().getActiveAuctions().forEach(auction -> auction.unsubscribe(userId));
    }

    //new notification method - receives subscriber list from Auction
    public void notifySubscribers(Auction auction, Set<Integer> subscriberIds) {
        Response response = new Response(true, "AUCTION_UPDATE", auction);
        
        if (subscriberIds == null || subscriberIds.isEmpty()) {
            return;
        }

        subscriberIds.forEach(userId -> {
            List<ObjectOutputStream> streams = userSessions.get(userId);
            if (streams == null) {
                return;
            }
            for (ObjectOutputStream out : streams) {
                //synchronized to prevent concurrent modification exception
                synchronized (out) {
                    try {
                        out.writeObject(response);
                        out.flush();
                    } catch (IOException e) {
                        unregister(userId, out);
                    }
                }
            }
        });
    }
}
