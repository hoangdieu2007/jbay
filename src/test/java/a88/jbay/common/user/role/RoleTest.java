package a88.jbay.common.user.role;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @Test
    void testEnumValues() {
        assertEquals(3, Role.values().length);
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
    void testFromStringInvalidReturnsBan() {
        assertEquals(Role.BAN, Role.fromString("INVALID"));
        assertEquals(Role.BAN, Role.fromString(""));
    }

    @Test
    void testFromStringNullReturnsBan() {
        assertEquals(Role.BAN, Role.fromString(null));
    }
}
