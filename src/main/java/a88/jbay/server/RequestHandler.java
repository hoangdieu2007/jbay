package a88.jbay.server;

import a88.jbay.common.item.Item;
import a88.jbay.common.user.User;
import a88.jbay.common.network.RequestType;
import a88.jbay.common.auction.Auction;
import a88.jbay.common.network.Request;
import a88.jbay.common.network.Response;
import a88.jbay.system.AuctionSystem;
import a88.jbay.system.BidSystem;
import a88.jbay.system.update.ConnectionSystem;
import a88.jbay.system.update.UpdateSystem;
import a88.jbay.system.user.AdminService;
import a88.jbay.system.user.UserSystem;

/**
 * Handles processing of client requests by calling the appropriate systems.
 * Responsible for request routing and business logic delegation.
 */
public class RequestHandler {
    private final UserSystem userSystem;
    private final AdminService adminService;
    private final AuctionSystem auctionSystem;
    private final ConnectionSystem connectionSystem;
    private final UpdateSystem updateSystem;
    private final BidSystem bidSystem;

    public RequestHandler(UserSystem userSystem, AdminService adminService,
                          AuctionSystem auctionSystem, ConnectionSystem connectionSystem,
                          UpdateSystem updateSystem, BidSystem bidSystem) {
        this.userSystem = userSystem;
        this.adminService = adminService;
        this.auctionSystem = auctionSystem;
        this.connectionSystem = connectionSystem;
        this.updateSystem = updateSystem;
        this.bidSystem = bidSystem;
    }
    
//    public static void setObjectOutputStream(ObjectOutputStream outParam) {
//        RequestHandler.out = outParam;
//    }

    // directing request to respective handler
    public Response handleRequest(Request request) {
        System.out.println("Received request: " + request.getType().name());

        //check permission
        String sessionId = (String) request.get("sessionId");
        if (sessionId != null) {
            //authorized
            User user = userSystem.findBySessionId(sessionId);
            if (user == null) return new Response(false, "INVALID_SESSION", null);
            if (!user.can(request.getType())) {
                return new Response(false, "PERMISSION_DENIED", null);
            }
        } else {
            //unauthorized
            if (request.getType().equals(RequestType.LOGIN)) {
                return handleLogin(request);
            } else if (request.getType().equals(RequestType.REGISTER)) {
                return handleRegister(request);
            } else if (request.getType().equals(RequestType.PING)) {
                return handlePing(request);
            } else {
                return new Response(false, "PERMISSION_DENIED", null);
            }
        }

        return switch (request.getType()) {
            case PING -> handlePing(request);
            case LOGIN -> handleLogin(request);
            case REGISTER -> handleRegister(request);
            case LOGOUT -> handleLogout(request);
            case BID -> handleBid(request);
            case AUTO_BID -> handleAutoBid(request);
            case CANCEL_AUTO_BID -> handleCancelAutoBid(request);
            case SELL -> handleSell(request);
            case PAY -> handlePay(request);
            case CONFIRM_PAYMENT -> handleConfirmPayment(request);
            case CANCEL -> handleCancel(request);
            case SUBSCRIBE_AUCTION -> handleSubscribeAuction(request);
            case UNSUBSCRIBE_AUCTION -> handleUnsubscribeAuction(request);
            case GET_AUCTIONS -> handleGetAuctions(request);
            case GET_USERS -> handleGetUsers(request);
            case BAN -> handleBan(request);
            case MISC -> handleMisc(request);
        };
    }

    private Response handlePing(Request request) {
        return new Response(true, "PONG", null);
    }

