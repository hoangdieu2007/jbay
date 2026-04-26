package a88.jbay.system;

import a88.jbay.model.network.Response;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationSystem {
    private static NotificationSystem instance;
    private final ConcurrentHashMap<Integer, ObjectOutputStream> activeSessions = new ConcurrentHashMap<>();

    public synchronized static NotificationSystem getInstance() {
        if (instance == null) {
            instance = new NotificationSystem();
        }
        return instance;
    }

    public void register(int userId, ObjectOutputStream out) {
        activeSessions.put(userId, out);
    }

    public void unregister(int userId) {
        activeSessions.remove(userId);
    }

    public void broadcast(Response response) {
        activeSessions.forEach((userId, out) -> {
            synchronized (out) {
                try {
                    out.writeObject(response);
                    out.flush();
                } catch (IOException e) {
                    unregister(userId);
                }
            }
        });
    }
}