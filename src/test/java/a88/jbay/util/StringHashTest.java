package a88.jbay.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringHashTest {

    @Test
    void testHashNullReturnsEmpty() {
        assertEquals("", StringHash.hash(null));
    }

    @Test
    void testHashEmptyString() {
        String hash = StringHash.hash("");
        assertNotNull(hash);
        assertFalse(hash.isEmpty());
    }

    @Test
    void testHashDeterministic() {
        String input = "hello";
        assertEquals(StringHash.hash(input), StringHash.hash(input));
    }

    @Test
    void testHashDifferentInputs() {
        assertNotEquals(StringHash.hash("abc"), StringHash.hash("xyz"));
    }

    @Test
    void testHashKnownValue() {
        String hash = StringHash.hash("hello");
        assertEquals(44, hash.length());
    }

    @Test
    void testHashNonEmptyForNonEmptyInput() {
        assertFalse(StringHash.hash("world").isEmpty());
    }
}
