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

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final RequestHandler requestHandler;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.requestHandler = new RequestHandler(UserSystem.getInstance(), AuctionSystem.getInstance());
    }

    //the handle loop
    @Override
    public void run() {
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            socket.setSoTimeout(120000); // 2 minute read timeout on server side
            socket.setKeepAlive(true);  // Enable TCP keep-alive
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            requestHandler.setObjectOutputStream(out);

            while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                try {
                    Request request = (Request) in.readObject();
                    if (request == null) break;

                    Response response = requestHandler.handleRequest(request);

                    //prevent crash when update and response sends at the same time
                    synchronized (this) {
                        out.reset();
                        out.writeObject(response);
                        out.flush();
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("Invalid request object received: " + e.getMessage());
                    break;
                } catch (IOException e) {
                    if (!socket.isClosed()) {
                        System.err.println("Client connection error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                        if (e instanceof java.net.SocketTimeoutException) {
                            System.err.println("Client socket timeout - connection may be idle");
                        } else if (e instanceof java.io.EOFException) {
                            System.err.println("Client disconnected unexpectedly (EOF)");
                        }
                    } else {
                        System.err.println("Client disconnected normally");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to establish client connection: " + e.getMessage());
        } finally {
            requestHandler.cleanupCurrentUserSession();
            closeResources(out, in, socket);
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

