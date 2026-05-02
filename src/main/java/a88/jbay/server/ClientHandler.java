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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
    the code for handling individual clients request by calling the systems
    mainly responsible for processing requests and sending responses back immediately if needed
    not responsible for notifying the clients about the changes, this is done by the notification system
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
        this.currentUser = new User();
        this.out = null;
    }

    //the handle loop
    @Override
    public void run() {
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            this.out = out;
            
            while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                try {
                    Request request = (Request) in.readObject();
                    if (request == null) break;
                    
                    Response response = handleRequest(request);

                    //prevent crash when update and response sends at the same time
                    synchronized (out) {
                        out.writeObject(response);
                        out.flush();
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("Invalid request object received: " + e.getMessage());
                    break;
                } catch (IOException e) {
                    if (!socket.isClosed()) {
                        System.err.println("Client connection error: " + e.getMessage());
                    }
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to establish client connection: " + e.getMessage());
        } finally {
            cleanupCurrentUserSession();
            closeResources(out, in, socket);
        }
    }
    
    private void closeResources(ObjectOutputStream out, ObjectInputStream in, Socket socket) {
        try {
            if (out != null) out.close();
        } catch (IOException e) {
            System.err.println("Error closing output stream: " + e.getMessage());
        }
        try {
            if (in != null) in.close();
        } catch (IOException e) {
            System.err.println("Error closing input stream: " + e.getMessage());
        }
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }

    // directing request to respective handler
    private Response handleRequest(Request request) {
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
            this.currentUser = user;

            //check if user is banned
            if (user.getRole().equals("BAN")) {
                return new Response(false, "LOGIN_BAN", null);
            }

            //register user session to the systems and update their auctions
            UserSystem.getInstance().addActiveUser(user.getId(), user);
            UpdateSystem.getInstance().register(user.getId(), out);
            UpdateSystem.getInstance().updateAllAuctions(user.getId());

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
        User user = this.currentUser;
        if (user == null) return new Response(false, "INVALID_SESSION", null);
        if (user.can(ActionType.BID)) {
            boolean success = auctionSystem.placeBid(user.getId(), (Integer) request.get("auctionId"), (Double) request.get("amount"));
            return new Response(success, success ? "BID_SUCCESS" : "BID_FAIL", null);
        }
        return new Response(false, "BID_FAIL", null);
    }

    //handling selling and creating auction
    private Response handleSell(Request request) {
        User user = this.currentUser;
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
        User user = this.currentUser;
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
        User user = this.currentUser;
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
        User user = this.currentUser;
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
        cleanupCurrentUserSession();
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

    //erase current user session
    //remove all subscriptions, unregister from notification system
    private void cleanupCurrentUserSession() {
        if (currentUser == null) {
            return;
        }
        UpdateSystem.getInstance().unregister(currentUser.getId(), out);
        UpdateSystem.getInstance().unsubscribeUserFromAllAuctions(currentUser.getId());
        currentUser = null;
    }
}
