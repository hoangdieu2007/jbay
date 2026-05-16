package a88.jbay.controller.client;

import a88.jbay.client.ServerConnection;
import a88.jbay.util.StringHash;
import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class ClientLoginRegisterController {
    @FXML
    private Label loginLabel;
    @FXML
    private Button loginButton;
    @FXML
    private TextField usernameTextField;
    @FXML
    private PasswordField passwordPasswordField;
    @FXML
    private Label registerLabel;
    @FXML
    private Button registerButton;
    @FXML
    private TextField usernameTextFieldRegister;
    @FXML
    private PasswordField passwordPasswordFieldRegister;

    public void updateLoginLabel(String text) {
        loginLabel.setText(text);
    }

    public void updateRegisterLabel(String text) {
        registerLabel.setText(text);
    }

    // what happens when i click login
    @FXML
    public void onClickLoginButton(ActionEvent event) {
        String username = usernameTextField.getText();
        String password = passwordPasswordField.getText();

        if (!username.isBlank() && !password.isBlank()) {
            loginLabel.setText("Logging in...");
            Request request = new Request(RequestType.LOGIN)
                    .put("username", username)
                    .put("password", StringHash.hash(password));
            
            CompletableFuture.runAsync(() -> {
                try {
                    ServerConnection.getInstance().send(request);
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        loginLabel.setText("Connection error: " + e.getMessage());
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        loginLabel.setText("Error: " + e.getMessage());
                    });
                }
            });
        } else loginLabel.setText("Username or password is empty");
    }

    @FXML
    public void onClickRegisterButton(ActionEvent event) {
        String username = usernameTextFieldRegister.getText();
        String password = passwordPasswordFieldRegister.getText();

        if (!username.isBlank() && !password.isBlank()) {
            registerLabel.setText("Registering...");
            Request request = new Request(RequestType.REGISTER)
                    .put("username", username)
                    .put("password", StringHash.hash(password));
            
            CompletableFuture.runAsync(() -> {
                try {
                    ServerConnection.getInstance().send(request);
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        registerLabel.setText("Connection error: " + e.getMessage());
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        registerLabel.setText("Error: " + e.getMessage());
                    });
                }
            });
        } else registerLabel.setText("Username or password is empty");
    }
}
