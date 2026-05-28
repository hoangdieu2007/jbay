package a88.jbay.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class HelloApplicationTest {

    @Test
    @DisplayName("Should run auction demo without throwing")
    void testMain() {
        assertDoesNotThrow(() -> HelloApplication.main(new String[0]));
    }
}
