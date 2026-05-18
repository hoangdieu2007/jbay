package a88.jbay.controller.client;


import a88.jbay.di.ApplicationContext;
import a88.jbay.server.ClientService;
import a88.jbay.server.DatabaseController;
import a88.jbay.system.user.UserSystem;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.BindException;
import java.sql.SQLException;

public class ServerDatabaseController {
    @FXML private TextField serverURLTextField;
    @FXML private TextField databaseUsernameTextField;
    @FXML private TextField databasePassword;
    @FXML private TextField portNumberTextField;
    @FXML private TextField adminUsernameTextField;
    @FXML private TextField adminPasswordTextField;
    @FXML private Label lblConnectionState;
    @FXML private Label lblStartServiceState;

    @FXML private Button btnStartService;
    @FXML private Button btnRegisterAdmin;

    private UserSystem userSystem;



    @FXML
    public void initialize(){
        btnRegisterAdmin.setDisable(true);
        btnStartService.setDisable(true);
    }


    @FXML
    private void handleConnectToDatabase(){
        String url = serverURLTextField.getText();
        String username = databaseUsernameTextField.getText();
        String password = databasePassword.getText();

        try {
            DatabaseController.getInstance().initializePool(url, username, password);
            DatabaseController.getInstance().getConnection();

            btnStartService.setDisable(false);
            btnRegisterAdmin.setDisable(false);

            lblConnectionState.setText("Connect to Database successfully");

            ApplicationContext.initialize();

            userSystem = ApplicationContext.getInstance().getDependency(UserSystem.class);

        }
        catch (IllegalStateException e){
            lblConnectionState.setText(e.getMessage());
        }
        catch (SQLException e) {
            lblConnectionState.setText("Cannot connect to Database");
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void handleRegisterAdmin(){
        String adminUsername = adminUsernameTextField.getText();
        String adminPassword = adminPasswordTextField.getText();

        userSystem.register(adminUsername, adminPassword, "ADMIN");

    }

    ClientService clientService = ClientService.getInstance();

    @FXML
    private void handleStartService(){
        String port = portNumberTextField.getText();
        int portNum = Integer.parseInt(port);

        // clientService is a blocking operation
        Thread serverThread = new Thread(() -> {
                try {
                    clientService.setupServerSocket(portNum);
                    clientService.startService();

                    Platform.runLater(() -> lblStartServiceState.setText("Starting service successfully!"));

                } catch (BindException e) {
                    Platform.runLater(() -> lblStartServiceState.setText("Port is already used!")); // update UI from background thread

                } catch (IOException e) {
                    e.printStackTrace();
                }
        });
        serverThread.start();

    }

}

