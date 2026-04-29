package a88.jbay.view;

import a88.jbay.controller.ControllerProvider;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneManager {
    private static Stage primaryStage;
    
    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }
    
    public static void switchScene(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
        Parent root = loader.load();
        
        // Register controller
        Object controller = loader.getController();
        if (controller != null) {
            ControllerProvider.getInstance().registerController(controller);
        }
        
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static void switchScene(String fxmlPath, int width, int height) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
        Parent root = loader.load();
        
        // Register controller
        Object controller = loader.getController();
        if (controller != null) {
            ControllerProvider.getInstance().registerController(controller);
        }
        
        Scene scene = new Scene(root, width, height);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
