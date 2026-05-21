package a88.jbay.view;

import a88.jbay.controller.ControllerProvider;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class ViewManager {
    private static ViewManager instance;

    private static final double DEFAULT_WIDTH = 1280;
    private static final double DEFAULT_HEIGHT = 720;

    private ViewManager() {
    }

    public static ViewManager getInstance() {
        if (instance == null) {
            instance = new ViewManager();
        }
        return instance;
    }

    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static void newStage(String title) {
        closePrimaryStage();

        Stage stage = new Stage();
        stage.setResizable(true);
        stage.getIcons().add(new Image(MainClient.class.getResourceAsStream("/a88/jbay/image/logo-no-bg.png")));
        stage.setTitle(title);
        stage.setOnCloseRequest(event -> Platform.exit());

        setPrimaryStage(stage);
    }

    public static void closePrimaryStage() {
        if (primaryStage != null) {
            primaryStage.close();
        }
    }

    public static void setResolution(double width, double height) {
        primaryStage.setWidth(width);
        primaryStage.setHeight(height);
    }

    public static void displayScene(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                ViewManager.class.getResource("/a88/jbay/view/app/" + fxmlPath)
        );

        Region content = loader.load();

        // Register controller
        Object controller = loader.getController();
        if (controller != null) {
            ControllerProvider.getInstance().registerController(controller);
        }

        // get stage size
        double stageWidth = primaryStage.getWidth();
        double stageHeight = primaryStage.getHeight();

        // get fxml size
        double designWidth = content.getPrefWidth();
        double designHeight = content.getPrefHeight();

        // wrap in group to prevent layout managers from overriding
        Group group = new Group(content);
        StackPane viewport = new StackPane(group);

        // create the scene using existing stage dimensions
        // this prevents the window from snapping to a new size
        Scene scene = new Scene(viewport, stageWidth, stageHeight);

        Scale scale = new Scale();
        content.getTransforms().add(scale);

        // bind scale to the Scene size divided by the FXML's design size
        scale.xProperty().bind(Bindings.min(
                scene.widthProperty().divide(designWidth),
                scene.heightProperty().divide(designHeight)
        ));
        scale.yProperty().bind(scale.xProperty());

        primaryStage.setScene(scene);
        primaryStage.show();
    }


    /**
     * redesign homeScreen
     **/
    private StackPane mainSceneArea;

    public void setMainScene(StackPane contentArea) {
        mainSceneArea = contentArea;
    }

    public void loadIntoMainScene(String fxmlPath) throws IOException {
        if (mainSceneArea == null) {
            throw new IllegalStateException("Main Scene Area hasn't been set!");
        }

        loadSubScene(mainSceneArea, fxmlPath);

    }

    public void loadSubScene(StackPane contentArea, String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(ViewManager.class.getResource("/a88/jbay/view/app/" + fxmlPath));
        Region newContent = loader.load();

        // 1. Prepare: SNAP dimensions immediately to prevent sidebar "pushing"
        newContent.setOpacity(0);
        newContent.setTranslateY(10);

        // Use fixed dimensions if possible, or bind immediately
        newContent.setMinWidth(contentArea.getWidth());
        newContent.setPrefWidth(contentArea.getWidth());

        // 2. Add as top layer
        contentArea.getChildren().add(newContent);

        // 3. Fast Transitions (150ms is the sweet spot for "fast but smooth")
        FadeTransition fadeIn = new FadeTransition(Duration.millis(150), newContent);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideUp = new TranslateTransition(Duration.millis(150), newContent);
        slideUp.setFromY(10);
        slideUp.setToY(0);

        ParallelTransition combined = new ParallelTransition(fadeIn, slideUp);

        combined.setOnFinished(e -> {
            // CLEANUP: Keep only the new content
            if (contentArea.getChildren().size() > 1) {
                Region topNode = (Region) contentArea.getChildren().get(contentArea.getChildren().size() - 1);
                contentArea.getChildren().setAll(topNode);
                // Re-bind after cleanup to ensure responsiveness
                topNode.prefWidthProperty().bind(contentArea.widthProperty());
                topNode.prefHeightProperty().bind(contentArea.heightProperty());
            }
        });

        combined.play();
    }
}