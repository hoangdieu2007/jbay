package a88.jbay.client;

import a88.jbay.model.network.Request;
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
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        System.out.println("Connection successful");
    }

    //methods for sending requests
    public synchronized void send(Request request) throws IOException {
        out.writeObject(request);
        out.flush();
    }

    //listener
    public void startListener() {
        Thread listener = new Thread(() -> {
            try {
                while (!socket.isClosed()) {
                    Response response = (Response) in.readObject();
                    System.out.println((String) response.getMessage());
                    Platform.runLater(() -> responseHandler.handle(response));
                }
            } catch (Exception e) {
                System.out.println("Disconnected from server");
            }
        });

        listener.start();
    }
}
