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
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

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

    private final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .appendPattern("dd/MM HH:mm")
            .parseDefaulting(ChronoField.YEAR, LocalDateTime.now().getYear()) // Tự động lấy năm 2026
            .toFormatter();

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
                startErrorLabel.setText("Wrong format! Use dd/MM HH:mm");
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
        try {
            // Luôn ưu tiên parse từ TextField vì nó đã được đồng bộ với ComboBox
            return LocalDateTime.parse(startStr.getText().trim(), formatter);
        } catch (DateTimeParseException e) {
            return LocalDateTime.now();
        }
    }

    //Hàm tính toán endTime
    private LocalDateTime calculateEndTime(LocalDateTime start) {
        try {
            int days = Integer.parseInt(runStr.getText().trim());
            // Java tự động xử lý cộng ngày và nhảy tháng/năm thông minh
            return start.plusDays(days);
        } catch (NumberFormatException e) {
            return start.plusDays(1);
        }
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
        // Nạp dữ liệu vào ComboBox
        typeComboBox.getItems().addAll("Electronics", "Fashion", "Home", "Collectibles", "Others");
        startChoiceCombo.getItems().addAll("Now", "Custom time");
        runChoiceCombo.getItems().addAll("1 day", "3 days", "7 days", "Custom time");

        // Thiết lập ButtonCell để ẨN CHỮ khi đã chọn (Compact Mode)
        setupCompactComboBox(startChoiceCombo);
        setupCompactComboBox(runChoiceCombo);

        // CẬP NHẬT TEXTFIELD KHI CHỌN COMBOBOX START
        startChoiceCombo.setOnAction(e -> {
            String selected = startChoiceCombo.getValue();
            if ("Now".equals(selected)) {
                // Tự động điền thời gian hiện tại và khóa ô nhập
                startStr.setText(LocalDateTime.now().format(formatter));
                startStr.setEditable(false);
            } else {
                startStr.clear();
                startStr.setEditable(true);
                startStr.requestFocus();
            }
        });

        // CẬP NHẬT TEXTFIELD KHI CHỌN COMBOBOX RUN TIME
        runChoiceCombo.setOnAction(e -> {
            String selected = runChoiceCombo.getValue();
            if (!"Custom time".equals(selected)) {
                // Tách lấy số (ví dụ "3 days" -> "3")
                runStr.setText(selected.split(" ")[0]);
                runStr.setEditable(false);
            } else {
                runStr.clear();
                runStr.setEditable(true);
                runStr.requestFocus();
            }
        });

        // Mặc định chọn các giá trị ban đầu
        startChoiceCombo.getSelectionModel().selectFirst();
        runChoiceCombo.getSelectionModel().select(1); // Mặc định "3 days"
    }

    /**
     * Hàm hỗ trợ biến ComboBox thành dạng thu gọn:
     * Hiện chữ trong danh sách xổ xuống, nhưng ẩn chữ ở cái nút bấm.
     */
    private void setupCompactComboBox(ComboBox<String> comboBox) {
        comboBox.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(""); // Xóa sạch chữ trên giao diện chính của ComboBox
                }
            }
        });
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

    @FXML
    private void handleBack() {
        // Chỉ định tab cần mở khi quay về là Seller
        SellerBidderHomeScreenController.targetTabIndex = 0;

        try {
            ViewManager.displayScene("client/Seller-Bidder-HomeScreens.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
