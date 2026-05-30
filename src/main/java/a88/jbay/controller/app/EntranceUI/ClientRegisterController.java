package a88.jbay.controller.app.EntranceUI;

import a88.jbay.client.ServerConnection;
import a88.jbay.util.StringHash;
import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import a88.jbay.view.ViewManager;
import a88.jbay.util.ImageProcessor;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class ClientRegisterController {

    private static final String FXML_LOGIN = "EntranceUI/client-login-view.fxml";
    private static final String STYLE_SUCCESS_TEXT = "-fx-text-fill: #10B981; -fx-font-weight: bold;"; // Xanh lá
    private static final String STYLE_ERROR_TEXT = "-fx-text-fill: #EF4444; -fx-font-weight: bold;";   // Đỏ

    @FXML private Label registerLabel;
    @FXML private Button registerButton;
    @FXML private TextField usernameTextFieldRegister;
    @FXML private PasswordField passwordPasswordFieldRegister;
    @FXML private Button btnToLogin;
    @FXML private Button btnUploadQR;
    @FXML private Label lblQRFileName;

    private File selectedImageFile;

    // ACTION HANDLERS (Xử lý sự kiện)
    @FXML
    public void goToLoginScene(ActionEvent event) {
        try {
            ViewManager.displayScene(FXML_LOGIN);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUploadQR(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select QR Code Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            this.selectedImageFile = file;
            // Hiện tên file màu xanh báo hiệu thành công
            lblQRFileName.setText(file.getName());
            lblQRFileName.setStyle(STYLE_SUCCESS_TEXT);
        }
    }

    @FXML
    public void onClickRegisterButton(ActionEvent event) {
        // Nếu Validate thất bại thì dừng luôn
        if (!validateInputs()) return;

        showStatusMessage("Registering...", false);

        String username = usernameTextFieldRegister.getText().trim();
        String password = passwordPasswordFieldRegister.getText().trim();

        // Chạy đa luồng để không làm đơ giao diện khi nén ảnh và gửi mạng
        CompletableFuture.runAsync(() -> {
            try {
                byte[] qrCodeData = ImageProcessor.compressToBytes(selectedImageFile);

                Request request = new Request(RequestType.REGISTER)
                        .put("username", username)
                        .put("password", StringHash.hash(password))
                        .put("qrCode", qrCodeData);

                ServerConnection.getInstance().send(request);
            } catch (IOException e) {
                Platform.runLater(() -> showStatusMessage("Connection error: " + e.getMessage(), true));
            } catch (Exception e) {
                Platform.runLater(() -> showStatusMessage("Error: " + e.getMessage(), true));
            }
        });
    }

    // VALIDATION & HELPERS (Kiểm tra & Tiện ích)
    private boolean validateInputs() {
        String username = usernameTextFieldRegister.getText();
        String password = passwordPasswordFieldRegister.getText();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            showStatusMessage("Username or password cannot be empty!", true);
            return false;
        }

        if (selectedImageFile == null) {
            showStatusMessage("Please upload a QR Code image for payment!", true);
            return false;
        }

        return true;
    }

    // Hàm tiện ích nội bộ để chỉnh màu chữ
    private void showStatusMessage(String text, boolean isError) {
        if (registerLabel != null) {
            registerLabel.setText(text);
            registerLabel.setStyle(isError ? STYLE_ERROR_TEXT : STYLE_SUCCESS_TEXT);
        }
    }

    public void updateRegisterLabel(String text) {
        if (registerLabel != null) {
            registerLabel.setText(text);
            if (text.toLowerCase().contains("fail") || text.toLowerCase().contains("error")) {
                registerLabel.setStyle(STYLE_ERROR_TEXT);
            } else {
                registerLabel.setStyle(STYLE_SUCCESS_TEXT);
            }
        }
    }
}