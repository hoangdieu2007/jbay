package a88.jbay;

import a88.jbay.model.system.AuctionSystem;
import a88.jbay.model.system.UserSystem;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Stage loadingStage = new Stage();
        Scene loadingScene = new FXMLLoader(getClass().getResource("loading-view.fxml")).load();
        loadingStage.setTitle("Loading...");
        loadingStage.setScene(loadingScene);
        loadingStage.show();

        Task<Void> loadingTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                //loading

                UserSystem userSystem = UserSystem.getInstance();
                AuctionSystem auctionSystem = AuctionSystem.getInstance();

                Thread.sleep(3000);

                return null;
            }
        };

        loadingTask.setOnSucceeded(e -> {
            loadingStage.close();

            try {
                FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
                Scene scene = new Scene(fxmlLoader.load(), 320, 240);
                stage.setTitle("Hello!");
                stage.setScene(scene);
                stage.show();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        });

        loadingTask.setOnFailed(e -> {
            loadingStage.close();
            loadingTask.getException().printStackTrace();
        });

        Thread loadingThread = new Thread(loadingTask);
        loadingThread.start();
    }
}
