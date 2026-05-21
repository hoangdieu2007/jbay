package a88.jbay.view;

import a88.jbay.controller.ControllerProvider;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

import java.io.IOException;

public class ViewManager {
    private static ViewManager instance;

    private static final double DEFAULT_WIDTH = 1280;
    private static final double DEFAULT_HEIGHT = 720;

    private ViewManager() {}

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


    /** redesign homeScreen**/
    private StackPane mainSceneArea;

    public void setMainScene(StackPane contentArea){
        mainSceneArea = contentArea;
    }

    public void loadIntoMainScene(String fxmlPath) throws IOException {
        if(mainSceneArea == null){
            throw new IllegalStateException("Main Scene Area hasn't been set!");
        }

        loadSubScene(mainSceneArea, fxmlPath);

    }

    public void loadSubScene(StackPane contentArea, String fxmlPath) throws IOException{
        FXMLLoader loader = new FXMLLoader(ViewManager.class.getResource("/a88/jbay/view/app/" + fxmlPath)); // find a file (URL)

        Region content = loader.load(); // Region is a parent class --> can use for any types of container

        Object controller = loader.getController();
        if(controller != null){
            ControllerProvider.getInstance().registerController(controller);
        }

        //replace old scene with new one
        contentArea.getChildren().setAll(content);

        // if parent node grows, its children grow too (auto resizing)
        content.prefHeightProperty().bind(contentArea.heightProperty());
        content.prefWidthProperty().bind(contentArea.widthProperty());


    }
}
