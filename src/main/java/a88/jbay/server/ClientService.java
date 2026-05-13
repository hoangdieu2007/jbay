package a88.jbay.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ClientService {
    private static ClientService instance;
    private ServerSocket serverSocket;
    private ExecutorService executor;

    private int port;

    private ClientService() {
        serverSocket = null;
        executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public static ClientService getInstance() {
        if (instance == null) {
            instance = new ClientService();
        }
        return instance;
    }

    public void setupServerSocket(int port) throws IOException {
        this.port = port;
        serverSocket = new ServerSocket(port);
    }

    public void startService() {
        Thread clientHandler = new Thread(() -> {
            System.out.println("Client handler starting...");

            try {
                Socket client = null;
                while (true) {
                    client = serverSocket.accept();
                    System.out.println("Client connected...");

                    ClientConnection connection = new ClientConnection(client);
                    executor.submit(connection);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        clientHandler.start();
    }

    public void stopService() {
        try {
            // Close the server socket to stop accepting new connections
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
            // Shutdown the executor and wait for active tasks to complete
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Executor did not terminate");
                }
            }
        } catch (IOException | InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
