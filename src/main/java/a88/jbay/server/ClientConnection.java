package a88.jbay.server;

import a88.jbay.model.network.Request;
import a88.jbay.model.network.Response;
import a88.jbay.system.AuctionSystem;
import a88.jbay.system.UserSystem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
    Handles individual client connections and socket management.
    Responsible for connection lifecycle, I/O operations, and delegating request processing to RequestHandler.
 */

public class ClientConnection implements Runnable {
    private final Socket socket;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;

    public ClientConnection(Socket socket) throws IOException {
        this.socket = socket;
        socket.setSoTimeout(120000); // 2 minute read timeout on server side
        socket.setKeepAlive(true);  // enable TCP keep-alive
        this.out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    //the handle loop
    @Override
    public void run() {
        try {
            while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                try {
                    Request request = (Request) in.readObject();
                    if (request == null) break;

                    Response response = RequestHandler.handleRequest(request);

                    //prevent crash when update and response sends at the same time
                    send(response);
                } catch (IOException e) {
                    System.err.println("Error reading request: " + e.getMessage());
                    break;
                }
                catch (ClassNotFoundException e) {
                    System.err.println("Invalid request object received: " + e.getMessage());
                    break;
                }
            }
        } finally {
            // later add clean up codes here!

            closeResources(out, in, socket);
        }
    }

    public void send(Response response) {
        try {
            //prevent crash when update and response send at the same time
            synchronized (this) {
                out.reset();
                out.writeObject(response);
                out.flush();
            }
        } catch (IOException e) {

        }
    }

    private void closeResources(ObjectOutputStream out, ObjectInputStream in, Socket socket) {
        try {
            if (out != null) out.close();
        } catch (IOException e) {
            System.err.println("Error closing output stream: " + e.getMessage());
        }
        try {
            if (in != null) in.close();
        } catch (IOException e) {
            System.err.println("Error closing input stream: " + e.getMessage());
        }
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }
}

