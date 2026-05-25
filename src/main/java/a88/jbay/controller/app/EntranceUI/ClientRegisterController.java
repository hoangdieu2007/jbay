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

    @FXML private Label registerLabel;
    @FXML private Button registerButton;
    @FXML private TextField usernameTextFieldRegister;
    @FXML private PasswordField passwordPasswordFieldRegister;
    @FXML private Button btnToLogin;

    @FXML private Button btnUploadQR;
    @FXML private Label lblQRFileName;

    private File selectedImageFile;

    @FXML
    public void goToLoginScene(ActionEvent event) {
        try {
            // Gọi ViewManager để quay lại file FXML đăng nhập
            ViewManager.displayScene("EntranceUI/client-login-view.fxml");
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

            // Hiển thị tên file lên nhãn bên cạnh
            lblQRFileName.setText(file.getName());
            lblQRFileName.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
        }
    }

    public void updateRegisterLabel(String text) {
        if (registerLabel != null) registerLabel.setText(text);
    }

    @FXML
    public void onClickRegisterButton(ActionEvent event) {
        String username = usernameTextFieldRegister.getText();
        String password = passwordPasswordFieldRegister.getText();

        // Kiểm tra tài khoản hoặc mật khẩu trống
        if (username.isBlank() || password.isBlank()) {
            updateRegisterLabel("Username or password is empty");
            return;
        }

        // CHỐT CHẶN: Nếu chưa chọn file ảnh QR thì chặn lại, không cho submit
        if (selectedImageFile == null) {
            updateRegisterLabel("Please upload a QR Code image for payment!");
            return;
        }

        updateRegisterLabel("Registering...");

        // Khi đã vượt qua các bước kiểm tra thì mới tiến hành nén ảnh và gửi đi
        byte[] qrCodeData = ImageProcessor.compressToBytes(selectedImageFile);

        Request request = new Request(RequestType.REGISTER)
                .put("username", username)
                .put("password", StringHash.hash(password))
                .put("qrCode", qrCodeData);

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
    }
}