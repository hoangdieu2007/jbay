package a88.jbay.controller.client.EntranceUI;

import a88.jbay.client.ServerConnection;
import a88.jbay.util.StringHash;
import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import a88.jbay.view.ViewManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class ClientLoginController {

    @FXML private Label loginLabel;
    @FXML private Label bottomErrorLabel;
    @FXML private Button loginButton;
    @FXML private TextField usernameTextField;
    @FXML private PasswordField passwordPasswordField;
    @FXML private Button btnToRegister;

    @FXML
    public void goToRegisterScene(ActionEvent event) {
        try {
            // Gọi ViewManager để chuyển sang file FXML đăng ký
            ViewManager.displayScene("client/client-register-view.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateLoginLabel(String text) {
        if (loginLabel != null) loginLabel.setText(text);
        if (bottomErrorLabel != null) bottomErrorLabel.setText(text);
    }

    @FXML
    public void onClickLoginButton(ActionEvent event) {
        String username = usernameTextField.getText();
        String password = passwordPasswordField.getText();

        if (!username.isBlank() && !password.isBlank()) {
            updateLoginLabel("Logging in...");
            Request request = new Request(RequestType.LOGIN)
                    .put("username", username)
                    .put("password", StringHash.hash(password));

            CompletableFuture.runAsync(() -> {
                try {
                    ServerConnection.getInstance().send(request);
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        updateLoginLabel("Connection error: " + e.getMessage());
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        updateLoginLabel("Error: " + e.getMessage());
                    });
                }
            });
        } else {
            updateLoginLabel("Username or password is empty");
        }
    }
}