package a88.jbay.common.auction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutoBidConfigTest {

    @Test
    void testFullConstructor() {
        AutoBidConfig config = new AutoBidConfig(1, 1000.0, 50.0);
        assertEquals(1, config.getUserId());
        assertEquals(1000.0, config.getMaxAmount(), 0.001);
        assertEquals(50.0, config.getIncrement(), 0.001);
    }

    @Test
    void testTwoArgConstructorDefaultsUserIdToZero() {
        AutoBidConfig config = new AutoBidConfig(500.0, 25.0);
        assertEquals(0, config.getUserId());
        assertEquals(500.0, config.getMaxAmount(), 0.001);
        assertEquals(25.0, config.getIncrement(), 0.001);
    }

    @Test
    void testGetters() {
        AutoBidConfig config = new AutoBidConfig(5, 999.99, 10.5);
        assertEquals(5, config.getUserId());
        assertEquals(999.99, config.getMaxAmount(), 0.001);
        assertEquals(10.5, config.getIncrement(), 0.001);
    }

    @Test
    void testSerializable() {
        assertTrue(java.io.Serializable.class.isAssignableFrom(AutoBidConfig.class));
    }
}
