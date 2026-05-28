package a88.jbay.data;

import a88.jbay.common.user.User;
import a88.jbay.common.user.UserData;
import a88.jbay.dao.UserDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserRepositoryTest {

    @Mock
    private UserDAO userDAO;

    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userRepository = new UserRepository(userDAO);
    }

    @Test
    @DisplayName("Should find user by username via DAO")
    void testFindByUsername() {
        UserData expected = new UserData(1, "testuser", "BIDDER", "password");
        when(userDAO.findByUsername("testuser")).thenReturn(expected);

        UserData result = userRepository.findByUsername("testuser");

        assertSame(expected, result);
        verify(userDAO).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should return null when username not found")
    void testFindByUsername_NotFound() {
        when(userDAO.findByUsername("unknown")).thenReturn(null);

        UserData result = userRepository.findByUsername("unknown");

        assertNull(result);
    }

    @Test
    @DisplayName("Should find user by user id via DAO")
    void testFindByUserId() {
        UserData expected = new UserData(1, "testuser", "BIDDER", "password");
        when(userDAO.findByUserId(1)).thenReturn(expected);

        UserData result = userRepository.findByUserId(1);

        assertSame(expected, result);
        verify(userDAO).findByUserId(1);
    }

    @Test
    @DisplayName("Should check username existence via DAO")
    void testUsernameExists() {
        when(userDAO.existsByUsername("testuser")).thenReturn(true);

        boolean result = userRepository.usernameExists("testuser");

        assertTrue(result);
        verify(userDAO).existsByUsername("testuser");
    }

    @Test
    @DisplayName("Should return false when username does not exist")
    void testUsernameExists_NotExists() {
        when(userDAO.existsByUsername("unknown")).thenReturn(false);

        boolean result = userRepository.usernameExists("unknown");

        assertFalse(result);
    }

    @Test
    @DisplayName("Should create user and return true on success")
    void testCreateUser_Success() {
        when(userDAO.insertUser("newuser", "hash", "BIDDER", new byte[]{1, 2, 3})).thenReturn(1);

        boolean result = userRepository.createUser("newuser", "hash", "BIDDER", new byte[]{1, 2, 3});

        assertTrue(result);
        verify(userDAO).insertUser("newuser", "hash", "BIDDER", new byte[]{1, 2, 3});
    }

    @Test
    @DisplayName("Should return false when DAO insert returns -1")
    void testCreateUser_Failure() {
        when(userDAO.insertUser(anyString(), anyString(), anyString(), any())).thenReturn(-1);

        boolean result = userRepository.createUser("newuser", "hash", "BIDDER", null);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should create session and return true")
    void testCreateSession() {
        User user = new User(1, "BIDDER", "testuser", "session123");

        boolean result = userRepository.createSession("session123", user);

        assertTrue(result);
        assertSame(user, userRepository.findBySessionId("session123"));
    }

    @Test
    @DisplayName("Should delete session from cache")
    void testDeleteSession() {
        User user = new User(1, "BIDDER", "testuser", "session123");
        userRepository.createSession("session123", user);

        userRepository.deleteSession("session123");

        assertNull(userRepository.findBySessionId("session123"));
    }

    @Test
    @DisplayName("Should find session by session id")
    void testFindBySessionId() {
        User user = new User(1, "BIDDER", "testuser", "session123");
        userRepository.createSession("session123", user);

        User result = userRepository.findBySessionId("session123");

        assertSame(user, result);
    }

    @Test
    @DisplayName("Should return null for non-existent session")
    void testFindBySessionId_NotFound() {
        User result = userRepository.findBySessionId("nonexistent");

        assertNull(result);
    }

    @Test
    @DisplayName("Should get all normal users and convert to User objects")
    void testGetAllNormalUsers() {
        List<UserData> dataList = List.of(
                new UserData(1, "user1", "BIDDER", "pass1"),
                new UserData(2, "user2", "SELLER", "pass2")
        );
        when(userDAO.getAllNormalUsers()).thenReturn(dataList);

        List<User> result = userRepository.getAllNormalUsers();

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getId());
        assertEquals("user1", result.get(0).getUsername());
        assertEquals("BIDDER", result.get(0).getRole());
        assertEquals(2, result.get(1).getId());
        verify(userDAO).getAllNormalUsers();
    }

    @Test
    @DisplayName("Should get QR code via DAO")
    void testGetQr() {
        byte[] expectedQr = new byte[]{1, 2, 3};
        when(userDAO.getQr(1)).thenReturn(expectedQr);

        byte[] result = userRepository.getQr(1);

        assertArrayEquals(expectedQr, result);
        verify(userDAO).getQr(1);
    }

    @Test
    @DisplayName("Should update role and evict banned user session")
    void testUpdateRole_BanEvictsSession() {
        User user = new User(1, "BIDDER", "testuser", "session123");
        userRepository.createSession("session123", user);
        when(userDAO.changeUserRole(1, "BAN")).thenReturn(true);

        boolean result = userRepository.updateRole(1, "BAN");

        assertTrue(result);
        assertNull(userRepository.findBySessionId("session123"));
        verify(userDAO).changeUserRole(1, "BAN");
    }

    @Test
    @DisplayName("Should update role and refresh non-ban session cache")
    void testUpdateRole_NonBanRefreshesSession() {
        User user = new User(1, "BIDDER", "testuser", "session123");
        userRepository.createSession("session123", user);
        when(userDAO.changeUserRole(1, "SELLER")).thenReturn(true);
        UserData freshData = new UserData(1, "testuser", "SELLER", "password");
        when(userDAO.findByUserId(1)).thenReturn(freshData);

        boolean result = userRepository.updateRole(1, "SELLER");

        assertTrue(result);
        User refreshed = userRepository.findBySessionId("session123");
        assertNotNull(refreshed);
        assertEquals("SELLER", refreshed.getRole());
        verify(userDAO).changeUserRole(1, "SELLER");
    }

    @Test
    @DisplayName("Should return false when DAO updateRole fails")
    void testUpdateRole_DAOFails() {
        when(userDAO.changeUserRole(1, "ADMIN")).thenReturn(false);

        boolean result = userRepository.updateRole(1, "ADMIN");

        assertFalse(result);
    }
}
