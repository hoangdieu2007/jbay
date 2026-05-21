package a88.jbay.controller.client.EntranceController;

import a88.jbay.client.ServerConnection;
import a88.jbay.view.ViewManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;

public class ClientServerConnectController {
    @FXML
    Label connectLabel;

    @FXML
    Button connectButton;

    @FXML
    TextField hostTextField;

    @FXML
    TextField portTextField;

    @FXML
    void onClickConnectButton() {
        String host = hostTextField.getText();
        String port = portTextField.getText();
        try {
            ServerConnection.getInstance().connect(host, Integer.parseInt(port));
            ServerConnection.getInstance().startListener();
            connectLabel.setText("Connected to server");
            ViewManager.displayScene("client/client-login-view.fxml");
        } catch (IOException e) {
            connectLabel.setText("Failed to connect to server");
        }
    }
}
