package a88.jbay.system.user;

import a88.jbay.common.user.User;
import a88.jbay.common.user.UserData;
import a88.jbay.dao.UserDAO;
import a88.jbay.data.UserRepository;
import a88.jbay.server.ClientConnection;
import a88.jbay.system.AuctionSystem;
import a88.jbay.system.update.ConnectionSystem;
import a88.jbay.util.JBayLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminServiceTest {

    @Mock
    private UserDAO userDAO;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConnectionSystem connectionSystem;

    @Mock
    private AuctionSystem auctionSystem;

    @Mock
    private UserSystem userSystem;

    @Mock
    private ClientConnection clientConnection;

    @Mock
    private User mockUser;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adminService = new AdminService(
                userDAO, userRepository, connectionSystem, auctionSystem, userSystem
        );
    }

    @Test
    @DisplayName("banUser should return null when user not found")
    void testBanUser_UserNotFound() {
        when(userDAO.findByUserId(999)).thenReturn(null);

        User result = adminService.banUser(999);

        assertNull(result);
        verify(userRepository, never()).updateRole(anyInt(), anyString());
        verify(connectionSystem, never()).sendToUser(anyInt(), any());
        verify(auctionSystem, never()).cancelAuctionsBySellerId(anyInt());
    }

    @Test
    @DisplayName("banUser should return null when role update fails")
    void testBanUser_UpdateRoleFails() {
        UserData userData = new UserData(1, "target_user", "USER", "hash");
        when(userDAO.findByUserId(1)).thenReturn(userData);
        when(userRepository.updateRole(1, "BAN")).thenReturn(false);

        User result = adminService.banUser(1);

        assertNull(result);
        verify(connectionSystem, never()).sendToUser(anyInt(), any());
    }

    @Test
    @DisplayName("banUser should ban user with live connections")
    void testBanUser_WithConnections() {
        UserData userData = new UserData(1, "target_user", "USER", "hash");
        when(userDAO.findByUserId(1)).thenReturn(userData);
        when(userRepository.updateRole(1, "BAN")).thenReturn(true);

        Set<ClientConnection> connections = new HashSet<>();
        connections.add(clientConnection);
        Map<Integer, Set<ClientConnection>> connMap = new HashMap<>();
        connMap.put(1, connections);
        when(connectionSystem.getConnections()).thenReturn(connMap);
        when(clientConnection.getUserCache()).thenReturn(mockUser);
        when(mockUser.getSessionId()).thenReturn("session123");

        User result = adminService.banUser(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("BAN", result.getRole());
        assertEquals("target_user", result.getUsername());

        verify(connectionSystem).sendToUser(eq(1), any());
        verify(userSystem).logout("session123");
        verify(connectionSystem).unregister(1);
        verify(auctionSystem).cancelAuctionsBySellerId(1);
        verify(auctionSystem).reloadSystem();
    }

    @Test
    @DisplayName("banUser should ban user without live connections")
    void testBanUser_WithoutConnections() {
        UserData userData = new UserData(2, "offline_user", "USER", "hash");
        when(userDAO.findByUserId(2)).thenReturn(userData);
        when(userRepository.updateRole(2, "BAN")).thenReturn(true);

        Map<Integer, Set<ClientConnection>> connMap = new HashMap<>();
        when(connectionSystem.getConnections()).thenReturn(connMap);

        User result = adminService.banUser(2);

        assertNotNull(result);
        assertEquals("BAN", result.getRole());
        verify(connectionSystem).sendToUser(eq(2), any());
        verify(connectionSystem).unregister(2);
        verify(userSystem, never()).logout(anyString());
        verify(auctionSystem).cancelAuctionsBySellerId(2);
        verify(auctionSystem).reloadSystem();
    }

    @Test
    @DisplayName("unbanUser should return null when user not found")
    void testUnbanUser_UserNotFound() {
        when(userDAO.findByUserId(999)).thenReturn(null);

        User result = adminService.unbanUser(999);

        assertNull(result);
        verify(userRepository, never()).updateRole(anyInt(), anyString());
        verify(auctionSystem, never()).reloadSystem();
    }

    @Test
    @DisplayName("unbanUser should return null when role update fails")
    void testUnbanUser_UpdateRoleFails() {
        UserData userData = new UserData(3, "banned_user", "BAN", "hash");
        when(userDAO.findByUserId(3)).thenReturn(userData);
        when(userRepository.updateRole(3, "USER")).thenReturn(false);

        User result = adminService.unbanUser(3);

        assertNull(result);
        verify(auctionSystem, never()).reloadSystem();
    }

    @Test
    @DisplayName("unbanUser should successfully unban a user")
    void testUnbanUser_Success() {
        UserData userData = new UserData(4, "banned_user", "BAN", "hash");
        when(userDAO.findByUserId(4)).thenReturn(userData);
        when(userRepository.updateRole(4, "USER")).thenReturn(true);

        User result = adminService.unbanUser(4);

        assertNotNull(result);
        assertEquals(4, result.getId());
        assertEquals("USER", result.getRole());
        assertEquals("banned_user", result.getUsername());
        verify(auctionSystem).reloadSystem();
    }
}
