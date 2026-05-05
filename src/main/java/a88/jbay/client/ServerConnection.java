package a88.jbay.client;

import a88.jbay.model.network.Request;
import a88.jbay.model.network.RequestType;
import a88.jbay.model.network.Response;
import javafx.application.Platform;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class ServerConnection {
    private static ServerConnection instance;
    private ResponseHandler responseHandler;

    private ServerConnection() {
        responseHandler = ResponseHandler.getInstance();
    }

    public synchronized static ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public void connect(String host, int port) throws UnknownHostException, IOException {
        socket = new Socket(host, port);
        socket.setSoTimeout(60000); // 60 second read timeout
        socket.setKeepAlive(true);  // Enable TCP keep-alive
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        System.out.println("Connection successful");
    }

    //methods for sending requests
    public synchronized void send(Request request) throws IOException {
        out.reset();
        out.writeObject(request);
        out.flush();
    }

    //listener
    public void startListener() {
        Thread listener = new Thread(() -> {
            try {
                while (!socket.isClosed()) {
                    try {
                        Response response = (Response) in.readObject();
                        System.out.println("Received response: " + (String) response.getMessage());
                        Platform.runLater(() -> responseHandler.handle(response));
                    } catch (java.net.SocketTimeoutException e) {
                        // Socket timeout is normal, continue the loop
                        System.out.println("Connection timeout, checking connection...");
                        continue;
                    } catch (java.io.EOFException e) {
                        System.out.println("Server closed the connection");
                        break;
                    }
                }
            } catch (Exception e) {
                System.out.println("Disconnected from server: " + e.getMessage());
                e.printStackTrace();
            } finally {
                System.out.println("Listener thread ended");
            }
        });

        listener.start();
    }

    public void disconnect() {
        try {
            send(new Request(RequestType.MISC)
                    .put("disconnect", true));
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
