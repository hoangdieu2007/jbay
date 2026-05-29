package a88.jbay.client;

import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import a88.jbay.common.user.User;
import a88.jbay.view.ViewManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServerConnectionTest {

    private ServerConnection connection;
    private ResponseHandler responseHandler;
    private ClientSession clientSession;

    @BeforeEach
    void setUp() {
        responseHandler = mock(ResponseHandler.class);
        clientSession = new ClientSession();
        connection = new ServerConnection(responseHandler, clientSession, mock(ViewManager.class));
    }

    @AfterEach
    void tearDown() {
        connection.disconnect();
    }

    @Test
    @DisplayName("Should throw when sending before connecting")
    void testSend_NotConnected() {
        IOExceptionAssert.assertThrowsIOException(() -> connection.send(new Request(RequestType.LOGIN)));
    }

    @Test
    @DisplayName("Should ignore listener and ping start before connecting")
    void testStartBeforeConnect() {
        assertDoesNotThrow(connection::startListener);
        assertDoesNotThrow(connection::startPing);
    }

    @Test
    @DisplayName("Should disconnect safely without an active connection")
    void testDisconnect_NotConnected() {
        assertDoesNotThrow(connection::disconnect);
    }

    @Test
    @DisplayName("Should send request with session id over a socket connection")
    void testSend_AddsSessionIdAndSerializesRequest() throws Exception {
        clientSession.setUser(new User(5, "USER", "alice", "session-123"));
        ArrayBlockingQueue<Request> receivedRequests = new ArrayBlockingQueue<>(1);

        try (ServerSocket serverSocket = new ServerSocket(0)) {
            Thread serverThread = new Thread(() -> acceptOneRequest(serverSocket, receivedRequests));
            serverThread.setDaemon(true);
            serverThread.start();

            connection.connect("127.0.0.1", serverSocket.getLocalPort());
            Request request = new Request(RequestType.BID).put("auctionId", 10);

            connection.send(request);

            Request received = receivedRequests.poll(5, TimeUnit.SECONDS);
            assertNotNull(received);
            assertEquals(RequestType.BID, received.getType());
            assertEquals(10, received.get("auctionId"));
            assertEquals("session-123", received.get("sessionId"));
        }
    }

    @Test
    @DisplayName("Should not add blank or none session id to outgoing request")
    void testSend_SkipsInvalidSessionId() throws Exception {
        clientSession.setUser(new User(5, "USER", "alice", "none"));
        ArrayBlockingQueue<Request> receivedRequests = new ArrayBlockingQueue<>(1);

        try (ServerSocket serverSocket = new ServerSocket(0)) {
            Thread serverThread = new Thread(() -> acceptOneRequest(serverSocket, receivedRequests));
            serverThread.setDaemon(true);
            serverThread.start();

            connection.connect("127.0.0.1", serverSocket.getLocalPort());
            connection.send(new Request(RequestType.LOGOUT));

            Request received = receivedRequests.poll(5, TimeUnit.SECONDS);
            assertNotNull(received);
            assertNull(received.get("sessionId"));
        }
    }

    private static void acceptOneRequest(
            ServerSocket serverSocket,
            ArrayBlockingQueue<Request> receivedRequests
    ) {
        try (
                Socket socket = serverSocket.accept();
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            out.flush();
            receivedRequests.offer((Request) in.readObject());
        } catch (Exception ignored) {
            // Test thread observes failure by timing out waiting for the request.
        }
    }

    private static class IOExceptionAssert {
        private interface IoAction {
            void run() throws java.io.IOException;
        }

        private static void assertThrowsIOException(IoAction action) {
            assertThrows(java.io.IOException.class, action::run);
        }
    }
}