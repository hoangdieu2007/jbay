package a88.jbay.controller.server;

import a88.jbay.model.entity.item.Item;
import a88.jbay.model.entity.user.User;
import a88.jbay.model.entity.user.role.ActionType;
import a88.jbay.model.network.Request;
import a88.jbay.model.network.Response;
import a88.jbay.system.AuctionSystem;
import a88.jbay.system.NotificationSystem;
import a88.jbay.system.UserSystem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/*
the code for handling individual clients request and talk back to the client
 */

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final UserSystem userSystem;
    private final AuctionSystem auctionSystem;
    private User currentUser;
    private ObjectOutputStream out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.userSystem = UserSystem.getInstance();
        this.auctionSystem = AuctionSystem.getInstance();
    }

    @Override
    public void run() {
        try (
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            this.out = out;
            while (true) {
                Request request = (Request) in.readObject();
                Response response = handleRequest(request);
                out.writeObject(response);
                out.flush();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            cleanupCurrentUserSession();
        }
    }

    private Response handleRequest(Request request) {
        return switch (request.getType()) {
            case LOGIN -> handleLogin(request);
            case REGISTER -> handleRegister(request);
            case LOGOUT -> handleLogout(request);
            case BID -> handleBid(request);
            case SELL -> handleSell(request);
            case CANCEL -> handleCancel(request);
            case SUBSCRIBE_AUCTION -> handleSubscribeAuction(request);
            case UNSUBSCRIBE_AUCTION -> handleUnsubscribeAuction(request);
            default -> new Response(false, "Unsupported request", null);
        };
    }

    private Response handleLogin(Request request) {
        String username = (String) request.get("username");
        String password = (String) request.get("password");

        User user = userSystem.login(username, password);
        if (user != null) {
            this.currentUser = user;
            NotificationSystem.getInstance().register(user.getId(), out);
            return new Response(true, "LOGIN_SUCCESS", user);
        }
        return new Response(false, "INVALID_CREDENTIALS", null);
    }

    private Response handleRegister(Request request) {
        String username = (String) request.get("username");
        String password = (String) request.get("password");
        String role = (String) request.get("role");

        if (role == null) role = "USER"; // Default role

        if (userSystem.register(username, password, role)) {
            return new Response(true, "REGISTER_SUCCESS", null);
        }
        return new Response(false, "USER_ALREADY_EXISTS", null);
    }

    private Response handleBid(Request request) {
        User user = this.currentUser;
        if (user == null) return new Response(false, "INVALID_SESSION", null);
        if (user.can(ActionType.BID)) {
            boolean success = auctionSystem.placeBid(user.getId(), (Integer) request.get("auctionId"), (Double) request.get("amount"));
            return new Response(success, success ? "BID_SUCCESS" : "BID_FAIL", null);
        }
        return new Response(false, "BID_FAIL", null);
    }

    private Response handleSell(Request request) {
        User user = this.currentUser;
        if (user == null) return new Response(false, "INVALID_SESSION", null);
        if (user.can(ActionType.SELL)) {
            boolean success = auctionSystem.createAuction((Item)request.get("item"), user.getId(), (java.time.LocalDateTime) request.get("start"), (java.time.LocalDateTime) request.get("end"));
            return new Response(success, success ? "SELL_SUCCESS" : "SELL_FAIL", null);
        }
        return new Response(false, "SELL_FAIL", null);
    }

    private Response handleCancel(Request request) {
        User user = this.currentUser;
        if (user == null) return new Response(false, "INVALID_SESSION", null);
        if (user.can(ActionType.BID)) {
            //direct cancel to system
        }
        return new Response(false, "BID_FAIL", null);
    }

    private Response handleSubscribeAuction(Request request) {
        User user = this.currentUser;
        if (user == null) return new Response(false, "INVALID_SESSION", null);

        Integer auctionId = (Integer) request.get("auctionId");
        if (auctionId == null || !auctionSystem.isAuctionActive(auctionId)) {
            return new Response(false, "AUCTION_NOT_FOUND", null);
        }

        NotificationSystem.getInstance().subscribe(user.getId(), auctionId);
        return new Response(true, "SUBSCRIBE_AUCTION_SUCCESS", null);
    }

    private Response handleUnsubscribeAuction(Request request) {
        User user = this.currentUser;
        if (user == null) return new Response(false, "INVALID_SESSION", null);

        Integer auctionId = (Integer) request.get("auctionId");
        if (auctionId == null) {
            return new Response(false, "INVALID_AUCTION", null);
        }

        NotificationSystem.getInstance().unsubscribe(user.getId(), auctionId);
        return new Response(true, "UNSUBSCRIBE_AUCTION_SUCCESS", null);
    }

    private Response handleLogout(Request request) {
        String sessionId = (String) request.get("sessionId");
        cleanupCurrentUserSession();
        userSystem.logout(sessionId);
        return new Response(true, "LOGOUT_SUCCESS", null);
    }

    private void cleanupCurrentUserSession() {
        if (currentUser == null) {
            return;
        }
        NotificationSystem.getInstance().unregister(currentUser.getId(), out);
        NotificationSystem.getInstance().unsubscribeUserFromAllAuctions(currentUser.getId());
        currentUser = null;
    }
}
