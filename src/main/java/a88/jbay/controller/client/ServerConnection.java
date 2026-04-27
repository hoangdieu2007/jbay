package a88.jbay.controller.client;

import a88.jbay.model.network.Request;
import a88.jbay.model.network.Response;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class ServerConnection {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public void connect(String host, int port) throws UnknownHostException, IOException {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
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
                    // pass to UI
                }
            } catch (Exception e) {
                System.out.println("Disconnected from server");
            }
        });

        listener.start();
    }
}
