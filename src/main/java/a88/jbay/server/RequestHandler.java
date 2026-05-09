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

import java.io.ObjectOutputStream;

/**
 * Handles processing of client requests by calling the appropriate systems.
 * Responsible for request routing and business logic delegation.
 */
public class RequestHandler {
    private static UserSystem userSystem = UserSystem.getInstance();
    private static AuctionSystem auctionSystem = AuctionSystem.getInstance();
//    private static User currentUser;
//    private static ObjectOutputStream out;

    // this method is mostly for testing
    // DO NOT CALL UNLESS FOR TESTING
    public static void inject(UserSystem userSystemParam, AuctionSystem auctionSystemParam) {
        RequestHandler.userSystem = userSystemParam;
        RequestHandler.auctionSystem = auctionSystemParam;
//        RequestHandler.currentUser = new User();
//        RequestHandler.out = null;
    }

//    public static void setObjectOutputStream(ObjectOutputStream outParam) {
//        RequestHandler.out = outParam;
//    }

    // directing request to respective handler
    public static Response handleRequest(Request request) {
        System.out.println("Received request: " + request.getType().name());

        return switch (request.getType()) {
            case LOGIN -> handleLogin(request);
            case REGISTER -> handleRegister(request);
            case LOGOUT -> handleLogout(request);
            case BID -> handleBid(request);
            case AUTO_BID -> handleAutoBid(request);
            case SELL -> handleSell(request);
            case CANCEL -> handleCancel(request);
            case SUBSCRIBE_AUCTION -> handleSubscribeAuction(request);
            case UNSUBSCRIBE_AUCTION -> handleUnsubscribeAuction(request);
            case GET_AUCTIONS -> handleGetAuctions(request);
            case MISC -> handleMisc(request);
        };
    }

    //handling login
    private static Response handleLogin(Request request) {
        String username = (String) request.get("username");
        String password = (String) request.get("password");

        User user = userSystem.login(username, password);
        if (user != null) {
            //check if user is banned
            if (user.getRole().equals("BAN")) {
                return new Response(false, "LOGIN_BAN", null);
            }

            // UpdateSystem.getInstance().register(user.getId(), RequestHandler.out);

            System.out.println("Login successful");
            System.out.println(user + " " + user.getSessionId());
            return new Response(true, "LOGIN_SUCCESS", user);
        }
        System.out.println("Login failed");
        return new Response(false, "LOGIN_FAIL", null);
    }

    //handling register
    private static Response handleRegister(Request request) {
        String username = (String) request.get("username");
        String password = (String) request.get("password");
        String role = "USER";

        if (userSystem.register(username, password, role)) {
            return new Response(true, "REGISTER_SUCCESS", null);
        }
        return new Response(false, "REGISTER_FAIL", null);
    }

    //handling logout
    //deletes session and logs out user, also removes all subscriptions
    private static Response handleLogout(Request request) {
        String sessionId = (String) request.get("sessionId");
        // cleanupCurrentUserSession();
        userSystem.logout(sessionId);
        return new Response(true, "LOGOUT_SUCCESS", null);
    }

    //handling bidding
    private static Response handleBid(Request request) {
        User user = userSystem.findBySessionId((String) request.get("sessionId"));
        if (user == null) return new Response(false, "INVALID_SESSION", null);
        if (user.can(ActionType.BID)) {
            boolean success = auctionSystem.placeBid(user.getId(), (Integer) request.get("auctionId"), (Double) request.get("amount"));
            return new Response(success, success ? "BID_SUCCESS" : "BID_FAIL", null);
        }
        return new Response(false, "BID_FAIL", null);
    }

    //handling auto-bidding
    private static Response handleAutoBid(Request request) {
        User user = userSystem.findBySessionId((String) request.get("sessionId"));
        if (user == null) return new Response(false, "INVALID_SESSION", null);
        if (user.can(ActionType.BID)) {
            auctionSystem.placeBidAutomated(user.getId(), (Integer) request.get("auctionId"), (Double) request.get("max_amount"), (Double) request.get("increment"));
            return new Response(true, "AUTO_BID_SUCCESS", null);
        }
        return new Response(false, "AUTO_BID_FAIL", null);
    }

    //handling selling and creating auction
    private static Response handleSell(Request request) {
        User user = userSystem.findBySessionId((String) request.get("sessionId"));
        if (user == null) return new Response(false, "INVALID_SESSION", null);
        if (user.can(ActionType.SELL)) {
            boolean success = auctionSystem.createAuction((Item)request.get("item"), user.getId(), (java.time.LocalDateTime) request.get("start"), (java.time.LocalDateTime) request.get("end"));
            return new Response(success, success ? "SELL_SUCCESS" : "SELL_FAIL", null);
        }
        return new Response(false, "SELL_FAIL", null);
    }

    //canceling auctions
    //ADMIN ONLY, REPORT IF CALLS FROM NORMAL USERS ALSO RETURN CANCEL_SUCCESS
    private static Response handleCancel(Request request) {
        User user = userSystem.findBySessionId((String) request.get("sessionId"));
        if (user == null) return new Response(false, "INVALID_SESSION", null);

        Auction auction = auctionSystem.getAuctionById((Integer) request.get("auctionId"));

        if (!user.getUsername().equals(auction.getSellerName()) && !user.getRole().equals("ADMIN")) {
            return new Response(false, "CANCEL_FAIL", null);
        }

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
    private static Response handleSubscribeAuction(Request request) {
        User user = userSystem.findBySessionId((String) request.get("sessionId"));
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
    private static Response handleUnsubscribeAuction(Request request) {
        User user = userSystem.findBySessionId((String) request.get("sessionId"));
        if (user == null) return new Response(false, "INVALID_SESSION", null);

        Integer auctionId = (Integer) request.get("auctionId");
        if (auctionId == null) {
            return new Response(false, "INVALID_AUCTION", null);
        }

        Auction auction = auctionSystem.getAuctionById(auctionId);
        auction.unsubscribe(user.getId());
        return new Response(true, "UNSUBSCRIBE_AUCTION_SUCCESS", null);
    }

    private static Response handleGetAuctions(Request request) {
        UpdateSystem.getInstance().updateAllAuctions((int) request.get("userId"));
        return new Response(true, "GET_AUCTIONS_SUCCESS", null);
    }

    //misc commands
    private static Response handleMisc(Request request) {
        return switch ((String) request.get("command")) {
            case "ls-auction" -> new Response(true, "LIST_AUCTION_SUCCESS", auctionSystem.listActiveAuctions());
            case "disconnect" -> {
                Thread.currentThread().interrupt();
                yield null;
            }
            default -> new Response(false, "INVALID_MISC_COMMAND", null);
        };
    }

    //erase current user session
    //remove all subscriptions, unregister from notification system
//    public static void cleanupCurrentUserSession() {
//        if (RequestHandler.currentUser == null) {
//            return;
//        }
//        UpdateSystem.getInstance().unregister(RequestHandler.currentUser.getId(), RequestHandler.out);
//        UpdateSystem.getInstance().unsubscribeUserFromAllAuctions(RequestHandler.currentUser.getId());
//        RequestHandler.currentUser = null;
//    }
}
