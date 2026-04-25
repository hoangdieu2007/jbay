package a88.jbay.controller.client;

import a88.jbay.dao.UserDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class ClientLoginRegisterController {
    // the DAO that handles sql logic
    private UserDAO userDAO = UserDAO.getInstance();

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
        String password = passwordPasswordField.getText();

        if (!username.isBlank() && !password.isBlank()) {
            loginLabel.setText(userDAO.checkLogin(username, password));
        } else loginLabel.setText("Username or password is empty");
    }

    @FXML
    public void onClickRegisterButton(ActionEvent event) {
        String username = usernameTextFieldRegister.getText();
        String password = passwordPasswordFieldRegister.getText();

        if (!username.isBlank() && !password.isBlank()) {
            registerLabel.setText(userDAO.registerUser(username, password));
        } else registerLabel.setText("Username or password is empty");
    }
}
