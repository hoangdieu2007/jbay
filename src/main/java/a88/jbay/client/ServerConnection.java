package a88.jbay.client;

import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import a88.jbay.common.network.Response;
import a88.jbay.util.JBayLogger;
import a88.jbay.view.ViewManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerConnection {
    private static ServerConnection instance;
    private ResponseHandler responseHandler;
    private final JBayLogger logger;
    private Thread listenerThread, heartbeatThread;
    private final ScheduledExecutorService scheduler;
    private volatile boolean listenerRunning = false;

    private ServerConnection() {
        this.logger = JBayLogger.getLogger(ServerConnection.class);
        responseHandler = ResponseHandler.getInstance();
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public synchronized static ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public void connect(String host, int port) throws UnknownHostException, IOException {
        logger.info("Connecting to server: " + host + ":" + port);
        socket = new Socket(host, port);
        socket.setKeepAlive(true);  // Enable TCP keep-alive

        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());

        logger.info("Connection successful to server: " + host + ":" + port);
    }

    //methods for sending requests
    public void send(Request request) throws IOException {
        logger.info("Sending request: " + request.getType().name());

        //automatically add sessionId
        request.put("sessionId", ClientSession.getInstance().getUser().getSessionId());
        synchronized(out) {
            out.writeObject(request);
            out.flush();
            out.reset();
        }
    }

    //listener
    public void startListener() {
        if (listenerRunning) {
            logger.warn("Listener already running, skipping startListener()");
            return;
        }

        listenerRunning = true;
        listenerThread = new Thread(() -> {
            try {
                while (!socket.isClosed() && listenerRunning) {
                    try {
                        Response response = (Response) in.readObject();
                        logger.debug("Received response: " + (String) response.getMessage());
                        Platform.runLater(() -> responseHandler.handle(response));
                    } catch (java.io.EOFException e) {
                        logger.info("Server closed the connection");
                        // handle disconnection - switch to connection view and show alert
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.WARNING, "Disconnected from server");
                            alert.showAndWait();
                            try {
                                ViewManager.displayScene("client/client-server-connect-view.fxml");
                            } catch (IOException ex) {
                                logger.error("Failed to switch to connection view: " + ex.getMessage(), ex);
                            }
                        });
                        break;
                    }
                }
            } catch (Exception e) {
                logger.error("Disconnected from server: " + e.getMessage(), e);
                // handle disconnection - switch to connection view and show alert
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "Disconnected from server");
                    alert.showAndWait();
                    try {
                        ViewManager.closePrimaryStage();
                        ViewManager.newStage("Welcome to jBay");
                        ViewManager.setResolution(600, 429);
                        ViewManager.displayScene("client/client-server-connect-view.fxml");
                    } catch (IOException ex) {
                        logger.error("Failed to switch to connection view: " + ex.getMessage(), ex);
                    }
                });
            } finally {
                logger.info("Listener thread ended");
                listenerRunning = false;
            }
        });

        listenerThread.start();
        startPing();
    }

    public void startPing() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                send(new Request(RequestType.PING));
            } catch (IOException e) {
                logger.error("Failed to send ping: " + e.getMessage(), e);
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    public void disconnect() {
        listenerRunning = false;
        scheduler.shutdown();
        listenerThread.interrupt();
        try {
            send(new Request(RequestType.MISC)
                    .put("disconnect", true));
            socket.close();
        } catch (IOException e) {
        }
    }
}
