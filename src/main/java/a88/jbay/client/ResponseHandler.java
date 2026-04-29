package a88.jbay.client;

import a88.jbay.controller.ControllerProvider;
import a88.jbay.controller.client.ClientLoginRegisterController;
import a88.jbay.model.entity.user.User;
import a88.jbay.model.network.Response;

public class ResponseHandler {
    private static ResponseHandler instance;
    private ClientSession clientSession;
    private ControllerProvider controllerProvider;

    private ResponseHandler() {
        clientSession = ClientSession.getInstance();
        controllerProvider = ControllerProvider.getInstance();
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
        clientSession.setUser(new User());
    }
}