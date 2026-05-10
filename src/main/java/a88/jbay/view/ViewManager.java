package a88.jbay.view;

import a88.jbay.client.ClientSession;
import a88.jbay.controller.ControllerProvider;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

import java.io.IOException;

public class ViewManager {
    private static ViewManager instance;

    private static final double DEFAULT_WIDTH = 1327;
    private static final double DEFAULT_HEIGHT = 861;

    private ViewManager() {}

    public synchronized static ViewManager getInstance() {
        if (instance == null) {
            instance = new ViewManager();
        }
        return instance;
    }

    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static void closePrimaryStage() {
        if (primaryStage != null) {
            primaryStage.close();
        }
    }

    public static void displayScene(String fxmlPath) throws IOException {
        // Clear auction listeners before scene change to prevent memory leaks
        ClientSession.getInstance().clearAuctionListeners();

        FXMLLoader loader = new FXMLLoader(
                ViewManager.class.getResource("/a88/jbay/view/" + fxmlPath)
        );

        Parent content = loader.load();

        // Register controller
        Object controller = loader.getController();
        if (controller != null) {
            ControllerProvider.getInstance().registerController(controller);
        }

        // Get original FXML designed size
        double designWidth = content.prefWidth(-1);
        double designHeight = content.prefHeight(-1);

        // Fallback if pref sizes are not set
        if (designWidth <= 0) designWidth = 1280;
        if (designHeight <= 0) designHeight = 720;

        Group group = new Group(content);

        StackPane viewport = new StackPane(group);

        Scene scene = new Scene(viewport, designWidth, designHeight);

        Scale scale = new Scale();
        scale.setPivotX(0);
        scale.setPivotY(0);

        content.getTransforms().add(scale);

        scale.xProperty().bind(
                Bindings.min(
                        scene.widthProperty().divide(designWidth),
                        scene.heightProperty().divide(designHeight)
                )
        );

        scale.yProperty().bind(scale.xProperty());

        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
