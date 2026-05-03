package a88.jbay.server;

import a88.jbay.model.entity.item.Item;
import a88.jbay.model.entity.user.User;
import a88.jbay.model.entity.user.role.ActionType;
import a88.jbay.model.event.Auction;
import a88.jbay.model.network.Request;
import a88.jbay.model.network.Response;
import a88.jbay.system.AuctionSystem;
import a88.jbay.system.UpdateSystem;
import a88.jbay.system.UserSystem;

import java.io.IOException;
import java.net.Socket;

/**
    code for handling individual clients request by calling the systems
    mainly responsible for processing requests and sending responses back immediately if needed
    not responsible for notifying the clients about changes, this is done by the notification system
 */

public class RequestHandler implements Runnable {
    private final ClientConnection clientConnection;
    private final UserSystem userSystem;
    private final AuctionSystem auctionSystem;

    public RequestHandler(Socket socket) throws IOException {
        this.clientConnection = new ClientConnection(socket);
        this.userSystem = UserSystem.getInstance();
        this.auctionSystem = AuctionSystem.getInstance();
    }

    //the handle loop
    @Override
    public void run() {
        try {
            clientConnection.runConnectionLoop(this);
        } finally {
            clientConnection.close();
        }
    }
    
    
    // directing request to respective handler
    public Response handleRequest(Request request) {
        System.out.println("Received request: " + request.getType().name());

        return switch (request.getType()) {
            case LOGIN -> handleLogin(request);
            case REGISTER -> handleRegister(request);
            case LOGOUT -> handleLogout(request);
            case BID -> handleBid(request);
            case SELL -> handleSell(request);
            case CANCEL -> handleCancel(request);
            case SUBSCRIBE_AUCTION -> handleSubscribeAuction(request);
            case UNSUBSCRIBE_AUCTION -> handleUnsubscribeAuction(request);
            case MISC -> handleMisc(request);
            default -> new Response(false, "Unsupported request", null);
        };
    }

    //handling login
    private Response handleLogin(Request request) {
        String username = (String) request.get("username");
        String password = (String) request.get("password");

        User user = userSystem.login(username, password);
        if (user != null) {
            //check if user is banned
            if (user.getRole().equals("BAN")) {
                return new Response(false, "LOGIN_BAN", null);
            }

            //register user session to the systems and update their auctions
            UserSystem.getInstance().registerUserSession(user.getId(), clientConnection);
            clientConnection.registerForNotifications(user.getId());
            UpdateSystem.getInstance().sendAllAuctionUpdates(user.getId());

            return new Response(true, "LOGIN_SUCCESS", user);
        }
        return new Response(false, "LOGIN_FAIL", null);
    }

    //handling register
    private Response handleRegister(Request request) {
        String username = (String) request.get("username");
        String password = (String) request.get("password");
        String role = "USER";

        if (userSystem.register(username, password, role)) {
            return new Response(true, "REGISTER_SUCCESS", null);
        }
        return new Response(false, "REGISTER_FAIL", null);
    }

    //handling bidding
    private Response handleBid(Request request) {
        String sessionId = (String) request.get("sessionId");
        User user = userSystem.getBySessionId(sessionId);
        if (user == null) return new Response(false, "INVALID_SESSION", null);
        if (user.can(ActionType.BID)) {
            boolean success = auctionSystem.placeBid(user.getId(), (Integer) request.get("auctionId"), (Double) request.get("amount"));
            return new Response(success, success ? "BID_SUCCESS" : "BID_FAIL", null);
        }
        return new Response(false, "BID_FAIL", null);
    }

    //handling selling and creating auction
    private Response handleSell(Request request) {
        String sessionId = (String) request.get("sessionId");
        User user = userSystem.getBySessionId(sessionId);
        if (user == null) return new Response(false, "INVALID_SESSION", null);
        if (user.can(ActionType.SELL)) {
            boolean success = auctionSystem.createAuction((Item)request.get("item"), user.getId(), (java.time.LocalDateTime) request.get("start"), (java.time.LocalDateTime) request.get("end"));
            return new Response(success, success ? "SELL_SUCCESS" : "SELL_FAIL", null);
        }
        return new Response(false, "SELL_FAIL", null);
    }

    //canceling auctions
    //ADMIN ONLY, REPORT IF CALLS FROM NORMAL USERS ALSO RETURN CANCEL_SUCCESS
    private Response handleCancel(Request request) {
        String sessionId = (String) request.get("sessionId");
        User user = userSystem.getBySessionId(sessionId);
        if (user == null) return new Response(false, "INVALID_SESSION", null);
        if (user.can(ActionType.CANCEL)) {
            //direct cancel to system
            boolean success = auctionSystem.cancelAuction((Integer) request.get("auctionId"));
            return new Response(success, success ? "CANCEL_SUCCESS" : "CANCEL_FAIL", null);
        }
        return new Response(false, "CANCEL_FAIL", null);
    }

    //subscribing to auctions
    //this is often automatically handled by the auction system upon bidding/selling a product
    //but separating this makes everything clear
    private Response handleSubscribeAuction(Request request) {
        String sessionId = (String) request.get("sessionId");
        User user = userSystem.getBySessionId(sessionId);
        if (user == null) return new Response(false, "INVALID_SESSION", null);

        Integer auctionId = (Integer) request.get("auctionId");
        if (auctionId == null || !auctionSystem.isAuctionActive(auctionId)) {
            return new Response(false, "AUCTION_NOT_FOUND", null);
        }

        Auction auction = auctionSystem.getAuctionById(auctionId);
        auction.subscribe(user.getId());
        return new Response(true, "SUBSCRIBE_AUCTION_SUCCESS", null);
    }

    //unsubscribing from auctions
    //also automatically handled by the auction system when an auction finishes
    private Response handleUnsubscribeAuction(Request request) {
        String sessionId = (String) request.get("sessionId");
        User user = userSystem.getBySessionId(sessionId);
        if (user == null) return new Response(false, "INVALID_SESSION", null);

        Integer auctionId = (Integer) request.get("auctionId");
        if (auctionId == null) {
            return new Response(false, "INVALID_AUCTION", null);
        }

        Auction auction = auctionSystem.getAuctionById(auctionId);
        auction.unsubscribe(user.getId());
        return new Response(true, "UNSUBSCRIBE_AUCTION_SUCCESS", null);
    }

    //handling logout
    //deletes session and logs out user, also removes all subscriptions
    private Response handleLogout(Request request) {
        String sessionId = (String) request.get("sessionId");
        User user = userSystem.getBySessionId(sessionId);
        if (user != null) {
            clientConnection.unregisterFromNotifications(user.getId());
            UpdateSystem.getInstance().unsubscribeConnectionFromAllAuctions(user.getId(), clientConnection);
            UserSystem.getInstance().removeUserSession(user.getId());
        }
        userSystem.logout(sessionId);
        return new Response(true, "LOGOUT_SUCCESS", null);
    }

    //misc commands
    private Response handleMisc(Request request) {
        switch ((String) request.get("command")) {
            case "ls-auction":
                return new Response(true, "LIST_AUCTION_SUCCESS", auctionSystem.listActiveAuctions());
            default:
                return new Response(false, "INVALID_MISC_COMMAND", null);
        }
    }

    //no cleanup needed since RequestHandler is now stateless
}