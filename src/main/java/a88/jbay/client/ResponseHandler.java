package a88.jbay.client;

import a88.jbay.common.auction.AuctionState;
import a88.jbay.controller.ControllerProvider;
import a88.jbay.controller.app.EntranceUI.ClientLoginController;
import a88.jbay.common.user.User;
import a88.jbay.common.user.role.Role;
import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.BidTransaction;
import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import a88.jbay.common.network.Response;
import a88.jbay.controller.app.EntranceUI.ClientRegisterController;
import a88.jbay.util.JBayLogger;
import a88.jbay.di.ClientApplicationContext;
import a88.jbay.view.ViewManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.IOException;
import java.util.List;

public class ResponseHandler {
    private ClientSession clientSession;
    private ControllerProvider controllerProvider;
    private ViewManager viewManager;
    private final JBayLogger logger;
    private Auction pendingPaymentAuction;

    public ResponseHandler(ClientSession clientSession, ControllerProvider controllerProvider, ViewManager viewManager) {
        this.logger = JBayLogger.getLogger(ResponseHandler.class);
        this.clientSession = clientSession;
        this.controllerProvider = controllerProvider;
        this.viewManager = viewManager;
    }

    public static ResponseHandler getInstance() {
        return ClientApplicationContext.getInstance().getDependency(ResponseHandler.class);
    }

    public void setPendingPaymentAuction(Auction auction) {
        this.pendingPaymentAuction = auction;
    }

    public void handle(Response response) {
        if (response.isSuccess()) {
            switch (response.getMessage()) {
                case "LOGIN_SUCCESS" -> handleLoginSuccess(response);
                case "REGISTER_SUCCESS" -> handleRegisterSuccess(response);
                case "LOGOUT_SUCCESS" -> handleLogoutSuccess(response);
                case "ACTIVE_AUCTION_LIST" -> handleActiveAuctionList(response);
                case "SELLER_AUCTION_LIST" -> handleSellerAuctionList(response);
                case "BIDDER_AUCTION_LIST" -> handleBidderAuctionList(response);
                case "AUCTION_UPDATE" -> handleAuctionUpdate(response);
                case "AUCTION_UPDATE_NOTIFY" -> handleAuctionUpdateNotify(response);
                case "PAY_QR" -> handlePayQr(response);
                case "CONFIRM_PAYMENT_SUCCESS" -> handleConfirmPaymentSuccess(response);
                case "ADMIN_AUCTION_LIST" -> handleAdminAuctionList(response);
                case "ADMIN_USER_LIST" -> handleAdminUserList(response);
                case "BAN_USER" -> handleBanUser(response);
                case "USER_STATE_CHANGED" -> handleUserStateChanged(response);
                case "NEW_USER_REGISTERED" -> handleNewUserRegistered(response);
                case "PONG" -> handlePong(response);
                default -> handleDefault(response);
            };
        } else {
            switch (response.getMessage()) {
                case "LOGIN_FAIL" -> handleLoginFail(response);
                case "REGISTER_FAIL" -> handleRegisterFail(response);
                default -> handleDefault(response);
            }
        }
    }

    public void handlePong(Response response) {
        logger.debug("Received PONG from server");
    }

    public void handleDefault(Response response) {
        logger.info((String) response.getMessage());
    }

    public void handleLoginSuccess(Response response) {
        User curUser = (User) response.getPayload();
        clientSession.setUser(curUser);
        controllerProvider.getController(ClientLoginController.class).updateLoginLabel("Login successful");

        try {
            logger.info("Displaying home screen");
            viewManager.openStage("Auction88's jBay");
            viewManager.resizeStage(1280, 720);
            if (curUser.getRole() == Role.USER) {
                viewManager.showScene("UserHomeScreenUI/user-HomeScreen.fxml");
                ServerConnection.getInstance().send(new Request(RequestType.GET_AUCTIONS)
                        .put("userId", clientSession.getUser().getId()));
            }
            else if (curUser.getRole() == Role.ADMIN) {
                viewManager.showScene("AdminUI/Admin-HomeScreens.fxml");
                ServerConnection.getInstance().send(new Request(RequestType.GET_AUCTIONS)
                        .put("userId", clientSession.getUser().getId()));
                ServerConnection.getInstance().send(new Request(RequestType.GET_USERS)
                        .put("userId", clientSession.getUser().getId()));
            }
        } catch (IOException e) {
            controllerProvider.getController(ClientLoginController.class).updateLoginLabel("Failed to display home screen");
            logger.error("Failed to display home screen" + e.getMessage() + e);
            e.printStackTrace();
        }
    }

    public void handleLoginFail(Response response) {
        controllerProvider.getController(ClientLoginController.class).updateLoginLabel("Login failed");
    }

    public void handleRegisterSuccess(Response response) {
        logger.info((String) response.getMessage());
        controllerProvider.getController(ClientRegisterController.class).updateRegisterLabel("Register successful");
    }

    public void handleRegisterFail(Response response) {
        controllerProvider.getController(ClientRegisterController.class).updateRegisterLabel("Register failed");
    }

    public void handleLogoutSuccess(Response response) {
        try {
            viewManager.openStage("Welcome to jBay");
            viewManager.resizeStage(1280, 720);
            clientSession.resetSession();
            controllerProvider.clearControllers();
            viewManager.showScene("EntranceUI/client-login-view.fxml");
        } catch (IOException e) {
            logger.error("Failed to display login view");
        }
    }

