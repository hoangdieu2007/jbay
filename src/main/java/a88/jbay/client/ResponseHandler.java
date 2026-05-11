package a88.jbay.client;

import a88.jbay.controller.ControllerProvider;
import a88.jbay.controller.client.ClientLoginRegisterController;
import a88.jbay.controller.client.SellerBidderHomeScreenController;
import a88.jbay.model.entity.user.User;
import a88.jbay.model.event.Auction;
import a88.jbay.model.event.BidTransaction;
import a88.jbay.model.network.Request;
import a88.jbay.model.network.RequestType;
import a88.jbay.model.network.Response;
import a88.jbay.util.JBayLogger;
import a88.jbay.view.ViewManager;
import javafx.scene.control.Alert;

import javax.swing.text.View;
import java.io.IOException;
import java.util.List;

public class ResponseHandler {
    private static ResponseHandler instance;
    private ClientSession clientSession;
    private ControllerProvider controllerProvider;
    private ViewManager viewManager;
    private final JBayLogger logger;

    private ResponseHandler() {
        this.logger = JBayLogger.getLogger(ResponseHandler.class);
        clientSession = ClientSession.getInstance();
        controllerProvider = ControllerProvider.getInstance();
        viewManager = ViewManager.getInstance();
    }

    public static ResponseHandler getInstance() {
        if (instance == null) {
            instance = new ResponseHandler();
        }
        return instance;
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
                case "BAN_USER" -> handleBanUser(response);
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
        controllerProvider.getController(ClientLoginRegisterController.class).updateLoginLabel("Login successful");

        try {
            ViewManager.closePrimaryStage();
            ViewManager.newStage("Auction88's jBay");
            ViewManager.setResolution(1280, 720);
            if (curUser.getRole().equals("USER"))
                ViewManager.displayScene("client/Seller-Bidder-HomeScreens.fxml");
            else if (curUser.getRole().equals("ADMIN"))
                ViewManager.displayScene("client/Admin-HomeScreens.fxml");

            ServerConnection.getInstance().send(new Request(RequestType.GET_AUCTIONS)
                    .put("userId", clientSession.getUser().getId()));
        } catch (IOException e) {
            controllerProvider.getController(ClientLoginRegisterController.class).updateLoginLabel("Failed to display home screen");
                    }
    }

    public void handleLoginFail(Response response) {
        controllerProvider.getController(ClientLoginRegisterController.class).updateLoginLabel("Login failed");
    }

    public void handleRegisterSuccess(Response response) {
        logger.info((String) response.getMessage());
        controllerProvider.getController(ClientLoginRegisterController.class).updateRegisterLabel("Register successful");
    }

    public void handleRegisterFail(Response response) {
        controllerProvider.getController(ClientLoginRegisterController.class).updateRegisterLabel("Register failed");
    }

    public void handleLogoutSuccess(Response response) {
        try {
            ViewManager.closePrimaryStage();
            ViewManager.newStage("Welcome to jBay");
            ViewManager.setResolution(600, 429);
            ClientSession.getInstance().resetSession();
            ControllerProvider.getInstance().clearControllers();
            ViewManager.displayScene("client/client-login-register-view.fxml");
        } catch (IOException e) {
            logger.error("Failed to display login register view");
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
        for (Auction auction : bidderAuctions) {
            clientSession.getBidderAuctions().put(auction.getId(), auction);
            System.out.println(auction);
        }
    }

    private void handleAuctionUpdate(Response response) {
        Auction auction = (Auction) response.getPayload();

        System.out.println(auction);
        System.out.println("Bid List:");
        for (BidTransaction bid : auction.getBidHistory()) {
            System.out.println(bid);
        }

        if (clientSession.getUser().getUsername().equals(auction.getSellerName())) {
            clientSession.getSellerAuctions().put(auction.getId(), auction);
        } else {
            clientSession.getBidderAuctions().put(auction.getId(), auction);
        }
    }

    private void handleBanUser(Response response) {
        clientSession.resetSession();
        ControllerProvider.getInstance().clearControllers();
        try {
            ViewManager.closePrimaryStage();
            ViewManager.newStage("Welcome to jBay");
            ViewManager.setResolution(600, 429);
            ViewManager.displayScene("client/client-login-register-view.fxml");
            new Alert(Alert.AlertType.WARNING, "You have been banned").show();
        } catch (IOException e) {
            logger.error("Failed to display login scene");
        }
    }

    private void handleAuctionUpdateNotify(Response response) {
        Auction auction = (Auction) response.getPayload();
        logger.info("handleAuctionUpdateNotify called for auction " + auction.getId());
        new Alert(Alert.AlertType.INFORMATION, "Auction " + auction.getId() + " - " + auction.getItem().getName() + " update: " + auction.getWinner() + " is the current winner, current price is " + auction.getCurrentPrice() + " USD").show();
    }
}