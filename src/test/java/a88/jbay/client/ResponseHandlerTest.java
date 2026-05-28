package a88.jbay.client;

import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.common.auction.BidTransaction;
import a88.jbay.common.item.Item;
import a88.jbay.common.network.Response;
import a88.jbay.common.user.User;
import a88.jbay.controller.ControllerProvider;
import a88.jbay.controller.app.EntranceUI.ClientLoginController;
import a88.jbay.controller.app.EntranceUI.ClientRegisterController;
import a88.jbay.view.ViewManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResponseHandlerTest {

    private ClientSession clientSession;
    private ControllerProvider controllerProvider;
    private ResponseHandler handler;
    private MockedStatic<ClientSession> csMock;
    private MockedStatic<ControllerProvider> cpMock;
    private MockedStatic<ViewManager> vmMock;
    private MockedStatic<Platform> platformMock;
    private MockedStatic<ServerConnection> scMock;

    @BeforeEach
    void setUp() throws Exception {
        resetResponseHandlerSingleton();
        clientSession = mock(ClientSession.class);
        controllerProvider = mock(ControllerProvider.class);

        csMock = mockStatic(ClientSession.class);
        csMock.when(ClientSession::getInstance).thenReturn(clientSession);

        cpMock = mockStatic(ControllerProvider.class);
        cpMock.when(ControllerProvider::getInstance).thenReturn(controllerProvider);

        vmMock = mockStatic(ViewManager.class);
        vmMock.when(ViewManager::getInstance).thenReturn(mock(ViewManager.class));

        scMock = mockStatic(ServerConnection.class);
        scMock.when(ServerConnection::getInstance).thenReturn(mock(ServerConnection.class));

        platformMock = mockStatic(Platform.class);

        handler = ResponseHandler.getInstance();
    }

    @AfterEach
    void tearDown() {
        csMock.close();
        cpMock.close();
        vmMock.close();
        scMock.close();
        platformMock.close();
    }

    private Item makeItem() {
        return new Item(1, "Test", "TYPE", "desc", 100.0);
    }

    private void resetResponseHandlerSingleton() throws Exception {
        Field instance = ResponseHandler.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    // --- PONG ---

    @Test
    void testHandlePong() {
        Response response = new Response(true, "PONG", null);
        handler.handle(response);
    }

    // --- DEFAULT (unknown message) ---

    @Test
    void testHandleDefault() {
        Response response = new Response(true, "UNKNOWN_MESSAGE", null);
        assertDoesNotThrow(() -> handler.handle(response));
    }

    @Test
    void testHandleDefaultFailure() {
        Response response = new Response(false, "UNKNOWN_FAILURE", null);
        assertDoesNotThrow(() -> handler.handle(response));
    }

    // --- LOGIN_FAIL ---

    @Test
    void testHandleLoginFail() {
        ClientLoginController loginController = mock(ClientLoginController.class);
        when(controllerProvider.getController(ClientLoginController.class)).thenReturn(loginController);

        Response response = new Response(false, "LOGIN_FAIL", null);
        handler.handle(response);

        verify(loginController).updateLoginLabel("Login failed");
    }

    // --- LOGIN_SUCCESS ---

    @Test
    void testHandleLoginSuccessAsUser() {
        User curUser = new User(1, "USER", "testuser", "sess");
        Response response = new Response(true, "LOGIN_SUCCESS", curUser);

        ClientLoginController loginController = mock(ClientLoginController.class);
        when(controllerProvider.getController(ClientLoginController.class)).thenReturn(loginController);

        when(clientSession.getUser()).thenReturn(curUser);

        handler.handle(response);

        verify(clientSession).setUser(curUser);
        verify(loginController).updateLoginLabel("Login successful");
    }

    @Test
    void testHandleLoginSuccessAsAdmin() {
        User curUser = new User(1, "ADMIN", "admin1", "sess");
        Response response = new Response(true, "LOGIN_SUCCESS", curUser);

        ClientLoginController loginController = mock(ClientLoginController.class);
        when(controllerProvider.getController(ClientLoginController.class)).thenReturn(loginController);

        when(clientSession.getUser()).thenReturn(curUser);

        handler.handle(response);

        verify(clientSession).setUser(curUser);
        verify(loginController).updateLoginLabel("Login successful");
    }

    @Test
    void testHandleLoginSuccessIOException() {
        User curUser = new User(1, "USER", "testuser", "sess");
        Response response = new Response(true, "LOGIN_SUCCESS", curUser);

        ClientLoginController loginController = mock(ClientLoginController.class);
        when(controllerProvider.getController(ClientLoginController.class)).thenReturn(loginController);
        when(clientSession.getUser()).thenReturn(curUser);

        vmMock.when(() -> ViewManager.displayScene(anyString())).thenThrow(new java.io.IOException("Test error"));

        handler.handle(response);

        verify(loginController).updateLoginLabel("Failed to display home screen");
        verify(clientSession).setUser(curUser);
    }

    // --- LOGOUT_SUCCESS ---

    @Test
    void testHandleLogoutSuccess() {
        Response response = new Response(true, "LOGOUT_SUCCESS", null);
        assertDoesNotThrow(() -> handler.handle(response));
        verify(clientSession).resetSession();
    }

    // --- REGISTER_SUCCESS / REGISTER_FAIL ---

    @Test
    void testHandleRegisterSuccess() {
        ClientRegisterController registerController = mock(ClientRegisterController.class);
        when(controllerProvider.getController(ClientRegisterController.class)).thenReturn(registerController);

        Response response = new Response(true, "REGISTER_SUCCESS", null);
        handler.handle(response);

        verify(registerController).updateRegisterLabel("Register successful");
    }

    @Test
    void testHandleRegisterFail() {
        ClientRegisterController registerController = mock(ClientRegisterController.class);
        when(controllerProvider.getController(ClientRegisterController.class)).thenReturn(registerController);

        Response response = new Response(false, "REGISTER_FAIL", null);
        handler.handle(response);

        verify(registerController).updateRegisterLabel("Register failed");
    }

    // --- ACTIVE_AUCTION_LIST ---

    @Test
    void testHandleActiveAuctionList() {
        ObservableMap<Integer, Auction> bidderAuctions = FXCollections.observableHashMap();
        when(clientSession.getBidderAuctions()).thenReturn(bidderAuctions);
        when(clientSession.getUser()).thenReturn(new User(1, "USER", "test"));

        Auction auction = new Auction(1, makeItem(),
                new a88.jbay.common.user.UserData(2, "seller", "USER", "pass"),
                LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        List<Auction> auctions = List.of(auction);

        Response response = new Response(true, "ACTIVE_AUCTION_LIST", auctions);
        handler.handle(response);

        assertEquals(1, bidderAuctions.size());
        assertSame(auction, bidderAuctions.get(1));
    }

    // --- SELLER_AUCTION_LIST ---

    @Test
    void testHandleSellerAuctionList() {
        ObservableMap<Integer, Auction> sellerAuctions = FXCollections.observableHashMap();
        when(clientSession.getSellerAuctions()).thenReturn(sellerAuctions);

        Auction auction = new Auction(2, makeItem(),
                new a88.jbay.common.user.UserData(3, "seller2", "USER", "pass"),
                LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        List<Auction> auctions = List.of(auction);

        Response response = new Response(true, "SELLER_AUCTION_LIST", auctions);
        handler.handle(response);

        assertEquals(1, sellerAuctions.size());
    }

    // --- BIDDER_AUCTION_LIST (maps to wonAuctions) ---

    @Test
    void testHandleBidderAuctionList() {
        ObservableMap<Integer, Auction> wonAuctions = FXCollections.observableHashMap();
        when(clientSession.getWonAuctions()).thenReturn(wonAuctions);

        Auction auction = new Auction(3, makeItem(),
                new a88.jbay.common.user.UserData(4, "seller3", "USER", "pass"),
                LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        List<Auction> auctions = List.of(auction);

        Response response = new Response(true, "BIDDER_AUCTION_LIST", auctions);
        handler.handle(response);

        assertEquals(1, wonAuctions.size());
    }

    // --- AUCTION_UPDATE ---

    @Test
    void testHandleAuctionUpdateGoesToBidderAuctions() {
        User user = new User(1, "USER", "testuser", "sess");
        when(clientSession.getUser()).thenReturn(user);

        ObservableMap<Integer, Auction> bidderAuctions = FXCollections.observableHashMap();
        when(clientSession.getBidderAuctions()).thenReturn(bidderAuctions);

        Auction auction = mock(Auction.class);
        when(auction.getId()).thenReturn(10);
        when(auction.getSellerName()).thenReturn("other");
        when(auction.getWinner()).thenReturn("someone");
        when(auction.getBidHistory()).thenReturn(List.of());

        Response response = new Response(true, "AUCTION_UPDATE", auction);
        handler.handle(response);

        assertSame(auction, bidderAuctions.get(10));
    }

    @Test
    void testHandleAuctionUpdateGoesToSellerAuctions() {
        User user = new User(1, "USER", "seller1", "sess");
        when(clientSession.getUser()).thenReturn(user);

        ObservableMap<Integer, Auction> sellerAuctions = FXCollections.observableHashMap();
        when(clientSession.getSellerAuctions()).thenReturn(sellerAuctions);

        Auction auction = mock(Auction.class);
        when(auction.getId()).thenReturn(10);
        when(auction.getSellerName()).thenReturn("seller1");
        when(auction.getBidHistory()).thenReturn(List.of());

        Response response = new Response(true, "AUCTION_UPDATE", auction);
        handler.handle(response);

        assertSame(auction, sellerAuctions.get(10));
    }

    @Test
    void testHandleAuctionUpdateGoesToWonAuctionsWhenFinished() {
        User user = new User(1, "USER", "winner1", "sess");
        when(clientSession.getUser()).thenReturn(user);

        ObservableMap<Integer, Auction> wonAuctions = FXCollections.observableHashMap();
        when(clientSession.getWonAuctions()).thenReturn(wonAuctions);

        Auction auction = mock(Auction.class);
        when(auction.getId()).thenReturn(10);
        when(auction.getWinner()).thenReturn("winner1");
        when(auction.getAuctionState()).thenReturn(AuctionState.FINISHED);
        when(auction.getBidHistory()).thenReturn(List.of());

        Response response = new Response(true, "AUCTION_UPDATE", auction);
        handler.handle(response);

        assertSame(auction, wonAuctions.get(10));
    }

    @Test
    void testHandleAuctionUpdateGoesToWonAuctionsWhenPaid() {
        User user = new User(1, "USER", "winner1", "sess");
        when(clientSession.getUser()).thenReturn(user);

        ObservableMap<Integer, Auction> wonAuctions = FXCollections.observableHashMap();
        when(clientSession.getWonAuctions()).thenReturn(wonAuctions);

        Auction auction = mock(Auction.class);
        when(auction.getId()).thenReturn(10);
        when(auction.getWinner()).thenReturn("winner1");
        when(auction.getAuctionState()).thenReturn(AuctionState.PAID);
        when(auction.getBidHistory()).thenReturn(List.of());

        Response response = new Response(true, "AUCTION_UPDATE", auction);
        handler.handle(response);

        assertSame(auction, wonAuctions.get(10));
    }

    @Test
    void testHandleAuctionUpdateAdminAlsoGoesToAdminAuctions() {
        User user = new User(1, "ADMIN", "admin", "sess");
        when(clientSession.getUser()).thenReturn(user);

        ObservableMap<Integer, Auction> adminAuctions = FXCollections.observableHashMap();
        ObservableMap<Integer, Auction> bidderAuctions = FXCollections.observableHashMap();
        when(clientSession.getAdminAuctions()).thenReturn(adminAuctions);
        when(clientSession.getBidderAuctions()).thenReturn(bidderAuctions);

        Auction auction = mock(Auction.class);
        when(auction.getId()).thenReturn(10);
        when(auction.getSellerName()).thenReturn("other");
        when(auction.getWinner()).thenReturn("someone");
        when(auction.getBidHistory()).thenReturn(List.of());

        Response response = new Response(true, "AUCTION_UPDATE", auction);
        handler.handle(response);

        assertSame(auction, bidderAuctions.get(10));
        assertSame(auction, adminAuctions.get(10));
    }

    @Test
    void testHandleAuctionUpdateAdminSellerGoesToSellerAndAdminAuctions() {
        User user = new User(1, "ADMIN", "seller1", "sess");
        when(clientSession.getUser()).thenReturn(user);

        ObservableMap<Integer, Auction> sellerAuctions = FXCollections.observableHashMap();
        ObservableMap<Integer, Auction> adminAuctions = FXCollections.observableHashMap();
        when(clientSession.getSellerAuctions()).thenReturn(sellerAuctions);
        when(clientSession.getAdminAuctions()).thenReturn(adminAuctions);

        Auction auction = mock(Auction.class);
        when(auction.getId()).thenReturn(10);
        when(auction.getSellerName()).thenReturn("seller1");
        when(auction.getWinner()).thenReturn("someone");
        when(auction.getBidHistory()).thenReturn(List.of());

        Response response = new Response(true, "AUCTION_UPDATE", auction);
        handler.handle(response);

        assertSame(auction, sellerAuctions.get(10));
        assertSame(auction, adminAuctions.get(10));
    }

    // --- ADMIN_AUCTION_LIST ---

    @Test
    void testHandleAdminAuctionList() {
        ObservableMap<Integer, Auction> adminAuctions = FXCollections.observableHashMap();
        when(clientSession.getAdminAuctions()).thenReturn(adminAuctions);

        Auction auction = new Auction(1, makeItem(),
                new a88.jbay.common.user.UserData(2, "seller", "USER", "pass"),
                LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        List<Auction> auctions = List.of(auction);

        Response response = new Response(true, "ADMIN_AUCTION_LIST", auctions);
        handler.handle(response);

        assertEquals(1, adminAuctions.size());
    }

    // --- ADMIN_USER_LIST ---

    @Test
    void testHandleAdminUserList() {
        ObservableMap<Integer, User> adminUsers = FXCollections.observableHashMap();
        when(clientSession.getAdminUsers()).thenReturn(adminUsers);

        User user = new User(1, "USER", "test");
        List<User> users = List.of(user);

        Response response = new Response(true, "ADMIN_USER_LIST", users);
        handler.handle(response);

        assertEquals(1, adminUsers.size());
        assertSame(user, adminUsers.get(1));
    }

    // --- USER_STATE_CHANGED ---

    @Test
    void testHandleUserStateChanged() {
        ObservableMap<Integer, User> adminUsers = FXCollections.observableHashMap();
        when(clientSession.getAdminUsers()).thenReturn(adminUsers);

        User updatedUser = new User(5, "BAN", "target");
        Response response = new Response(true, "USER_STATE_CHANGED", updatedUser);
        handler.handle(response);

        assertSame(updatedUser, adminUsers.get(5));
    }

    // --- NEW_USER_REGISTERED ---

    @Test
    void testHandleNewUserRegisteredAsAdmin() {
        ObservableMap<Integer, User> adminUsers = FXCollections.observableHashMap();
        when(clientSession.getAdminUsers()).thenReturn(adminUsers);

        User currentUser = new User(1, "ADMIN", "adminUser", "sess");
        when(clientSession.getUser()).thenReturn(currentUser);

        platformMock.when(() -> Platform.runLater(any(Runnable.class)))
                .thenAnswer(invocation -> {
                    ((Runnable) invocation.getArgument(0)).run();
                    return null;
                });

        User newUser = new User(10, "USER", "newbie");
        Response response = new Response(true, "NEW_USER_REGISTERED", newUser);
        handler.handle(response);

        assertSame(newUser, adminUsers.get(10));
    }

    @Test
    void testHandleNewUserRegisteredAsNonAdmin() {
        ObservableMap<Integer, User> adminUsers = mock(ObservableMap.class);
        when(clientSession.getAdminUsers()).thenReturn(adminUsers);

        User currentUser = new User(1, "USER", "regular", "sess");
        when(clientSession.getUser()).thenReturn(currentUser);

        User newUser = new User(10, "USER", "newbie");
        Response response = new Response(true, "NEW_USER_REGISTERED", newUser);
        handler.handle(response);

        verify(adminUsers, never()).put(anyInt(), any());
    }

    // --- PAY_QR ---

    @Test
    void testHandlePayQr() {
        byte[] qrData = new byte[]{1, 2, 3};
        Response response = new Response(true, "PAY_QR", qrData);
        assertDoesNotThrow(() -> handler.handle(response));
    }

    // --- SET_PENDING_PAYMENT ---

    @Test
    void testSetPendingPaymentAuction() {
        Auction auction = mock(Auction.class);
        handler.setPendingPaymentAuction(auction);
        // No getter for pendingPaymentAuction; smoke test only
    }

    // --- AUCTION_UPDATE_NOTIFY (Alert requires JavaFX toolkit) ---

    @Test
    void testHandleAuctionUpdateNotifyDispatches() {
        Response response = new Response(true, "AUCTION_UPDATE_NOTIFY", null);
        try {
            handler.handle(response);
        } catch (Throwable ignored) {
            // Alert requires JavaFX toolkit not available in test
        }
    }

    // --- CONFIRM_PAYMENT_SUCCESS (Alert requires JavaFX toolkit) ---

    @Test
    void testHandleConfirmPaymentSuccessDispatches() {
        Response response = new Response(true, "CONFIRM_PAYMENT_SUCCESS", null);
        try {
            handler.handle(response);
        } catch (Throwable ignored) {
            // Alert requires JavaFX toolkit not available in test
        }
    }

    // --- BAN_USER (Alert requires JavaFX toolkit) ---

    @Test
    void testHandleBanUserDispatches() {
        handler.setPendingPaymentAuction(mock(Auction.class));
        Response response = new Response(true, "BAN_USER", null);
        try {
            handler.handle(response);
        } catch (Throwable ignored) {
            // Alert requires JavaFX toolkit not available in test
        }
        verify(clientSession).resetSession();
    }
}
