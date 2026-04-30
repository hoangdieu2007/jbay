package a88.jbay.controller.client;

import a88.jbay.client.ServerConnection;
import a88.jbay.model.ImageProcessor;
import a88.jbay.model.entity.item.Item;
import a88.jbay.model.network.Request;
import a88.jbay.model.network.RequestType;
import a88.jbay.view.ViewManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ClientSellerItemController {
    @FXML
    private TextField nameField;
    @FXML
    private TextField priceField;
    @FXML
    private ComboBox<String> startChoiceCombo;
    @FXML
    private ComboBox<String> runChoiceCombo;
    @FXML
    private ComboBox<String> typeComboBox;
    @FXML
    private TextField startStr;
    @FXML
    private TextField runStr;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private ImageView itemImageView;

    @FXML
    private Label nameErrorLabel;
    @FXML
    private Label priceErrorLabel;
    @FXML
    private Label startErrorLabel;
    @FXML
    private Label endErrorLabel;
    @FXML
    private Label typeErrorLabel;

    private File selectedImageFile;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private boolean validateInputs() {
        //Reset lại các lỗi
        nameErrorLabel.setVisible(false);
        priceErrorLabel.setVisible(false);
        startErrorLabel.setVisible(false);
        endErrorLabel.setVisible(false);
        typeErrorLabel.setVisible(false);

        // 1. Kiểm tra Name
        if (nameField.getText().trim().isEmpty()) {
            nameErrorLabel.setText("Item name cannot be empty!");
            nameErrorLabel.setVisible(true);
            return false;
        }

        // 2. Kiểm tra Price
        try {
            String pText = priceField.getText().trim();
            if (pText.isEmpty()) {
                priceErrorLabel.setText("Price is required!");
                priceErrorLabel.setVisible(true);
                return false;
            }
            double price = Double.parseDouble(pText);
            if (price <= 0) {
                priceErrorLabel.setText("Price must be greater than 0!");
                priceErrorLabel.setVisible(true);
                return false;
            }
        } catch (NumberFormatException e) {
            priceErrorLabel.setText("Price must be a valid number!");
            priceErrorLabel.setVisible(true);
            return false;
        }

        // 3. Kiểm tra Start Time
        String startChoice = startChoiceCombo.getValue();
        if (startChoice == null) {
            startErrorLabel.setText("Please select a start option!");
            startErrorLabel.setVisible(true);
            return false;
        }

        if (startChoice.equals("Custom time")) {
            String sText = startStr.getText().trim();
            if (sText.isEmpty()) {
                startErrorLabel.setText("Please enter custom start time!");
                startErrorLabel.setVisible(true);
                return false;
            }
            try {
                LocalDateTime st = LocalDateTime.parse(sText, formatter);
                if (st.isBefore(LocalDateTime.now().minusMinutes(1))) {
                    startErrorLabel.setText("Start time cannot be in the past!");
                    startErrorLabel.setVisible(true);
                    return false;
                }
            } catch (DateTimeParseException e) {
                startErrorLabel.setText("Wrong format! Use dd/MM/yyyy HH:mm");
                startErrorLabel.setVisible(true);
                return false;
            }
        }

        // 4. Kiểm tra Run Time (Thời gian chạy đấu giá)
        String runChoice = runChoiceCombo.getValue();
        if (runChoice == null) {
            endErrorLabel.setText("Please select auction duration!");
            endErrorLabel.setVisible(true);
            return false;
        }

        if (runChoice.equals("Custom time")) {
            try {
                String rText = runStr.getText().trim();
                if (rText.isEmpty()) {
                    endErrorLabel.setText("Please enter number of days!");
                    endErrorLabel.setVisible(true);
                    return false;
                }
                int days = Integer.parseInt(rText);
                if (days <= 0) {
                    endErrorLabel.setText("Duration must be at least 1 day!");
                    endErrorLabel.setVisible(true);
                    return false;
                }
            } catch (NumberFormatException e) {
                endErrorLabel.setText("Days must be a whole number!");
                endErrorLabel.setVisible(true);
                return false;
            }
        }

        // 5. Kiểm tra Type (Thể loại)
        if (typeComboBox.getValue() == null) {
            typeErrorLabel.setText("Please select an item category!");
            typeErrorLabel.setVisible(true);
            return false;
        }

        // Nếu vượt qua tất cả các chốt chặn trên
        return true;
    }

    //Hàm tính toán startTime
    private LocalDateTime calculateStartTime() {
        String choice = startChoiceCombo.getValue();
        if ("Now".equals(choice)) {
            return LocalDateTime.now();
        }
        // Vì đã validate ở bước trước, nên parse ở đây chắc chắn an toàn
        return LocalDateTime.parse(startStr.getText().trim(), formatter);
    }

    //Hàm tính toán endTime
    private LocalDateTime calculateEndTime(LocalDateTime start) {
        int days = 0;
        String choice = runChoiceCombo.getValue();

        if ("Custom time".equals(choice)) {
            days = Integer.parseInt(runStr.getText().trim());
        } else {
            // Tách lấy số từ chuỗi "3 days" -> "3"
            days = Integer.parseInt(choice.split(" ")[0]);
        }

        return start.plusDays(days);
    }

    //Hàm khởi tạo đối tượng item
    private Item createItemFromFields(byte[] imageData) {
        String name = nameField.getText().trim();
        String type = typeComboBox.getValue();
        String desc = descriptionArea.getText().trim();
        double price = Double.parseDouble(priceField.getText().trim());

        if (desc.isEmpty()) desc = "No description provided.";
        return new Item(name, type, desc, price, imageData);
    }

    @FXML
    public void initialize() {
        // 1. Nạp các loại mặt hàng
        typeComboBox.getItems().addAll("Electronics", "Fashion", "Home", "Collectibles", "Others");

        // 2. Nạp lựa chọn thời gian bắt đầu
        startChoiceCombo.getItems().addAll("Now", "Custom time");
        startChoiceCombo.getSelectionModel().selectFirst(); // Mặc định chọn "Now"

        // 3. Nạp lựa chọn thời gian chạy
        runChoiceCombo.getItems().addAll("1 day", "3 days", "7 days", "Custom time");
        runChoiceCombo.getSelectionModel().select(1); // Mặc định chọn "3 days"
    }

    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product Image");

        // Chỉ lọc các định dạng ảnh mà JavaFX hỗ trợ
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        // Mở cửa sổ chọn file
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            // 1. Lưu file vào biến toàn cục để dùng khi Submit
            this.selectedImageFile = file;

            // 2. Hiển thị ảnh lên giao diện (Real-time preview)
            Image image = new Image(file.toURI().toString());
            itemImageView.setImage(image);

            System.out.println("File: " + file.getName());
        }
    }

    @FXML
    private void handleSubmit() {
        if (!validateInputs()) return;

        try {
            byte[] imageData = ImageProcessor.compressToBytes(selectedImageFile);

            Item newItem = createItemFromFields(imageData);

            Request request = new Request(RequestType.SELL)
                    .put("item", newItem)
                    .put("start", calculateStartTime())
                    .put("end", calculateEndTime(calculateStartTime()));

            ServerConnection.getInstance().send(request);

            ViewManager.getInstance().displayScene("client/Seller-Bidder-HomeScreens.fxml");

            new Alert(Alert.AlertType.INFORMATION, "Auction created successfully!").show();

        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).show();
        }
    }
}
