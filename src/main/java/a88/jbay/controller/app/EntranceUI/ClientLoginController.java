package a88.jbay.controller.app.EntranceUI;

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

    private static final String FXML_REGISTER = "EntranceUI/client-register-view.fxml";
    private static final String STYLE_SUCCESS = "-fx-text-fill: #10B981; -fx-font-weight: bold;"; // Xanh lá (Thành công)
    private static final String STYLE_ERROR = "-fx-text-fill: #EF4444; -fx-font-weight: bold;";   // Đỏ (Lỗi)
    private static final String STYLE_INFO = "-fx-text-fill: #3B82F6; -fx-font-weight: bold;";    // Xanh dương (Đang xử lý)

    @FXML private Label loginLabel;
    @FXML private Label bottomErrorLabel;
    @FXML private Button loginButton;
    @FXML private TextField usernameTextField;
    @FXML private PasswordField passwordPasswordField;
    @FXML private Button btnToRegister;

    // ACTION HANDLERS (Xử lý sự kiện)
    @FXML
    public void goToRegisterScene(ActionEvent event) {
        try {
            ViewManager.displayScene(FXML_REGISTER);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onClickLoginButton(ActionEvent event) {
        if (!validateInputs()) return;

        showStatusMessage("Logging in...", false, true);

        String username = usernameTextField.getText().trim();
        String password = passwordPasswordField.getText().trim();

        Request request = new Request(RequestType.LOGIN)
                .put("username", username)
                .put("password", StringHash.hash(password));

        // Chạy ngầm đa luồng để không đơ giao diện
        CompletableFuture.runAsync(() -> {
            try {
                ServerConnection.getInstance().send(request);
            } catch (IOException e) {
                Platform.runLater(() -> showStatusMessage("Connection error: " + e.getMessage(), true, false));
            } catch (Exception e) {
                Platform.runLater(() -> showStatusMessage("Error: " + e.getMessage(), true, false));
            }
        });
    }

    // VALIDATION & HELPERS (Tiện ích)
    private boolean validateInputs() {
        String username = usernameTextField.getText();
        String password = passwordPasswordField.getText();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            showStatusMessage("Username or password cannot be empty!", true, false);
            return false;
        }
        return true;
    }

    // Hàm nội bộ để đổi text và đổi màu linh hoạt
    private void showStatusMessage(String text, boolean isError, boolean isInfo) {
        String style = isError ? STYLE_ERROR : (isInfo ? STYLE_INFO : STYLE_SUCCESS);

        if (loginLabel != null) {
            loginLabel.setText(text);
            loginLabel.setStyle(style);
        }
        if (bottomErrorLabel != null) {
            bottomErrorLabel.setText(text);
            bottomErrorLabel.setStyle(style);
        }
    }

    // Tự động phân tích chuỗi văn bản do mạng trả về để tô màu cho xịn
    public void updateLoginLabel(String text) {
        String lowerText = text.toLowerCase();
        boolean isError = lowerText.contains("fail") || lowerText.contains("error") || lowerText.contains("empty");

        showStatusMessage(text, isError, false);
    }
}