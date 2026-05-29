package a88.jbay.view;

import a88.jbay.controller.ControllerProvider;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.AnchorPane;
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


    // ---- instance wrappers (for DI / testability) ----

    public void openStage(String title) {
        ViewManager.newStage(title);
    }

    public void resizeStage(double width, double height) {
        ViewManager.setResolution(width, height);
    }

    public void showScene(String fxmlPath) throws IOException {
        ViewManager.displayScene(fxmlPath);
    }

    public void closeStage() {
        ViewManager.closePrimaryStage();
    }

    public void assignStage(Stage stage) {
        ViewManager.setPrimaryStage(stage);
    }

    /**
     * redesign homeScreen
     **/
    private StackPane mainSceneArea;
    private FadeTransition currentSubSceneTransition;
    private long subSceneLoadVersion;

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
        long loadVersion = ++subSceneLoadVersion;
        if (currentSubSceneTransition != null) {
            currentSubSceneTransition.stop();
        }

        FXMLLoader loader = new FXMLLoader(ViewManager.class.getResource("/a88/jbay/view/app/" + fxmlPath));
        Region newContent = loader.load();

        Object controller = loader.getController();
        if (controller != null) {
            ControllerProvider.getInstance().registerController(controller);
        }

        contentArea.setAlignment(Pos.TOP_LEFT);

        AnchorPane viewport = new AnchorPane(newContent);
        viewport.setMinSize(0, 0);
        viewport.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        viewport.prefWidthProperty().bind(contentArea.widthProperty());
        viewport.prefHeightProperty().bind(contentArea.heightProperty());
        StackPane.setAlignment(viewport, Pos.TOP_LEFT);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(viewport.widthProperty());
        clip.heightProperty().bind(viewport.heightProperty());
        viewport.setClip(clip);

        newContent.setMinSize(0, 0);
        newContent.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        newContent.setTranslateX(0);
        newContent.setTranslateY(0);
        AnchorPane.setTopAnchor(newContent, 0.0);
        AnchorPane.setRightAnchor(newContent, 0.0);
        AnchorPane.setBottomAnchor(newContent, 0.0);
        AnchorPane.setLeftAnchor(newContent, 0.0);

        viewport.setOpacity(0);
        contentArea.getChildren().add(viewport);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), viewport);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setOnFinished(e -> {
            if (loadVersion == subSceneLoadVersion) {
                contentArea.getChildren().setAll(viewport);
                currentSubSceneTransition = null;
            }
        });
        currentSubSceneTransition = fadeIn;
        fadeIn.play();
    }

   }
