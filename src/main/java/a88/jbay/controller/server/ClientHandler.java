package a88.jbay.controller.server;

import a88.jbay.model.entity.user.User;
import a88.jbay.model.entity.user.role.ActionType;
import a88.jbay.model.entity.user.role.Permission;
import a88.jbay.model.network.Request;
import a88.jbay.model.network.Response;
import a88.jbay.system.AuctionSystem;
import a88.jbay.system.UserSystem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/*
the code for handling individual clients
 */

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final UserSystem userSystem;
    private final AuctionSystem auctionSystem;

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
            while (true) {
                Request request = (Request) in.readObject();
                Response response = handleRequest(request);
                out.writeObject(response);
                out.flush();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
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
            default -> new Response(false, "Unsupported request", null);
        };
    }

    private Response handleLogin(Request request) {
        String username = (String) request.get("username");
        String password = (String) request.get("password");

        User user = userSystem.login(username, password);
        if (user != null) {
            return new Response(true, "LOGIN_SUCCESS", user);
        }
        return new Response(false, "INVALID_CREDENTIALS", null);
    }

    private Response handleRegister(Request request) {
        String username = (String) request.get("username");
        String password = (String) request.get("password");
        String role = (String) request.get("role");

        if (role == null) role = "bidder"; // Default role

        if (userSystem.register(username, password, role)) {
            return new Response(true, "REGISTER_SUCCESS", null);
        }
        return new Response(false, "USER_ALREADY_EXISTS", null);
    }

    private Response handleBid(Request request) {
        User user = userSystem.getBySessionId((String) request.get("sessionId"));
        if (user == null) return new Response(false, "INVALID_SESSION", null);
        if (user.can(ActionType.BID)) {
            boolean success = auctionSystem.placeBid(user.getId(), (Integer) request.get("auctionId"), (Double) request.get("amount"));
            return new Response(success, success ? "BID_SUCCESS" : "BID_FAIL", null);
        }
        return new Response(false, "BID_FAIL", null);
    }

    private Response handleSell(Request request) {
        User user = userSystem.getBySessionId((String) request.get("sessionId"));
        if (user == null) return new Response(false, "INVALID_SESSION", null);
        if (user.can(ActionType.SELL)) {
            //direct sell to system
        }
        return new Response(false, "BID_FAIL", null);
    }

    private Response handleCancel(Request request) {
        User user = userSystem.getBySessionId((String) request.get("sessionId"));
        if (user == null) return new Response(false, "INVALID_SESSION", null);
        if (user.can(ActionType.BID)) {
            //direct cancel to system
        }
        return new Response(false, "BID_FAIL", null);
    }

    private Response handleLogout(Request request) {
        String sessionId = (String) request.get("sessionId");
        userSystem.logout(sessionId);
        return new Response(true, "LOGOUT_SUCCESS", null);
    }
}