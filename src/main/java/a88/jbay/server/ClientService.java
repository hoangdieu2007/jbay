package a88.jbay.server;

import a88.jbay.system.UpdateSystem;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientService {
    private static ClientService instance;
    private ServerSocket serverSocket;
    private ExecutorService executor;

    private int port;

    private ClientService() {
        serverSocket = null;
        executor = Executors.newVirtualThreadPerTaskExecutor();
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

    public void startService() {
        Thread clientHandler = new Thread(() -> {
            System.out.println("Client handler starting...");

            try {
                Socket client = null;
                while (true) {
                    client = serverSocket.accept();
                    System.out.println("Client connected...");

                    ClientConnection connection = new ClientConnection(client);
                    UpdateSystem.getInstance().register(connection);
                    executor.submit(connection);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        clientHandler.start();
    }

    public void stopService() {
        executor.shutdown();
    }
}
