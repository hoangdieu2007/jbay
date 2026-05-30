package a88.jbay.system.user;

import a88.jbay.common.user.User;
import a88.jbay.common.user.UserData;
import a88.jbay.common.user.role.Role;
import a88.jbay.data.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserSystemTest {

    @Mock
    private UserRepository userRepository;

    private UserSystem userSystem;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userSystem = new UserSystem(userRepository);
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void testLogin_Success() {
        String username = "testuser";
        String password = "correctpassword";
        String hashedPassword = a88.jbay.util.StringHash.hash(password);

        when(userRepository.findByUsername(username)).thenReturn(
                new UserData(1, username, "USER", hashedPassword)
        );
        when(userRepository.createSession(anyString(), any())).thenReturn(true);

        User result = userSystem.login(username, password);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals(username, result.getUsername());
        assertEquals(Role.USER, result.getRole());
        assertNotNull(result.getSessionId());
        verify(userRepository).findByUsername(username);
        verify(userRepository).createSession(anyString(), any());
    }

    @Test
    @DisplayName("Should return null when user not found")
    void testLogin_UserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(null);

        User result = userSystem.login("unknown", "password");

        assertNull(result);
        verify(userRepository, never()).createSession(anyString(), any());
    }

    @Test
    @DisplayName("Should return null when password is incorrect")
    void testLogin_WrongPassword() {
        when(userRepository.findByUsername("testuser")).thenReturn(
                new UserData(1, "testuser", "USER", "correct_hash")
        );

        User result = userSystem.login("testuser", "wrong_password");

        assertNull(result);
        verify(userRepository, never()).createSession(anyString(), any());
    }

    @Test
    @DisplayName("Should return null when session creation fails")
    void testLogin_SessionCreationFails() {
        String username = "testuser";
        String password = "password";
        String hashedPassword = a88.jbay.util.StringHash.hash(password);

        when(userRepository.findByUsername(username)).thenReturn(
                new UserData(1, username, "USER", hashedPassword)
        );
        when(userRepository.createSession(anyString(), any())).thenReturn(false);

        User result = userSystem.login(username, password);

        assertNull(result);
    }

    @Test
    @DisplayName("Should register new user successfully")
    void testRegister_Success() {
        when(userRepository.usernameExists("newuser")).thenReturn(false);
        when(userRepository.createUser(eq("newuser"), anyString(), eq("USER"), eq(new byte[]{1, 2, 3})))
                .thenReturn(true);

        boolean result = userSystem.register("newuser", "password", "USER", new byte[]{1, 2, 3});

        assertTrue(result);
        verify(userRepository).usernameExists("newuser");
        verify(userRepository).createUser(eq("newuser"), anyString(), eq("USER"), any());
    }

    @Test
    @DisplayName("Should reject registration when username already exists")
    void testRegister_UsernameExists() {
        when(userRepository.usernameExists("existing")).thenReturn(true);

        boolean result = userSystem.register("existing", "password", "USER", null);

        assertFalse(result);
        verify(userRepository, never()).createUser(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should logout and delete session")
    void testLogout() {
        userSystem.logout("session123");

        verify(userRepository).deleteSession("session123");
    }

    @Test
    @DisplayName("Should find user by session id")
    void testFindBySessionId() {
        User expected = new User(1, Role.USER, "testuser", "session123");
        when(userRepository.findBySessionId("session123")).thenReturn(expected);

        User result = userSystem.findBySessionId("session123");

        assertSame(expected, result);
    }

    @Test
    @DisplayName("Should return null for non-existent session")
    void testFindBySessionId_NotFound() {
        when(userRepository.findBySessionId("nonexistent")).thenReturn(null);

        User result = userSystem.findBySessionId("nonexistent");

        assertNull(result);
    }

    @Test
    @DisplayName("Should get all normal users for admin")
    void testGetAllNormalUsersForAdmin() {
        List<User> expected = List.of(new User(1, Role.USER, "user1"));
        when(userRepository.getAllNormalUsers()).thenReturn(expected);

        List<User> result = userSystem.getAllNormalUsersForAdmin();

        assertEquals(1, result.size());
        verify(userRepository).getAllNormalUsers();
    }

    @Test
    @DisplayName("Should get user by name")
    void testGetUserByName() {
        when(userRepository.findByUsername("testuser")).thenReturn(
                new UserData(1, "testuser", "USER", "password")
        );

        User result = userSystem.getUserByName("testuser");

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals(Role.USER, result.getRole());
    }

    @Test
    @DisplayName("Should return null when user not found by name")
    void testGetUserByName_NotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(null);

        User result = userSystem.getUserByName("unknown");

        assertNull(result);
    }

    @Test
    @DisplayName("Should get QR code by user id")
    void testGetQr() {
        byte[] expectedQr = new byte[]{1, 2, 3};
        when(userRepository.getQr(1)).thenReturn(expectedQr);

        byte[] result = userSystem.getQr(1);

        assertArrayEquals(expectedQr, result);
        verify(userRepository).getQr(1);
    }
}
