package a88.jbay.server;

import a88.jbay.model.network.Request;
import a88.jbay.model.network.Response;
import a88.jbay.model.event.Auction;
import a88.jbay.system.UpdateSystem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

/**
 * manages socket connections and stream operations for individual clients.
 * handles low-level networking concerns like connection lifecycle and data transmission.
 */
public class ClientConnection {
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private volatile boolean isConnected;

    public ClientConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        out.flush(); // Flush header immediately
        this.in = new ObjectInputStream(socket.getInputStream());
        this.isConnected = true;
    }

    /**
     * reads a request from the client
     * @return the request object, or null if connection closed/failed
     */
    public Request readRequest() {
        try {
            if (!isConnected || socket.isClosed()) {
                return null;
            }
            return (Request) in.readObject();
        } catch (ClassNotFoundException e) {
            System.err.println("Invalid request object received: " + e.getMessage());
            return null;
        } catch (IOException e) {
            if (!socket.isClosed()) {
                System.err.println("Client connection error: " + e.getMessage());
            }
            isConnected = false;
            return null;
        }
    }

    /**
     * sends a response to the client
     * @param response the response to send
     * @return true if successful, false if failed
     */
    public boolean sendResponse(Response response) {
        if (!isConnected || socket.isClosed()) {
            return false;
        }

        try {
            synchronized (out) {
                out.writeObject(response);
                out.flush();
            }
            return true;
        } catch (IOException e) {
            System.err.println("Failed to send response: " + e.getMessage());
            isConnected = false;
            return false;
        }
    }

    /**
     * gets the objectoutputstream for external use (e.g., updatesystem)
     * @return the objectoutputstream
     */
    public ObjectOutputStream getOutputStream() {
        return out;
    }

    /**
     * checks if the connection is still active
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return isConnected && !socket.isClosed();
    }

    /**
     * closes the connection and cleans up resources
     */
    public void close() {
        isConnected = false;
        
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

    /**
     * gets the remote address of the connected client
     * @return the client's ip address
     */
    public String getRemoteAddress() {
        return socket.getRemoteSocketAddress().toString();
    }

    /**
     * sends an auction update notification to this client
     * @param auction the auction to send
     * @return true if successful, false if failed
     */
    public boolean sendAuctionUpdate(Auction auction) {
        Response response = new Response(true, "AUCTION_UPDATE", auction);
        return sendResponse(response);
    }

    /**
     * sends auction list updates to this client
     * @param auctionType type of auction list (ACTIVE, SELLER, BIDDER)
     * @param auctions list of auctions to send
     * @return true if successful, false if failed
     */
    public boolean sendAuctionList(String auctionType, List<Auction> auctions) {
        Response response = new Response(true, auctionType + "_AUCTION_LIST", auctions);
        return sendResponse(response);
    }

    /**
     * registers this connection for notifications
     * @param userId the user id to register
     */
    public void registerForNotifications(int userId) {
        UpdateSystem.getInstance().registerConnection(userId, this);
    }

    /**
     * unregisters this connection from notifications
     * @param userId the user id to unregister
     */
    public void unregisterFromNotifications(int userId) {
        UpdateSystem.getInstance().unregisterConnection(userId, this);
    }

    /**
     * runs the main connection loop, processing requests until disconnected
     * @param requestHandler the handler to process requests
     */
    public void runConnectionLoop(RequestHandler requestHandler) {
        try {
            while (isConnected() && !Thread.currentThread().isInterrupted()) {
                try {
                    Request request = readRequest();
                    if (request == null) break;
                    
                    Response response = requestHandler.handleRequest(request);
                    sendResponse(response);
                } catch (Exception e) {
                    System.err.println("Error handling client request: " + e.getMessage());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to establish client connection: " + e.getMessage());
        }
    }
}
