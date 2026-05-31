package a88.jbay.server;

import a88.jbay.common.user.User;
import a88.jbay.common.network.Request;
import a88.jbay.common.network.Response;
import a88.jbay.di.ApplicationContext;
import a88.jbay.di.DependencyInjectionContainer;
import a88.jbay.system.update.ConnectionSystem;
import a88.jbay.system.user.UserSystem;
import a88.jbay.util.JBayLogger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
    Handles individual client connections and socket management.
    Responsible for connection lifecycle, I/O operations, and delegating request processing to RequestHandler.
 */

public class ClientConnection implements Runnable {
    private static final AtomicInteger ID_GEN = new AtomicInteger(0);

    private final Socket socket;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;
    private final int connectionId;
    private final JBayLogger logger;
    private final RequestHandler requestHandler;
    private final ConnectionSystem connectionSystem;
    private final UserSystem userSystem;
    private final ReentrantLock sendLock = new ReentrantLock();
    private final ScheduledExecutorService sendWatchdog;

    private volatile User userCache;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public static final String MSG_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String MSG_LOGOUT_SUCCESS = "LOGOUT_SUCCESS";

    public ClientConnection(Socket socket, ConnectionSystem connectionSystem,
                            UserSystem userSystem, RequestHandler requestHandler) throws IOException {
        this.connectionId = ID_GEN.incrementAndGet();
        this.socket = socket;
        socket.setKeepAlive(true);

        this.out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());

        this.userCache = new User();
        this.logger = JBayLogger.getLogger(ClientConnection.class);
        this.connectionSystem = connectionSystem;
        this.userSystem = userSystem;
        this.requestHandler = requestHandler;
        this.sendWatchdog = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "client-send-watchdog-" + connectionId);
            thread.setDaemon(true);
            return thread;
        });
    }

    public int getConnectionId() {
        return connectionId;
    }

    public boolean isActive() {
        return !closed.get() && socket != null && !socket.isClosed() && !Thread.currentThread().isInterrupted();
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

                    Response response = requestHandler.handleRequest(request);
                    if (response == null) {
                        break;
                    }

                    // update cache if login success
                    if (MSG_LOGIN_SUCCESS.equals(response.getMessage())) {
                        this.userCache = (User) response.getPayload();
                        connectionSystem.register(this);
                    } else if (MSG_LOGOUT_SUCCESS.equals(response.getMessage())) {
                        connectionSystem.unregister(this); // this has to be called before setting userCache to new User()
                        this.userCache = new User();
                    }

                    //prevent crash when update and response sends at the same time
                    if (!send(response)) {
                        break;
                    }
                } catch (IOException e) {
                    System.err.println("Error reading request: " + e.getMessage());
                    break;
                }
                catch (ClassNotFoundException e) {
                    System.err.println("Invalid request object received: " + e.getMessage());
                    break;
                } catch (RuntimeException e) {
                    logger.error("Error handling request: " + e.getMessage(), e);
                    if (!send(new Response(false, "SERVER_ERROR", null))) {
                        break;
                    }
                }
            }
        } finally {
            // later add clean up codes here!
            connectionSystem.unregister(this);
            String sessionId = userCache.getSessionId();
            if (sessionId != null) {
                userSystem.logout(sessionId);
            }
            close();
            Thread.currentThread().interrupt();
        }
    }

    public boolean send(Response response) {
        if (!isActive()) {
            return false;
        }
        logger.info("Sending response: " + response.getMessage());

        boolean locked = false;
        ScheduledFuture<?> timeoutTask = null;
        try {
            locked = sendLock.tryLock(2, TimeUnit.SECONDS);
            if (!locked) {
                logger.warn("Timed out waiting for send lock. Closing connection: " + connectionId);
                close();
                return false;
            }

            timeoutTask = sendWatchdog.schedule(() -> {
                logger.warn("Timed out sending response. Closing connection: " + connectionId);
                close();
            }, 10, TimeUnit.SECONDS);

            out.writeObject(response);
            out.flush();
            out.reset();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            close();
            return false;
        } catch (IOException e) {
            logger.error("Error sending response: " + e.getMessage(), e);
            close();
            return false;
        } finally {
            if (timeoutTask != null) {
                timeoutTask.cancel(false);
            }
            if (locked) {
                sendLock.unlock();
            }
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
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        closeResources(out, in, socket);
        sendWatchdog.shutdownNow();
    }
}

