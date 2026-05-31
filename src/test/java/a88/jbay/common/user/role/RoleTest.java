package a88.jbay.common.user.role;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @Test
    void testEnumValues() {
        assertEquals(4, Role.values().length);
        assertEquals(Role.GUEST, Role.valueOf("GUEST"));
        assertEquals(Role.BAN, Role.valueOf("BAN"));
        assertEquals(Role.USER, Role.valueOf("USER"));
        assertEquals(Role.ADMIN, Role.valueOf("ADMIN"));
    }

    @Test
    void testFromStringValid() {
        assertEquals(Role.ADMIN, Role.fromString("ADMIN"));
        assertEquals(Role.USER, Role.fromString("USER"));
        assertEquals(Role.BAN, Role.fromString("BAN"));
    }

    @Test
    void testFromStringCaseInsensitive() {
        assertEquals(Role.ADMIN, Role.fromString("admin"));
        assertEquals(Role.USER, Role.fromString("user"));
        assertEquals(Role.BAN, Role.fromString("ban"));
    }

    @Test
    void testFromStringInvalidReturnsGuest() {
        assertEquals(Role.GUEST, Role.fromString("INVALID"));
        assertEquals(Role.GUEST, Role.fromString(""));
    }

    @Test
    void testFromStringNullReturnsGuest() {
        assertEquals(Role.GUEST, Role.fromString(null));
    }
}
