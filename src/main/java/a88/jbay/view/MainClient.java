package a88.jbay.view;

import a88.jbay.client.ClientSession;
import a88.jbay.client.ServerConnection;
import a88.jbay.di.ClientApplicationContext;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class MainClient extends Application {
    @Override
    public void stop() {
        ServerConnection.getInstance().disconnect();
    }


    @Override
    public void start(Stage stage) throws IOException {
        ClientApplicationContext.getInstance().configure();

        ViewManager viewManager = ViewManager.getInstance();

        Stage loadingStage = new Stage();
        loadingStage.initStyle(StageStyle.UNDECORATED);
        loadingStage.setTitle("Loading...");

        viewManager.setPrimaryStage(loadingStage);
        viewManager.setResolution(289, 216);
        viewManager.displayScene("loading-view.fxml");

        Task<Void> loadingTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                //loading

                //client session setup
                ClientSession clientSession = ClientSession.getInstance();

                //just to display the logo longer
                Thread.sleep(1500);

                return null;
            }
        };

        loadingTask.setOnSucceeded(e -> {
            viewManager.closePrimaryStage();

            try {
                viewManager.newStage("Welcome to jBay");
                viewManager.setResolution(1280, 720);
                viewManager.displayScene("EntranceUI/client-server-connect-view.fxml");
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        });

        loadingTask.setOnFailed(e -> {
            loadingStage.close();
            loadingTask.getException().printStackTrace();
        });

        Thread loadingThread = new Thread(loadingTask);
        loadingThread.setDaemon(true);
        loadingThread.start();
    }
}
