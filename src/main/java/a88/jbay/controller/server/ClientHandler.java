package a88.jbay.controller.server;

import a88.jbay.model.entity.user.Credentials;
import a88.jbay.model.network.Request;
import a88.jbay.model.network.RequestType;
import a88.jbay.model.network.Response;
import a88.jbay.system.UserSystem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final UserSystem userService;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.userService = UserSystem.getInstance();
    }

    @Override
    public void run() {
        try (
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            while (true) {
                Request request = (Request) in.readObject();
                Response response = handleRequest(request);
                out.writeObject(response);
                out.flush();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Response handleRequest(Request request) {
        return switch (request.getType()) {
            case LOGIN -> handleLogin(request);
            case REGISTER -> handleRegister(request);
            default -> new Response(false, "Unsupported request", null);
        };
    }

    private Response handleLogin(Request request) {
        String username = (String) request.get("username");
        String password = (String) request.get("password");

        Credentials credentials = userService.login(username, password);
        if (credentials != null) {
            return new Response(true, "LOGIN_SUCCESS", credentials);
        }
        return new Response(false, "INVALID_CREDENTIALS", null);
    }

    private Response handleRegister(Request request) {
        String username = (String) request.get("username");
        String password = (String) request.get("password");
        String role = (String) request.get("role");

        if (role == null) role = "bidder"; // Default role

        if (userService.register(username, password, role)) {
            return new Response(true, "REGISTER_SUCCESS", null);
        }
        return new Response(false, "USER_ALREADY_EXISTS", null);
    }
}