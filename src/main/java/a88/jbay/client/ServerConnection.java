package a88.jbay.client;

import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import a88.jbay.common.network.Response;
import a88.jbay.util.JBayLogger;
import a88.jbay.di.ClientApplicationContext;
import a88.jbay.view.ViewManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerConnection {
    private ResponseHandler responseHandler;
    private ClientSession clientSession;
    private ViewManager viewManager;
    private final JBayLogger logger;
    private Thread listenerThread;
    private ScheduledExecutorService scheduler;
    private ExecutorService sendExecutor;
    private ScheduledFuture<?> pingTask;
    private volatile boolean listenerRunning = false;
    private volatile boolean disconnecting = false;

    public ServerConnection(ResponseHandler responseHandler, ClientSession clientSession, ViewManager viewManager) {
        this.logger = JBayLogger.getLogger(ServerConnection.class);
        this.responseHandler = responseHandler;
        this.clientSession = clientSession;
        this.viewManager = viewManager;
    }

    public static ServerConnection getInstance() {
        return ClientApplicationContext.getInstance().getDependency(ServerConnection.class);
    }

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public synchronized void connect(String host, int port) throws UnknownHostException, IOException {
        logger.info("Connecting to server: " + host + ":" + port);
        disconnecting = false;

        if (socket != null && !socket.isClosed()) {
            closeResources();
        }

        socket = new Socket(host, port);
        socket.setKeepAlive(true);  // Enable TCP keep-alive

        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "client-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        sendExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "client-send-queue");
            thread.setDaemon(true);
            return thread;
        });

        logger.info("Connection successful to server: " + host + ":" + port);
    }

    //methods for sending requests
    public void send(Request request) throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected to server. Please connect first.");
        }

        if (request == null) return;

        logger.info("Sending request: " + request.getType().name());

        //automatically add sessionId
        String sessionId = clientSession.getUser().getSessionId();
        if (sessionId != null && !sessionId.isBlank() && !"none".equals(sessionId)) {
            request.put("sessionId", sessionId);
        }

        if (sendExecutor == null || sendExecutor.isShutdown()) {
            throw new IOException("Send queue is not available.");
        }

        sendExecutor.execute(() -> sendQueued(request));
    }

    private void sendQueued(Request request) {
        ObjectOutputStream currentOut = out;
        if (!isConnected() || currentOut == null) {
            return;
        }

        try {
            currentOut.writeObject(request);
            currentOut.flush();
            currentOut.reset();
        } catch (IOException e) {
            logger.error("Failed to send request: " + request.getType().name() + " - " + e.getMessage(), e);
            handleConnectionLost("Connection to server lost");
        }
    }

    //listener
    public void startListener() {
        if (listenerRunning) {
            logger.warn("Listener already running, skipping startListener()");
            return;
        }
        if (!isConnected()) {
            logger.warn("Cannot start listener before connecting to server");
            return;
        }

        listenerRunning = true;
        listenerThread = new Thread(() -> {
            try {
                while (listenerRunning && isConnected()) {
                    try {
                        Response response = (Response) in.readObject();
                        logger.debug("Received response: " + (String) response.getMessage());
                        Platform.runLater(() -> responseHandler.handle(response));
                    } catch (java.io.EOFException e) {
                        logger.info("Server closed the connection");
                        handleConnectionLost("Disconnected from server");
                        break;
                    }
                }
            } catch (Exception e) {
                logger.error("Disconnected from server: " + e.getMessage(), e);
                handleConnectionLost("Disconnected from server");
            } finally {
                logger.info("Listener thread ended");
                listenerRunning = false;
            }
        });

        listenerThread.start();
        startPing();
    }

    public synchronized void startPing() {
        if (scheduler == null || scheduler.isShutdown()) {
            return;
        }
        if (pingTask != null && !pingTask.isCancelled() && !pingTask.isDone()) {
            return;
        }

        pingTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                send(new Request(RequestType.PING));
            } catch (IOException e) {
                logger.error("Failed to send ping: " + e.getMessage(), e);
                handleConnectionLost("Connection to server lost");
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    public synchronized void disconnect() {
        disconnecting = true;
        listenerRunning = false;
        if (pingTask != null) {
            pingTask.cancel(true);
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
        closeResources();
        if (sendExecutor != null) {
            sendExecutor.shutdownNow();
        }
    }

    private boolean isConnected() {
        return socket != null && !socket.isClosed() && out != null;
    }

    private void handleConnectionLost(String message) {
        if (disconnecting) {
            return;
        }
        disconnecting = true;
        listenerRunning = false;
        closeResources();
        if (pingTask != null) {
            pingTask.cancel(true);
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (sendExecutor != null) {
            sendExecutor.shutdownNow();
        }

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING, message);
            alert.showAndWait();
            try {
                viewManager.openStage("Welcome to jBay");
                viewManager.resizeStage(600, 429);
                viewManager.showScene("EntranceUI/client-server-connect-view.fxml");
            } catch (IOException ex) {
                logger.error("Failed to switch to connection view: " + ex.getMessage(), ex);
            }
        });
    }

    private void closeResources() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            logger.error("Failed to close input stream: " + e.getMessage(), e);
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            logger.error("Failed to close output stream: " + e.getMessage(), e);
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            logger.error("Failed to close socket: " + e.getMessage(), e);
        }

        in = null;
        out = null;
        socket = null;
    }
}
