package a88.jbay.client;

import a88.jbay.model.entity.user.User;
import a88.jbay.model.network.Response;

public class ResponseHandler {
    private ClientSession clientSession = ClientSession.getInstance();

    public boolean handle(Response response) {
        return switch (response.getMessage()) {
            case "LOGIN_SUCCESS" -> handleLoginSuccess(response);
            case "REGISTER_SUCCESS" -> handleRegisterSuccess(response);
            case "LOGOUT_SUCCESS" -> handleLogoutSuccess(response);
            default -> handleDefault(response);
        };
    }

    public boolean handleDefault(Response response) {
        System.out.println((String) response.getMessage());
        return false;
    }

    public boolean handleLoginSuccess(Response response) {
        clientSession.setUser((User) response.getPayload());
        return true;
    }

    public boolean handleRegisterSuccess(Response response) {
        return true;
    }

    public boolean handleLogoutSuccess(Response response) {
        clientSession.setUser(new User());
        return true;
    }
}