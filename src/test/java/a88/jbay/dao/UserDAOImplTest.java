package a88.jbay.dao;

import a88.jbay.common.user.UserData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserDAOImplTest extends DaoTestBase {

    private final UserDAO userDAO = new UserDAOImpl(dbController);

    @Test
    @DisplayName("Should insert and find user by username")
    void testFindByUsername() throws Exception {
        insertUser("testuser", "hash123", "BIDDER", null);

        UserData result = userDAO.findByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.username());
        assertEquals("hash123", result.password());
        assertEquals("BIDDER", result.role());
    }

    @Test
    @DisplayName("Should return null when username not found")
    void testFindByUsername_NotFound() {
        UserData result = userDAO.findByUsername("nonexistent");

        assertNull(result);
    }

    @Test
    @DisplayName("Should find user by user id")
    void testFindByUserId() throws Exception {
        int id = insertUser("user1", "pass", "SELLER", null);

        UserData result = userDAO.findByUserId(id);

        assertNotNull(result);
        assertEquals("user1", result.username());
        assertEquals("SELLER", result.role());
    }

    @Test
    @DisplayName("Should return null when user id not found")
    void testFindByUserId_NotFound() {
        UserData result = userDAO.findByUserId(999);

        assertNull(result);
    }

    @Test
    @DisplayName("Should check username existence")
    void testExistsByUsername_Exists() throws Exception {
        insertUser("existing", "pass", "BIDDER", null);

        assertTrue(userDAO.existsByUsername("existing"));
    }

    @Test
    @DisplayName("Should return false when username does not exist")
    void testExistsByUsername_NotExists() {
        assertFalse(userDAO.existsByUsername("nonexistent"));
    }

    @Test
    @DisplayName("Should insert user and return generated id")
    void testInsertUser() {
        int id = userDAO.insertUser("newuser", "hash", "BIDDER", null);

        assertTrue(id > 0);
        UserData saved = userDAO.findByUserId(id);
        assertNotNull(saved);
        assertEquals("newuser", saved.username());
    }

    @Test
    @DisplayName("Should change user role")
    void testChangeUserRole() throws Exception {
        int id = insertUser("user1", "pass", "BIDDER", null);

        boolean updated = userDAO.changeUserRole(id, "SELLER");

        assertTrue(updated);
        UserData user = userDAO.findByUserId(id);
        assertEquals("SELLER", user.role());
    }

    @Test
    @DisplayName("Should return false when changing role for non-existent user")
    void testChangeUserRole_NotFound() {
        boolean updated = userDAO.changeUserRole(999, "ADMIN");

        assertFalse(updated);
    }

    @Test
    @DisplayName("Should get all normal users excluding admins")
    void testGetAllNormalUsers() throws Exception {
        insertUser("admin", "pass", "ADMIN", null);
        insertUser("bidder1", "pass", "BIDDER", null);
        insertUser("seller1", "pass", "SELLER", null);

        List<UserData> normalUsers = userDAO.getAllNormalUsers();

        assertEquals(2, normalUsers.size());
        assertTrue(normalUsers.stream().noneMatch(u -> u.role().equals("ADMIN")));
    }

    @Test
    @DisplayName("Should get QR code by user id")
    void testGetQr() throws Exception {
        byte[] qrData = new byte[]{1, 2, 3, 4, 5};
        int id = insertUser("qruser", "pass", "BIDDER", qrData);

        byte[] result = userDAO.getQr(id);

        assertArrayEquals(qrData, result);
    }

    @Test
    @DisplayName("Should return null when QR is null")
    void testGetQr_Null() throws Exception {
        int id = insertUser("nqruser", "pass", "BIDDER", null);

        byte[] result = userDAO.getQr(id);

        assertNull(result);
    }

    @Test
    @DisplayName("Should return null when user not found for QR")
    void testGetQr_NotFound() {
        byte[] result = userDAO.getQr(999);

        assertNull(result);
    }
}
