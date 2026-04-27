package a88.jbay.system;

import a88.jbay.model.Observer;
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

    this is the ONLY observer of all auctions, it manages the current subscribers of each auction

    features:
        + add user id to the auction subscribers list, remove user id from the auction subscribers list (subscribe/unsubscribe)
        + manage each user session with an object output stream so the server can send response to the client (register/unregister)
 */

public class NotificationSystem implements Observer {
    private static NotificationSystem instance;
    private final Map<Integer, List<ObjectOutputStream>> userSessions = new ConcurrentHashMap<>();
    private final Map<Integer, Set<Integer>> auctionSubscribers = new ConcurrentHashMap<>();

    private NotificationSystem() {}

    public static synchronized NotificationSystem getInstance() {
        if (instance == null) {
            instance = new NotificationSystem();
        }
        return instance;
    }

    public void register(int userId, ObjectOutputStream out) {
        List<ObjectOutputStream> sessions =
                userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());

        sessions.add(out);
    }

    public void unregister(int userId, ObjectOutputStream out) {
        List<ObjectOutputStream> streams = userSessions.get(userId);
        if (streams != null) {
            streams.remove(out);
            if (streams.isEmpty()) {
                userSessions.remove(userId);
            }
        }
    }

    public void subscribe(int userId, int auctionId) {
        Set<Integer> subscribersForAuction = auctionSubscribers.get(auctionId);
        if (subscribersForAuction == null) {
            auctionSubscribers.putIfAbsent(auctionId, ConcurrentHashMap.newKeySet());
            subscribersForAuction = auctionSubscribers.get(auctionId);
        }
        subscribersForAuction.add(userId);
    }

    public void unsubscribe(int userId, int auctionId) {
        Set<Integer> subscribers = auctionSubscribers.get(auctionId);
        if (subscribers == null) {
            return;
        }

        subscribers.remove(userId);
        if (subscribers.isEmpty()) {
            auctionSubscribers.remove(auctionId);
        }
    }

    public void unsubscribeUserFromAllAuctions(int userId) {
        auctionSubscribers.forEach((auctionId, subscribers) -> {
            subscribers.remove(userId);
            if (subscribers.isEmpty()) {
                auctionSubscribers.remove(auctionId);
            }
        });
    }

    public void clearAuctionSubscribers(int auctionId) {
        auctionSubscribers.remove(auctionId);
    }

    @Override
    public void update(Auction auction) {
        Response response = new Response(true, "AUCTION_UPDATE", auction);
        Set<Integer> subscribers = auctionSubscribers.get(auction.getId());
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }

        subscribers.forEach(userId -> {
            List<ObjectOutputStream> streams = userSessions.get(userId);
            if (streams == null) {
                return;
            }
            for (ObjectOutputStream out : streams) {
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
