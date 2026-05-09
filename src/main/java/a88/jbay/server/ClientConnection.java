package a88.jbay.server;

import a88.jbay.model.entity.user.User;
import a88.jbay.model.network.Request;
import a88.jbay.model.network.Response;
import a88.jbay.system.AuctionSystem;
import a88.jbay.system.UpdateSystem;
import a88.jbay.system.UserSystem;
import a88.jbay.util.JBayLogger;

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
    private final int connectionId;
    private final JBayLogger logger;

    // user cache, this helps reduce the number of database queries
    private User userCache;

    public ClientConnection(Socket socket) throws IOException {
        this.connectionId = socket.hashCode(); // Use socket hash as connection ID
        this.socket = socket;
        socket.setSoTimeout(120000); // 2 minute read timeout on server side
        socket.setKeepAlive(true);  // enable TCP keep-alive
        this.out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());

        this.userCache = new User();

        this.logger = JBayLogger.getInstance();
    }

    public int getConnectionId() {
        return connectionId;
    }

    public boolean isActive() {
        return socket != null && !socket.isClosed() && !Thread.currentThread().isInterrupted();
    }

    public User getUserCache() {
        return userCache;
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

                    // update cache if login success
                    if (response.getMessage().equals("LOGIN_SUCCESS")) {
                        this.userCache = (User) response.getPayload();
                        UpdateSystem.getInstance().register(this);
                    } else if (response.getMessage().equals("LOGOUT_SUCCESS")) {
                        this.userCache = new User();
                        UpdateSystem.getInstance().unregister(this);
                    }

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
            UpdateSystem.getInstance().unregister(this);
            closeResources(out, in, socket);
        }
    }

    public void send(Response response) {
        logger.info("Sending response: " + response.getMessage());

        try {
            //prevent crash when update and response send at the same time
            synchronized (this) {
                out.reset();
                out.writeObject(response);
                out.flush();
            }
        } catch (IOException e) {
            logger.error("Error sending response: " + e.getMessage(), e);
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

    public void close() {
        closeResources(out, in, socket);
    }
}

