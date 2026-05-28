package a88.jbay.system.update;

import a88.jbay.common.network.Response;
import a88.jbay.common.user.User;
import a88.jbay.server.ClientConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConnectionSystemTest {

    @Mock
    private ClientConnection clientConnection;

    @Mock
    private User user;

    private ConnectionSystem connectionSystem;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        connectionSystem = new ConnectionSystem();
        when(clientConnection.getUserCache()).thenReturn(user);
        when(user.getId()).thenReturn(1);
    }

    @Test
    @DisplayName("Should register a client connection")
    void testRegister() {
        connectionSystem.register(clientConnection);

        Map<Integer, Set<ClientConnection>> connections = connectionSystem.getConnections();
        assertTrue(connections.containsKey(1));
        assertTrue(connections.get(1).contains(clientConnection));
    }

    @Test
    @DisplayName("Should register multiple connections for same user")
    void testRegister_MultipleConnections() {
        ClientConnection conn2 = mock(ClientConnection.class);
        when(conn2.getUserCache()).thenReturn(user);

        connectionSystem.register(clientConnection);
        connectionSystem.register(conn2);

        assertEquals(2, connectionSystem.getConnections().get(1).size());
    }

    @Test
    @DisplayName("Should unregister a client connection")
    void testUnregister() {
        connectionSystem.register(clientConnection);

        connectionSystem.unregister(clientConnection);

        assertTrue(connectionSystem.getConnections().isEmpty());
    }

    @Test
    @DisplayName("Should unregister user by id")
    void testUnregister_ByUserId() {
        connectionSystem.register(clientConnection);

        connectionSystem.unregister(1);

        assertTrue(connectionSystem.getConnections().isEmpty());
    }

    @Test
    @DisplayName("Should not fail when unregistering non-existent connection")
    void testUnregister_NonExistent() {
        connectionSystem.unregister(999);

        assertTrue(connectionSystem.getConnections().isEmpty());
    }

    @Test
    @DisplayName("Should send response to user")
    void testSendToUser() throws InterruptedException {
        connectionSystem.register(clientConnection);
        Response response = new Response(true, "TEST", "data");
        when(clientConnection.send(response)).thenReturn(true);

        connectionSystem.sendToUser(1, response);

        Thread.sleep(100);
        verify(clientConnection, timeout(500)).send(response);
    }

    @Test
    @DisplayName("Should not send to non-existent user")
    void testSendToUser_UserNotFound() {
        Response response = new Response(true, "TEST", "data");

        connectionSystem.sendToUser(999, response);

        verify(clientConnection, never()).send(any());
    }

    @Test
    @DisplayName("Should send response to multiple users")
    void testSendToUsers() throws InterruptedException {
        User user2 = mock(User.class);
        when(user2.getId()).thenReturn(2);
        ClientConnection conn2 = mock(ClientConnection.class);
        when(conn2.getUserCache()).thenReturn(user2);

        connectionSystem.register(clientConnection);
        connectionSystem.register(conn2);

        Response response = new Response(true, "BROADCAST", "data");
        when(clientConnection.send(response)).thenReturn(true);
        when(conn2.send(response)).thenReturn(true);

        connectionSystem.sendToUsers(Set.of(1, 2), response);

        Thread.sleep(100);
        verify(clientConnection, timeout(500)).send(response);
        verify(conn2, timeout(500)).send(response);
    }

    @Test
    @DisplayName("Should broadcast to all connected users")
    void testBroadcast() throws InterruptedException {
        connectionSystem.register(clientConnection);
        Response response = new Response(true, "BROADCAST", "data");
        when(clientConnection.send(response)).thenReturn(true);

        connectionSystem.broadcast(response);

        Thread.sleep(100);
        verify(clientConnection, timeout(500)).send(response);
    }

    @Test
    @DisplayName("Should unregister and close connection when send fails")
    void testSendToUser_ConnectionFails() throws InterruptedException {
        connectionSystem.register(clientConnection);
        Response response = new Response(true, "TEST", "data");
        when(clientConnection.send(response)).thenReturn(false);

        connectionSystem.sendToUser(1, response);

        Thread.sleep(100);
        verify(clientConnection, timeout(500)).send(response);
        verify(clientConnection).close();
        assertTrue(connectionSystem.getConnections().isEmpty());
    }

    @Test
    @DisplayName("Should return empty connections map initially")
    void testInitialState() {
        assertTrue(connectionSystem.getConnections().isEmpty());
    }
}
