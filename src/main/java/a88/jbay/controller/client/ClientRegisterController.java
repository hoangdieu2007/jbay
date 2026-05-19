package a88.jbay.controller.client;

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

public class ClientRegisterController {

    @FXML private Label registerLabel;
    @FXML private Button registerButton;
    @FXML private TextField usernameTextFieldRegister;
    @FXML private PasswordField passwordPasswordFieldRegister;
    @FXML private Button btnToLogin;

    @FXML
    public void goToLoginScene(ActionEvent event) {
        try {
            // Gọi ViewManager để quay lại file FXML đăng nhập
            ViewManager.displayScene("client/client-login-view.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateRegisterLabel(String text) {
        if (registerLabel != null) registerLabel.setText(text);
    }

    @FXML
    public void onClickRegisterButton(ActionEvent event) {
        String username = usernameTextFieldRegister.getText();
        String password = passwordPasswordFieldRegister.getText();

        if (!username.isBlank() && !password.isBlank()) {
            updateRegisterLabel("Registering...");
            Request request = new Request(RequestType.REGISTER)
                    .put("username", username)
                    .put("password", StringHash.hash(password));

            CompletableFuture.runAsync(() -> {
                try {
                    ServerConnection.getInstance().send(request);
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        updateRegisterLabel("Connection error: " + e.getMessage());
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        updateRegisterLabel("Error: " + e.getMessage());
                    });
                }
            });
        } else {
            updateRegisterLabel("Username or password is empty");
        }
    }
}