    //handling login
    private Response handleLogin(Request request) {
        String username = (String) request.get("username");
        String password = (String) request.get("password");

        User user = userSystem.login(username, password);
        if (user != null) {
            //check if user is banned
            if (user.getRole().equals("BAN")) {
                return new Response(true, "BAN_USER", null);
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
    private Response handleRegister(Request request) {
        String username = (String) request.get("username");
        String password = (String) request.get("password");
        byte[] qrCode = (byte[]) request.get("qrCode"); // Lấy dữ liệu ảnh
        String role = (String) request.get("role");
        if (role == null || role.isBlank()) role = "USER";

        // Truyền thêm biến qrCode vào UserSystem
        if (userSystem.register(username, password, role, qrCode)) {
            User newUser = userSystem.getUserByName(username);
            updateSystem.broadcastToAll(new Response(true, "NEW_USER_REGISTERED", newUser));

            return new Response(true, "REGISTER_SUCCESS", null);
        }
        return new Response(false, "REGISTER_FAIL", null);
    }

    //handling logout
    //deletes session and logs out user, also removes all subscriptions
    private Response handleLogout(Request request) {
        String sessionId = (String) request.get("sessionId");
        // cleanupCurrentUserSession();
        userSystem.logout(sessionId);
        return new Response(true, "LOGOUT_SUCCESS", null);
    }

    //handling bidding
    private Response handleBid(Request request) {
        User user = userSystem.findBySessionId((String) request.get("sessionId"));
        if (user == null) return new Response(false, "INVALID_SESSION", null);
        boolean success = bidSystem.placeBid(user.getId(), (Integer) request.get("auctionId"), (Double) request.get("amount"));
        return new Response(success, success ? "BID_SUCCESS" : "BID_FAIL", null);
    }

    //handling auto-bidding
    private Response handleAutoBid(Request request) {
        User user = userSystem.findBySessionId((String) request.get("sessionId"));
        if (user == null) return new Response(false, "INVALID_SESSION", null);
        bidSystem.placeBidAutomated(user.getId(), (Integer) request.get("auctionId"), (Double) request.get("max_amount"), (Double) request.get("increment"));
        return new Response(true, "AUTO_BID_SUCCESS", null);
    }

    //handling cancel auto-bid
    private Response handleCancelAutoBid(Request request) {
        User user = userSystem.findBySessionId((String) request.get("sessionId"));
        if (user == null) return new Response(false, "INVALID_SESSION", null);
        bidSystem.cancelAutoBid(user.getId(), (Integer) request.get("auctionId"));
        return new Response(true, "CANCEL_AUTO_BID_SUCCESS", null);
    }

    //handling selling and creating auction
    private Response handleSell(Request request) {
        User user = userSystem.findBySessionId((String) request.get("sessionId"));
        if (user == null) return new Response(false, "INVALID_SESSION", null);
        Item item = (Item) request.get("item");
        java.time.LocalDateTime start = (java.time.LocalDateTime) request.get("start");
        java.time.LocalDateTime end = (java.time.LocalDateTime) request.get("end");
        if (item == null || start == null || end == null) {
            return new Response(false, "SELL_FAIL", null);
        }

        Object minIncrementValue = request.get("minIncrement");
        double minIncrement = minIncrementValue instanceof Number number ? number.doubleValue() : 0.0;

        boolean success = auctionSystem.createAuction(
                item,
                user.getId(),
                minIncrement,
                start,
                end
        );
        return new Response(success, success ? "SELL_SUCCESS" : "SELL_FAIL", null);
    }

    //pay request - returns seller QR
    private Response handlePay(Request request) {
        User user = userSystem.findBySessionId((String) request.get("sessionId"));
        if (user == null) return new Response(false, "INVALID_SESSION", null);

        // Lấy thông tin phiên đấu giá
        Integer auctionId = (Integer) request.get("auctionId");
        Auction auction = auctionSystem.getAuctionById(auctionId);
        if (auction == null) return new Response(false, "PAY_FAIL", null);

        return new Response(true, "PAY_QR", userSystem.getQr(auction.getSellerId()));
    }

    //handle confirm payment
    private Response handleConfirmPayment(Request request) {
        User user = userSystem.findBySessionId((String) request.get("sessionId"));
        if (user == null) return new Response(false, "INVALID_SESSION", null);

        Auction auction = auctionSystem.getAuctionById((Integer) request.get("auctionId"));
        if (auction == null) return new Response(false, "INVALID_AUCTION", null);

        if (!user.getUsername().equals(auction.getSellerName())) {
            return new Response(false, "CONFIRM_PAYMENT_FAIL", null);
        }

        boolean success = auctionSystem.confirmPayment(auction.getId());

        if (success) {
            return new Response(true, "CONFIRM_PAYMENT_SUCCESS", null);
        }

        return new Response(false, "CONFIRM_PAYMENT_FAIL", null);
    }

    //canceling auctions
    //ADMIN ONLY, REPORT IF CALLS FROM NORMAL USERS ALSO RETURN CANCEL_SUCCESS
    private Response handleCancel(Request request) {
        User user = userSystem.findBySessionId((String) request.get("sessionId"));
        if (user == null) return new Response(false, "INVALID_SESSION", null);

        Auction auction = auctionSystem.getAuctionById((Integer) request.get("auctionId"));

        if (!user.getUsername().equals(auction.getSellerName()) && !user.getRole().equals("ADMIN")) {
            return new Response(false, "CANCEL_FAIL", null);
        }

        boolean success = auctionSystem.cancelAuction(auction.getId());
        return new Response(success, success ? "CANCEL_SUCCESS" : "CANCEL_FAIL", null);
    }

    //subscribing to auctions
    //this is often automatically handled by the auction system upon bidding/selling a product
    //but separating this makes everything clear
    private Response handleSubscribeAuction(Request request) {
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
    private Response handleUnsubscribeAuction(Request request) {
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

    private Response handleGetAuctions(Request request) {
        // Lấy thông tin user từ sessionId để kiểm tra Role
        String sessionId = (String) request.get("sessionId");
        User user = userSystem.findBySessionId(sessionId);

        // Kiểm tra xem có phải ADMIN không
        if (user != null && user.getRole().equals("ADMIN")) {
            // Chuyển hướng luồng chạy cho ADMIN
            auctionSystem.updateAdminAuctions(user.getId());
        } else {
            // GIỮ NGUYÊN LUỒNG CŨ CHO USER (Bidder/Seller)
            if (user == null) {
                return new Response(false, "INVALID_SESSION", null);
            }
            auctionSystem.updateAllAuctions(user.getId());
        }

        return new Response(true, "GET_AUCTIONS_SUCCESS", null);
    }

    // Xử lý luồng lấy danh sách User (Chỉ Admin mới có quyền)
    private Response handleGetUsers(Request request) {
        String sessionId = (String) request.get("sessionId");
        User user = userSystem.findBySessionId(sessionId);

        // Chặn cửa: Chỉ xử lý nếu là ADMIN
        if (user != null && user.getRole().equals("ADMIN")) {
            // Nhờ UpdateSystem đóng gói và đẩy qua mạng
            updateSystem.sendToUser(
                    user.getId(),
                    new Response(true, "ADMIN_USER_LIST", userSystem.getAllNormalUsersForAdmin())
            );
            return new Response(true, "GET_USERS_SUCCESS", null);
        }

        return new Response(false, "UNAUTHORIZED", null);
    }

    private Response handleBan(Request request) {
        String sessionId = (String) request.get("sessionId");
        User admin = userSystem.findBySessionId(sessionId);

        // Kiểm tra bảo mật nghiêm ngặt: Chỉ ADMIN mới được xử lý luồng này
        if (admin == null || !admin.getRole().equals("ADMIN")) {
            return new Response(false, "UNAUTHORIZED", null);
        }

        int userId = (int) request.get("userId");
        String action = (String) request.get("action"); // Lấy biến hành động "BAN" / "UNBAN"

        User updatedUser; // Khai báo đối tượng hứng kết quả

        // Phân luồng điều hướng nghiệp vụ
        if ("UNBAN".equals(action)) {
            updatedUser = adminService.unbanUser(userId);
        } else {
            updatedUser = adminService.banUser(userId);
        }

        // Nếu xử lý dưới DB hoặc Service thất bại (trả về null)
        if (updatedUser == null) {
            return new Response(false, "BAN_FAIL", null);
        }

        // Đóng gói Object mang trạng thái mới và phát loa
        Response broadcastResponse = new Response(true, "USER_STATE_CHANGED", updatedUser);
        updateSystem.broadcastToAll(broadcastResponse);

        return broadcastResponse;
    }

    //misc commands
    private Response handleMisc(Request request) {
        Object commandValue = request.get("command");
        if (!(commandValue instanceof String command) || command.isBlank()) {
            return new Response(false, "INVALID_MISC_COMMAND", null);
        }

        return switch (command) {
            case "ls-auction" -> new Response(true, "LIST_AUCTION_SUCCESS", auctionSystem.listActiveAuctions());
            case "disconnect" -> {
                Thread.currentThread().interrupt();
                yield null;
            }
            default -> new Response(false, "INVALID_MISC_COMMAND", null);
        };
    }

}
