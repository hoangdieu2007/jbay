package a88.jbay.controller.app.AuctionUI;

import a88.jbay.client.ServerConnection;
import a88.jbay.util.ImageProcessor;
import a88.jbay.common.item.Item;
import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import a88.jbay.util.JBayLogger;
import a88.jbay.view.ViewManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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
    private final JBayLogger logger = JBayLogger.getLogger(ClientSellerItemController.class);

    private static final String FXML_MY_LISTINGS = "UserHomeScreenUI/my-Listings.fxml";
    private static final String DATE_PATTERN = "dd/MM HH:mm";

    private final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .appendPattern(DATE_PATTERN)
            .parseDefaulting(ChronoField.YEAR, LocalDateTime.now().getYear())
            .toFormatter();

    @FXML private TextField nameField, priceField, minIncrementField, startStr, runStr;
    @FXML private ComboBox<String> startChoiceCombo, runChoiceCombo, typeComboBox;
    @FXML private TextArea descriptionArea;
    @FXML private ImageView itemImageView;
    @FXML private Label nameErrorLabel, priceErrorLabel, minIncrementErrorLabel, startErrorLabel, endErrorLabel, typeErrorLabel;

    private File selectedImageFile;

    // INITIALIZATION (Khởi tạo)
    @FXML
    public void initialize() {
        typeComboBox.getItems().addAll("Electronics", "Fashion", "Home", "Collectibles", "Others");
        startChoiceCombo.getItems().addAll("Now", "Custom time");
        runChoiceCombo.getItems().addAll("1 day", "3 days", "7 days", "Custom time");

        setupCompactComboBox(startChoiceCombo);
        setupCompactComboBox(runChoiceCombo);

        startChoiceCombo.setOnAction(e -> {
            if ("Now".equals(startChoiceCombo.getValue())) {
                startStr.setText(LocalDateTime.now().format(formatter));
                startStr.setEditable(false);
            } else {
                startStr.clear();
                startStr.setEditable(true);
                startStr.requestFocus();
            }
        });

        runChoiceCombo.setOnAction(e -> {
            String selected = runChoiceCombo.getValue();
            if (!"Custom time".equals(selected)) {
                runStr.setText(selected.split(" ")[0]);
                runStr.setEditable(false);
            } else {
                runStr.clear();
                runStr.setEditable(true);
                runStr.requestFocus();
            }
        });

        startChoiceCombo.getSelectionModel().selectFirst();
        runChoiceCombo.getSelectionModel().select(1);
    }

    // ACTION HANDLERS (Xử lý sự kiện)
    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            this.selectedImageFile = file;
            itemImageView.setImage(new Image(file.toURI().toString()));
            logger.debug("File selected: " + file.getName());
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
                    .put("minIncrement", getMinIncrementFromField())
                    .put("start", calculateStartTime())
                    .put("end", calculateEndTime(calculateStartTime()));

            ServerConnection.getInstance().send(request);

            ViewManager.getInstance().loadIntoMainScene(FXML_MY_LISTINGS);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Auction created successfully!");

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Connection error: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            ViewManager.getInstance().loadIntoMainScene(FXML_MY_LISTINGS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // VALIDATION LOGIC (Kiểm tra đầu vào)
    private boolean validateInputs() {
        hideAllErrors();

        if (nameField.getText().trim().isEmpty()) {
            return showError(nameErrorLabel, "Item name cannot be empty!");
        }

        try {
            String pText = priceField.getText().trim();
            if (pText.isEmpty()) return showError(priceErrorLabel, "Price is required!");
            if (Double.parseDouble(pText) <= 0) return showError(priceErrorLabel, "Price must be greater than 0!");
        } catch (NumberFormatException e) {
            return showError(priceErrorLabel, "Price must be a valid number!");
        }

        try {
            String minIncText = minIncrementField.getText().trim();
            if (!minIncText.isEmpty() && Double.parseDouble(minIncText) < 0) {
                return showError(minIncrementErrorLabel, "Minimum increment cannot be negative!");
            }
        } catch (NumberFormatException e) {
            return showError(minIncrementErrorLabel, "Must be a valid number!");
        }

        if (startChoiceCombo.getValue() == null) return showError(startErrorLabel, "Please select a start option!");
        if ("Custom time".equals(startChoiceCombo.getValue())) {
            String sText = startStr.getText().trim();
            if (sText.isEmpty()) return showError(startErrorLabel, "Please enter custom start time!");
            try {
                if (LocalDateTime.parse(sText, formatter).isBefore(LocalDateTime.now().minusMinutes(1))) {
                    return showError(startErrorLabel, "Start time cannot be in the past!");
                }
            } catch (DateTimeParseException e) {
                return showError(startErrorLabel, "Wrong format! Use " + DATE_PATTERN);
            }
        }

        if (runChoiceCombo.getValue() == null) return showError(endErrorLabel, "Please select auction duration!");
        if ("Custom time".equals(runChoiceCombo.getValue())) {
            try {
                String rText = runStr.getText().trim();
                if (rText.isEmpty()) return showError(endErrorLabel, "Please enter number of days!");
                if (Integer.parseInt(rText) <= 0) return showError(endErrorLabel, "Duration must be at least 1 day!");
            } catch (NumberFormatException e) {
                return showError(endErrorLabel, "Days must be a whole number!");
            }
        }

        if (typeComboBox.getValue() == null) {
            return showError(typeErrorLabel, "Please select an item category!");
        }

        return true;
    }

    // HELPER METHODS (Hàm tiện ích)
    private boolean showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        return false;
    }

    private void hideAllErrors() {
        nameErrorLabel.setVisible(false);
        priceErrorLabel.setVisible(false);
        minIncrementErrorLabel.setVisible(false);
        startErrorLabel.setVisible(false);
        endErrorLabel.setVisible(false);
        typeErrorLabel.setVisible(false);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type, msg);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.show();
        });
    }

    private LocalDateTime calculateStartTime() {
        try {
            return LocalDateTime.parse(startStr.getText().trim(), formatter);
        } catch (DateTimeParseException e) {
            return LocalDateTime.now();
        }
    }

    private LocalDateTime calculateEndTime(LocalDateTime start) {
        try {
            return start.plusDays(Integer.parseInt(runStr.getText().trim()));
        } catch (NumberFormatException e) {
            return start.plusDays(1);
        }
    }

    private Item createItemFromFields(byte[] imageData) {
        String desc = descriptionArea.getText().trim();
        return new Item(
                nameField.getText().trim(),
                typeComboBox.getValue(),
                desc.isEmpty() ? "No description provided." : desc,
                Double.parseDouble(priceField.getText().trim()),
                imageData
        );
    }

    private double getMinIncrementFromField() {
        String text = minIncrementField.getText().trim();
        return text.isEmpty() ? 0.0 : Double.parseDouble(text);
    }

    private void setupCompactComboBox(ComboBox<String> comboBox) {
        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : "");
            }
        });
    }
}