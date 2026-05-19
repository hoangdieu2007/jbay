package a88.jbay.server;

import a88.jbay.system.update.ConnectionSystem;
import a88.jbay.system.user.UserSystem;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ClientService {
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private final ConnectionSystem connectionSystem;
    private final UserSystem userSystem;
    private final RequestHandler requestHandler;

    public ClientService(ConnectionSystem connectionSystem,
                         UserSystem userSystem,
                         RequestHandler requestHandler) {
        this.connectionSystem = connectionSystem;
        this.userSystem = userSystem;
        this.requestHandler = requestHandler;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void setupServerSocket(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public void startService() {
        Thread clientHandler = new Thread(() -> {
            System.out.println("Client handler starting...");
            try {
                while (true) {
                    Socket client = serverSocket.accept();
                    System.out.println("Client connected...");
                    ClientConnection connection = new ClientConnection(
                            client, connectionSystem, userSystem, requestHandler
                    );
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
