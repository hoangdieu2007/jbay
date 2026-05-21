package a88.jbay.controller.app.ServerUI;


import a88.jbay.di.ApplicationContext;
import a88.jbay.server.ClientService;
import a88.jbay.server.DatabaseController;
import a88.jbay.system.user.UserSystem;
import a88.jbay.util.StringHash;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.BindException;
import java.sql.SQLException;

public class ServerDatabaseController {
    @FXML private TextField serverURLTextField;
    @FXML private TextField databaseUsernameTextField;
    @FXML private PasswordField databasePassword;
    @FXML private TextField portNumberTextField;
    @FXML private TextField adminUsernameTextField;
    @FXML private PasswordField adminPasswordField;
    @FXML private Label lblConnectionState;
    @FXML private Label lblStartServiceState;

    @FXML private Button btnStartService;
    @FXML private Button btnRegisterAdmin;

    private UserSystem userSystem;



    @FXML
    public void initialize(){
        btnRegisterAdmin.setDisable(true);
        btnStartService.setDisable(true);
        ApplicationContext.getInstance().configureDatabase();
    }


    @FXML
    private void handleConnectToDatabase(){
        String url = serverURLTextField.getText();
        String username = databaseUsernameTextField.getText();
        String password = databasePassword.getText();

        try {
            DatabaseController dbController = ApplicationContext.getInstance().getDependency(DatabaseController.class);
            dbController.initializePool(url, username, password);
            dbController.getConnection();

            btnStartService.setDisable(false);
            btnRegisterAdmin.setDisable(false);

            ApplicationContext.getInstance().configureServices(); // phase 2

            lblConnectionState.setText("Connect to Database successfully");
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


        userSystem = ApplicationContext.getInstance().getDependency(UserSystem.class);

        String adminUsername = adminUsernameTextField.getText();
        String adminPassword = adminPasswordField.getText();

        userSystem.register(adminUsername, StringHash.hash(adminPassword), "ADMIN");

    }


    @FXML
    private void handleStartService(){
        ApplicationContext.getInstance().configureServices();

        String port = portNumberTextField.getText();
        int portNum = Integer.parseInt(port);

        // clientService is a blocking operation
        Thread serverThread = new Thread(() -> {
                try {
                    ClientService clientService = ApplicationContext.getInstance().getDependency(ClientService.class);
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

