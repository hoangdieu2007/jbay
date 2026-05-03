package a88.jbay.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import a88.jbay.system.UpdateSystem;
import a88.jbay.system.UserSystem;

public class ClientService {
    private static ClientService instance;
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private volatile boolean isRunning;
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private static final int MAX_CONNECTIONS = 1000; // Connection limit

    private int port;

    private ClientService() {
        serverSocket = null;
        executor = Executors.newVirtualThreadPerTaskExecutor();
        isRunning = false;
    }

    public static synchronized ClientService getInstance() {
        if (instance == null) {
            instance = new ClientService();
        }
        return instance;
    }

    public void setupServerSocket(int port) throws IOException {
        this.port = port;
        serverSocket = new ServerSocket(port);
    }

    public synchronized void startService() {
        if (isRunning) {
            System.out.println("Service is already running");
            return;
        }
        isRunning = true;

        Thread clientHandler = new Thread(() -> {
            System.out.println("Client handler starting...");

            try {
                while (isRunning && !serverSocket.isClosed()) {
                    try {
                        Socket client = serverSocket.accept();
                        if (!isRunning) {
                            client.close(); // Reject if shutting down
                            break;
                        }

                        // Check connection limit
                        if (activeConnections.get() >= MAX_CONNECTIONS) {
                            System.err.println("Connection limit reached, rejecting client");
                            client.close();
                            continue;
                        }

                        System.out.println("Client connected... (Active: " + activeConnections.incrementAndGet() + ")");

                        executor.submit(() -> {
                            try {
                                RequestHandler handler = new RequestHandler(client);
                                handler.run();
                            } catch (IOException e) {
                                System.err.println("Failed to create client handler: " + e.getMessage());
                            } finally {
                                activeConnections.decrementAndGet();
                                System.out.println("Client disconnected (Active: " + activeConnections.get() + ")");
                            }
                        });
                    } catch (IOException e) {
                        if (isRunning) {
                            System.err.println("Error accepting client connection: " + e.getMessage());
                        }
                        // Break if server socket is closed during shutdown
                        if (serverSocket.isClosed()) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                if (isRunning) {
                    System.err.println("Unexpected error in client handler: " + e.getMessage());
                    e.printStackTrace();
                }
            } finally {
                System.out.println("Client handler stopped");
            }
        });

        clientHandler.start();
    }

    public synchronized void stopService() {
        if (!isRunning) {
            System.out.println("Service is already stopped");
            return;
        }
        isRunning = false;

        System.out.println("Shutting down service...");

        try {
            // Close server socket to stop accepting new connections
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Server socket closed");
            }

            // Shutdown executor and wait for active tasks to complete
            executor.shutdown();
            if (!executor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                System.out.println("Executor did not terminate gracefully, forcing shutdown");
                executor.shutdownNow();
            }
            System.out.println("Executor shutdown complete");

            // Clean up UpdateSystem connections
            UpdateSystem.getInstance().cleanupAllConnections();
            System.out.println("UpdateSystem cleanup complete");

            // Shutdown UserSystem sessions
            UserSystem.getInstance().shutdownAllSessions();
            System.out.println("UserSystem sessions shutdown complete");

        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Shutdown interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }

        System.out.println("Service stopped gracefully");
    }

    public boolean isRunning() {
        return isRunning;
    }
}
