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
import a88.jbay.view.ViewManager;

import java.io.IOException;
import java.util.List;

public class ResponseHandler {
    private static ResponseHandler instance;
    private ClientSession clientSession;
    private ControllerProvider controllerProvider;
    private ViewManager viewManager;

    private ResponseHandler() {
        clientSession = ClientSession.getInstance();
        controllerProvider = ControllerProvider.getInstance();
        viewManager = ViewManager.getInstance();
    }

    public synchronized static ResponseHandler getInstance() {
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
                case "BAN_USER" -> handleBanUser(response);
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

    public void handleDefault(Response response) {
        System.out.println((String) response.getMessage());
    }

    public void handleLoginSuccess(Response response) {
        clientSession.setUser((User) response.getPayload());
        controllerProvider.getController(ClientLoginRegisterController.class).updateLoginLabel("Login successful");

        try {
            ViewManager.displayScene("client/Seller-Bidder-HomeScreens.fxml");

            ServerConnection.getInstance().send(new Request(RequestType.GET_AUCTIONS)
                    .put("userId", clientSession.getUser().getId()));
        } catch (IOException e) {
            controllerProvider.getController(ClientLoginRegisterController.class).updateLoginLabel("Failed to display home screen");
            e.printStackTrace();
        }
    }

    public void handleLoginFail(Response response) {
        controllerProvider.getController(ClientLoginRegisterController.class).updateLoginLabel("Login failed");
    }

    public void handleRegisterSuccess(Response response) {
        System.out.println((String) response.getMessage());
        controllerProvider.getController(ClientLoginRegisterController.class).updateRegisterLabel("Register successful");
    }

    public void handleRegisterFail(Response response) {
        controllerProvider.getController(ClientLoginRegisterController.class).updateRegisterLabel("Register failed");
    }

    public void handleLogoutSuccess(Response response) {
        try {
            ViewManager.displayScene("client/client-login-register-view.fxml");
            ClientSession.getInstance().resetSession();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleActiveAuctionList(Response response) {
        System.out.println("handleActiveAuctionList");
        List<Auction> activeAuctions = (List<Auction>) response.getPayload();
        for (Auction auction : activeAuctions) {
            clientSession.getBidderAuctions().put(-auction.getId(), auction);
            System.out.println(auction);
        }
    }

    private void handleSellerAuctionList(Response response) {
        System.out.println("handleSellerAuctionList");
        List<Auction> sellerAuctions = (List<Auction>) response.getPayload();
        for (Auction auction : sellerAuctions) {
            clientSession.getSellerAuctions().put(-auction.getId(), auction);
            System.out.println(auction);
        }
    }

    private void handleBidderAuctionList(Response response) {
        System.out.println("handleBidderAuctionList");
        List<Auction> bidderAuctions = (List<Auction>) response.getPayload();
        for (Auction auction : bidderAuctions) {
            clientSession.getBidderAuctions().put(-auction.getId(), auction);
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
        try {
            ViewManager.displayScene("client/client-login-register-view.fxml");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}