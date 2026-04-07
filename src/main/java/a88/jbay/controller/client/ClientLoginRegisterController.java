package a88.jbay.controller.client;

import a88.jbay.controller.DatabaseController;
import a88.jbay.controller.UserDAO;
import a88.jbay.model.StringHash;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ClientLoginRegisterController {
    // the DAO that handles sql logic
    private UserDAO userDAO = new UserDAO();

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

    // what happens when i click login
    @FXML
    public void onClickLoginButton(ActionEvent event) {
        String username = usernameTextField.getText();
        String password = StringHash.hash(passwordPasswordField.getText());

        if (!username.isBlank() && !password.isBlank()) {
            if (userDAO.checkLogin(username, password)) {
                // update the sessionid
                loginLabel.setText("Login Successful");
            }
            else loginLabel.setText("Login Failed");
        } else loginLabel.setText("Username or password is empty");
    }

    @FXML
    public void onClickRegisterButton(ActionEvent event) {
        String username = usernameTextFieldRegister.getText();
        String password = StringHash.hash(passwordPasswordFieldRegister.getText());

        if (!username.isBlank() && !password.isBlank()) {
            if (userDAO.registerUser(username, password))
                registerLabel.setText("Register Successful");
            else {
                registerLabel.setText("Register Failed");
            }
        } else registerLabel.setText("Username or password is empty");
    }
}
