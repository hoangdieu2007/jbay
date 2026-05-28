package a88.jbay.di;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DependencyInjectionContainerTest {

    private DependencyInjectionContainer container;

    @BeforeEach
    void setUp() {
        container = new DependencyInjectionContainer();
    }

    @Test
    void testRegisterAndGet() {
        container.registerSingleton(String.class, "hello");
        String result = container.getInstance(String.class);
        assertEquals("hello", result);
    }

    @Test
    void testGetReturnsSameSingletonInstance() {
        container.registerSingleton(StringBuilder.class, new StringBuilder("singleton"));
        StringBuilder a = container.getInstance(StringBuilder.class);
        StringBuilder b = container.getInstance(StringBuilder.class);
        assertSame(a, b);
    }

    @Test
    void testGetMissingTypeThrows() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> container.getInstance(Integer.class));
        assertTrue(ex.getMessage().contains("NO INSTANCE OF"));
    }

    @Test
    void testClearRemovesAll() {
        container.registerSingleton(String.class, "hello");
        container.registerSingleton(Integer.class, 42);
        container.clear();
        assertThrows(IllegalArgumentException.class, () -> container.getInstance(String.class));
        assertThrows(IllegalArgumentException.class, () -> container.getInstance(Integer.class));
    }
}