    private void handleActiveAuctionList(Response response) {
        System.out.println("handleActiveAuctionList");
        List<Auction> activeAuctions = (List<Auction>) response.getPayload();
        for (Auction auction : activeAuctions) {
            clientSession.getBidderAuctions().put(auction.getId(), auction);
            System.out.println(auction);
        }
    }

    private void handleSellerAuctionList(Response response) {
        System.out.println("handleSellerAuctionList");
        List<Auction> sellerAuctions = (List<Auction>) response.getPayload();
        for (Auction auction : sellerAuctions) {
            clientSession.getSellerAuctions().put(auction.getId(), auction);
            System.out.println(auction);
        }
    }

    private void handleBidderAuctionList(Response response) {
        System.out.println("handleBidderAuctionList");
        List<Auction> bidderAuctions = (List<Auction>) response.getPayload();
        logger.info("Received bidder auctions: " + bidderAuctions.size());
        for (Auction auction : bidderAuctions) {
            clientSession.getWonAuctions().put(auction.getId(), auction);
            System.out.println(auction);
        }
    }

    private void handleAuctionUpdate(Response response) {
        Auction auction = (Auction) response.getPayload();
        Role role = clientSession.getUser().getRole();

        System.out.println(auction);
        System.out.println("Bid List:");
        for (BidTransaction bid : auction.getBidHistory()) {
            System.out.println(bid);
        }

        if (clientSession.getUser().getUsername().equals(auction.getSellerName())) {
            clientSession.getSellerAuctions().put(auction.getId(), auction);
        } else if (clientSession.getUser().getUsername().equals(auction.getWinner())) {
            if (auction.getAuctionState().equals(AuctionState.FINISHED) || auction.getAuctionState().equals(AuctionState.PAID)) {
                clientSession.getWonAuctions().put(auction.getId(), auction);
            } else {
                clientSession.getBidderAuctions().put(auction.getId(), auction);
            }
        } else {
            clientSession.getBidderAuctions().put(auction.getId(), auction);
        }
        if (role == Role.ADMIN) {
            clientSession.getAdminAuctions().put(auction.getId(), auction);
        }
    }

    public void handlePayQr(Response response) {
        byte[] qrData = (byte[]) response.getPayload();

        javafx.application.Platform.runLater(() -> {
            try {
                viewManager.loadIntoMainScene("AuctionUI/QR-payment.fxml");

                a88.jbay.controller.app.AuctionUI.QRPaymentController qrController =
                        controllerProvider.getController(a88.jbay.controller.app.AuctionUI.QRPaymentController.class);

                if (qrController != null) {
                    // Lấy tên Seller từ bộ nhớ tạm, nếu không có thì để "Unknown Seller"
                    String sellerName = (pendingPaymentAuction != null) ? pendingPaymentAuction.getSellerName() : "Unknown Seller";

                    // Truyền dữ liệu vào QR Controller
                    qrController.setData(qrData, sellerName, pendingPaymentAuction);
                }
            } catch (IOException e) {
                logger.error("Failed to load QR Payment view: " + e.getMessage());
            }
        });
    }

    public void handleConfirmPaymentSuccess(Response response) {
        //switch to the home scene here, display alert payment confirmed
        showAlert(Alert.AlertType.INFORMATION, "Payment confirmed");
    }

    private void handleBanUser(Response response) {
        clientSession.resetSession();
        try {
            viewManager.openStage("Welcome to jBay");
            viewManager.resizeStage(1280, 720);
            viewManager.showScene("EntranceUI/client-login-view.fxml");
            showAlert(Alert.AlertType.WARNING, "You have been banned");
        } catch (IOException e) {
            logger.error("Failed to display login scene");
        }
    }

    private void handleUserStateChanged(Response response) {
        User updatedUser = (User) response.getPayload();
        clientSession.getAdminUsers().put(updatedUser.getId(), updatedUser);
    }

    private void handleNewUserRegistered(Response response) {
        User currentUser = clientSession.getUser();
        if (currentUser != null && currentUser.getRole() == Role.ADMIN) {

            // Lấy luôn user mới từ payload và nhét vào kho lưu trữ cục bộ
            User newUser = (User) response.getPayload();

            if (newUser != null) {
                // Đẩy vào JavaFX Thread để tránh lỗi Not on FX Application Thread
                javafx.application.Platform.runLater(() -> {
                    clientSession.getAdminUsers().put(newUser.getId(), newUser);
                });
            }
        }
    }

    private void handleAuctionUpdateNotify(Response response) {
        Auction auction = (Auction) response.getPayload();
        logger.info("handleAuctionUpdateNotify called for auction " + auction.getId());
        showAlert(Alert.AlertType.INFORMATION, "Auction " + auction.getId() + " - " + auction.getItem().getName() + " update: " + auction.getWinner() + " is the current winner, current price is " + auction.getCurrentPrice() + " USD");
    }

    private void handleAdminAuctionList(Response response) {
        List<Auction> auctions = (List<Auction>) response.getPayload();
        for (Auction a : auctions) {
            clientSession.getAdminAuctions().put(a.getId(), a);
        }
    }

    private void handleAdminUserList(Response response) {
        List<User> users = (List<User>) response.getPayload();
        // Ném User vào kho dành riêng cho Admin
        for (User u : users) {
            clientSession.getAdminUsers().put(u.getId(), u);
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        try {
            Platform.runLater(() -> new Alert(type, message).show());
        } catch (Exception e) {
            logger.warn("showAlert skipped (JavaFX not available): " + e.getMessage());
        }
    }


}
