package a88.jbay.view;

import a88.jbay.di.ApplicationContext;
import a88.jbay.server.ClientService;
import a88.jbay.system.AuctionSystem;
import a88.jbay.system.user.UserSystem;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class MainServer extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        ApplicationContext.getInstance().configureDatabase();

        Stage loadingStage = new Stage();
        loadingStage.initStyle(StageStyle.UNDECORATED);
        FXMLLoader fxmlLoadingScreen = new FXMLLoader(MainServer.class.getResource("app/loading-view.fxml"));
        Scene loadingScene = new Scene(fxmlLoadingScreen.load(), 289, 216);
        loadingStage.setTitle("Loading...");
        loadingStage.setScene(loadingScene);
        loadingStage.show();



        // allow loading screen to be visible
        Task<Void> loadingTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Thread.sleep(1500);

                return null;
            }
        };

        loadingTask.setOnSucceeded(e -> {
            loadingStage.close();

            try {
                FXMLLoader fxmlLoader = new FXMLLoader(MainServer.class.getResource("app/ServerUI/server-database-view.fxml"));
                Scene scene = new Scene(fxmlLoader.load());
                stage.setResizable(false);
                stage.getIcons().add(new Image(MainServer.class.getResourceAsStream("/a88/jbay/image/logo-no-bg.png")));
                stage.setTitle("Login to jBay");
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

    // automatically call when users close the UI
    @Override
    public void stop(){
        try {
            ClientService clientService = getClientService();
            if (clientService != null) {
                clientService.stopService();
            }
        } catch (Exception e) {
            // DI not initialized — nothing to stop
        }
        exitApplication(0);

    }

    ClientService getClientService() {
        try {
            return ApplicationContext.getInstance().getDependency(ClientService.class);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    void exitApplication(int status) {
        System.exit(status);
    }
}
