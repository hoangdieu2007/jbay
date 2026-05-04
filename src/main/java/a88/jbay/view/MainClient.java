package a88.jbay.view;

import a88.jbay.client.ClientSession;
import a88.jbay.client.ServerConnection;
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

        ViewManager viewManager = ViewManager.getInstance();

        Stage loadingStage = new Stage();
        loadingStage.initStyle(StageStyle.UNDECORATED);
        loadingStage.setTitle("Loading...");

        viewManager.setPrimaryStage(loadingStage);
        viewManager.displayScene("loading-view.fxml", 289, 216);

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
                stage.setResizable(false);
                stage.getIcons().add(new Image(MainClient.class.getResourceAsStream("/a88/jbay/image/logo-no-bg.png")));
                stage.setTitle("Auction88's jBay");
                stage.setOnCloseRequest(event -> Platform.exit());


                viewManager.setPrimaryStage(stage);
                viewManager.displayScene("client/client-server-connect-view.fxml", 600, 400);
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
