package a88.jbay.controller.client;

import a88.jbay.client.ServerConnection;
import a88.jbay.dao.UserDAO;
import a88.jbay.model.StringHash;
import a88.jbay.model.entity.user.User;
import a88.jbay.model.network.Request;
import a88.jbay.model.network.RequestType;
import a88.jbay.model.network.Response;
import a88.jbay.system.UserSystem;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

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
            //loginLabel.setText(userSystem.checkLogin(username, password));
            Request request = new Request(RequestType.LOGIN)
                    .put("username", username)
                    .put("password", StringHash.hash(password));
            try {
                ServerConnection.getInstance().send(request);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else loginLabel.setText("Username or password is empty");
    }

    @FXML
    public void onClickRegisterButton(ActionEvent event) {
        String username = usernameTextFieldRegister.getText();
        String password = passwordPasswordFieldRegister.getText();

        if (!username.isBlank() && !password.isBlank()) {
            //registerLabel.setText(userSystem.registerUser(username, password));
            Request request = new Request(RequestType.REGISTER)
                    .put("username", username)
                    .put("password", StringHash.hash(password));
            try {
                ServerConnection.getInstance().send(request);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else registerLabel.setText("Username or password is empty");
    }
}
