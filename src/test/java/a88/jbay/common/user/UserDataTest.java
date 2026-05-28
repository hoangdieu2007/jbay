package a88.jbay.common.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserDataTest {

    @Test
    void testConstructionAndAccessors() {
        UserData user = new UserData(1, "alice", "USER", "secret");

        assertEquals(1, user.id());
        assertEquals("alice", user.username());
        assertEquals("USER", user.role());
        assertEquals("secret", user.password());
    }
}
