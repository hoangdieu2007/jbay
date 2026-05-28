package a88.jbay.view;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ViewManagerTest {

    private ViewManager viewManager;

    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> throwable = new AtomicReference<>();
        Thread starter = new Thread(() -> {
            try {
                new JFXPanel();
            } catch (Throwable error) {
                throwable.set(error);
            } finally {
                latch.countDown();
            }
        }, "javafx-test-starter");
        starter.setDaemon(true);
        starter.start();

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Timed out starting JavaFX toolkit");
        if (throwable.get() instanceof Exception exception) {
            throw exception;
        }
        if (throwable.get() instanceof Error error) {
            throw error;
        }
        Platform.setImplicitExit(false);
    }

    @BeforeEach
    void setUp() throws Exception {
        resetStaticField("instance");
        resetStaticField("primaryStage");
        viewManager = ViewManager.getInstance();
    }

    @Test
    @DisplayName("Should return the same singleton instance")
    void testGetInstance() {
        ViewManager anotherInstance = ViewManager.getInstance();

        assertSame(viewManager, anotherInstance);
    }

    @Test
    @DisplayName("Should set stage resolution")
    void testSetResolution() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();
            ViewManager.setPrimaryStage(stage);

            ViewManager.setResolution(800, 600);

            assertEquals(800, stage.getWidth());
            assertEquals(600, stage.getHeight());
            stage.close();
        });
    }

    @Test
    @DisplayName("Should close primary stage when one is set")
    void testClosePrimaryStage() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();
            ViewManager.setPrimaryStage(stage);

            stage.show();
            ViewManager.closePrimaryStage();

            assertFalse(stage.isShowing());
        });
    }

    @Test
    @DisplayName("Should ignore close request when primary stage is absent")
    void testClosePrimaryStage_NoStage() {
        assertDoesNotThrow(ViewManager::closePrimaryStage);
    }

    @Test
    @DisplayName("Should throw when main scene area has not been set")
    void testLoadIntoMainScene_MissingMainScene() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> viewManager.loadIntoMainScene("test-view.fxml")
        );

        assertEquals("Main Scene Area hasn't been set!", exception.getMessage());
    }

    @Test
    @DisplayName("Should display FXML scene using current stage size")
    void testDisplayScene() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();
            stage.initStyle(StageStyle.UNDECORATED);
            ViewManager.setPrimaryStage(stage);
            ViewManager.setResolution(640, 360);

            ViewManager.displayScene("test-view.fxml");

            Scene scene = stage.getScene();
            assertNotNull(scene);
            assertEquals(640.0, scene.getWidth());
            assertEquals(360.0, scene.getHeight());
            assertTrue(stage.isShowing());
            stage.close();
        });
    }

    @Test
    @DisplayName("Should load FXML into a sub scene container")
    void testLoadSubScene() throws Exception {
        runOnFxThread(() -> {
            StackPane contentArea = new StackPane();

            viewManager.loadSubScene(contentArea, "test-view.fxml");

            assertEquals(Pos.TOP_LEFT, contentArea.getAlignment());
            assertEquals(1, contentArea.getChildren().size());
            AnchorPane viewport = assertInstanceOf(
                    AnchorPane.class,
                    contentArea.getChildren().getFirst()
            );
            assertNotNull(viewport.getClip());
            assertEquals(1, viewport.getChildren().size());
        });
    }

    private static void runOnFxThread(ThrowingRunnable action) throws Exception {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> throwable = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable error) {
                throwable.set(error);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (throwable.get() instanceof Exception exception) {
            throw exception;
        }
        if (throwable.get() instanceof Error error) {
            throw error;
        }
    }

    private static void resetStaticField(String fieldName) throws Exception {
        Field field = ViewManager.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, null);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
