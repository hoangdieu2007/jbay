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

    public ServerConnection(String host, int port) throws UnknownHostException, IOException {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    //methods for sending requests and receiving responses
    public Response send(Request request) {
        Response response = null;
        try {
            out.writeObject(request);
            out.flush();
            response = (Response) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            response = new Response(false, "SEND_REQUEST_FAIL", null);
        } finally {
            return response;
        }
    }
}
