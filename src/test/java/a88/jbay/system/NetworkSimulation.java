package a88.jbay.system;

import a88.jbay.common.auction.Auction;
import a88.jbay.common.item.Item;
import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import a88.jbay.common.network.Response;
import a88.jbay.common.user.User;
import a88.jbay.server.RequestHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory network simulation facility for testing.
 * <p>
 * Simulates a full client-server request cycle without real TCP sockets.
 * Manages multiple virtual clients with independent sessions.
 * <p>
 * Usage:
 * <pre>{@code
 * NetworkSimulation sim = new NetworkSimulation(handler);
 * SimulatedClient alice = sim.createClient();
 * sim.register(alice, "alice", "pass");
 * sim.login(alice, "alice", "pass");
 * sim.bid(alice, auctionId, 150.0);
 * }</pre>
 */
public class NetworkSimulation {

    private final RequestHandler handler;
    private int nextUserId = 1000;

    public NetworkSimulation(RequestHandler handler) {
        this.handler = handler;
    }

    public SimulatedClient createClient() {
        return new SimulatedClient();
    }

    public SimulatedClient createClient(int userId) {
        return new SimulatedClient(userId);
    }

    public Response send(SimulatedClient client, Request request) {
        if (client.sessionId != null) {
            request.put("sessionId", client.sessionId);
        }
        Response response = handler.handleRequest(request);
        if (response != null && "LOGIN_SUCCESS".equals(response.getMessage())) {
            User user = (User) response.getPayload();
            if (user != null) {
                client.sessionId = user.getSessionId();
                client.userId = user.getId();
                client.username = user.getUsername();
                client.role = user.getRole();
            }
        }
        if (response != null && "LOGOUT_SUCCESS".equals(response.getMessage())) {
            client.sessionId = null;
        }
        return response;
    }

    public Response register(SimulatedClient client, String username, String password) {
        return send(client, new Request(RequestType.REGISTER)
                .put("username", username)
                .put("password", password));
    }

    public Response login(SimulatedClient client, String username, String password) {
        return send(client, new Request(RequestType.LOGIN)
                .put("username", username)
                .put("password", password));
    }

    public Response ping(SimulatedClient client) {
        return send(client, new Request(RequestType.PING));
    }

    public Response logout(SimulatedClient client) {
        return send(client, new Request(RequestType.LOGOUT));
    }

    public Response bid(SimulatedClient client, int auctionId, double amount) {
        return send(client, new Request(RequestType.BID)
                .put("auctionId", auctionId)
                .put("amount", amount));
    }

    public Response autoBid(SimulatedClient client, int auctionId, double maxAmount, double increment) {
        return send(client, new Request(RequestType.AUTO_BID)
                .put("auctionId", auctionId)
                .put("max_amount", maxAmount)
                .put("increment", increment));
    }

    public Response cancelAutoBid(SimulatedClient client, int auctionId) {
        return send(client, new Request(RequestType.CANCEL_AUTO_BID)
                .put("auctionId", auctionId));
    }

    public Response sell(SimulatedClient client, Item item, LocalDateTime start, LocalDateTime end) {
        return send(client, new Request(RequestType.SELL)
                .put("item", item)
                .put("start", start)
                .put("end", end));
    }

    public Response sell(SimulatedClient client, Item item, LocalDateTime start, LocalDateTime end, double minIncrement) {
        return send(client, new Request(RequestType.SELL)
                .put("item", item)
                .put("start", start)
                .put("end", end)
                .put("minIncrement", minIncrement));
    }

    public Response pay(SimulatedClient client, int auctionId) {
        return send(client, new Request(RequestType.PAY)
                .put("auctionId", auctionId));
    }

    public Response confirmPayment(SimulatedClient client, int auctionId) {
        return send(client, new Request(RequestType.CONFIRM_PAYMENT)
                .put("auctionId", auctionId));
    }

    public Response cancelAuction(SimulatedClient client, int auctionId) {
        return send(client, new Request(RequestType.CANCEL)
                .put("auctionId", auctionId));
    }

    public Response getAuctions(SimulatedClient client) {
        return send(client, new Request(RequestType.GET_AUCTIONS)
                .put("userId", client.userId));
    }

    public Response getUsers(SimulatedClient client) {
        return send(client, new Request(RequestType.GET_USERS));
    }

    public Response banUser(SimulatedClient client, int targetUserId) {
        return send(client, new Request(RequestType.BAN)
                .put("userId", targetUserId));
    }

    public Response unbanUser(SimulatedClient client, int targetUserId) {
        return send(client, new Request(RequestType.BAN)
                .put("userId", targetUserId)
                .put("action", "UNBAN"));
    }

    public Response subscribeAuction(SimulatedClient client, int auctionId) {
        return send(client, new Request(RequestType.SUBSCRIBE_AUCTION)
                .put("auctionId", auctionId));
    }

    public Response unsubscribeAuction(SimulatedClient client, int auctionId) {
        return send(client, new Request(RequestType.UNSUBSCRIBE_AUCTION)
                .put("auctionId", auctionId));
    }

    public Response misc(SimulatedClient client, String command) {
        return send(client, new Request(RequestType.MISC)
                .put("command", command));
    }

    public Response rawRequest(SimulatedClient client, RequestType type, Map<String, Object> data) {
        Request req = new Request(type);
        if (data != null) data.forEach(req::put);
        return send(client, req);
    }

    public static class SimulatedClient {
        String sessionId;
        int userId;
        String username;
        String role;

        SimulatedClient() {
        }

        SimulatedClient(int userId) {
            this.userId = userId;
        }

        public String getSessionId() { return sessionId; }
        public int getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
        public boolean isLoggedIn() { return sessionId != null; }
    }
}
