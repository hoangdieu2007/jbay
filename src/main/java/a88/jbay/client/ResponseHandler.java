package a88.jbay.client;

import a88.jbay.model.entity.user.User;
import a88.jbay.model.network.Response;

public class ResponseHandler {
    private ClientSession clientSession = ClientSession.getInstance();

    public void handle(Response response) {
        switch (response.getMessage()) {
            case "LOGIN_SUCCESS" -> handleLoginSuccess(response);
            case "REGISTER_SUCCESS" -> handleRegisterSuccess(response);
            case "LOGOUT_SUCCESS" -> handleLogoutSuccess(response);
            default -> handleDefault(response);
        };
    }

    public void handleDefault(Response response) {
        System.out.println((String) response.getMessage());
    }

    public void handleLoginSuccess(Response response) {
        clientSession.setUser((User) response.getPayload());
    }

    public void handleRegisterSuccess(Response response) {
        System.out.println((String) response.getMessage());
    }

    public void handleLogoutSuccess(Response response) {
        clientSession.setUser(new User());
    }
}