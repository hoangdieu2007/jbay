package a88.jbay.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MainClientTest {

    @Test
    @DisplayName("Should stop safely when no server connection is active")
    void testStop_NoActiveConnection() {
        MainClient mainClient = new MainClient();

        assertDoesNotThrow(mainClient::stop);
    }
}
