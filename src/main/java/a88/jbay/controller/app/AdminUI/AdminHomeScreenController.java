package a88.jbay.controller.app.AdminUI;

import a88.jbay.client.ClientSession;
import a88.jbay.client.ServerConnection;
import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import a88.jbay.common.user.User;
import a88.jbay.common.user.role.Role;
import a88.jbay.controller.app.AuctionUI.ViewAuctionController;
import a88.jbay.view.ViewManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import java.io.IOException;

public class AdminHomeScreenController {
    private static final String BTN_BASE_STYLE = "-fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 6 16; -fx-cursor: hand;";
    private static final String BTN_UNBAN_STYLE = "-fx-background-color: #3B82F6; " + BTN_BASE_STYLE; // Xanh dương
    private static final String BTN_BAN_STYLE = "-fx-background-color: #EF4444; " + BTN_BASE_STYLE;   // Đỏ
    private static final String BTN_VIEW_STYLE = "-fx-background-color: #10B981; " + BTN_BASE_STYLE;  // Xanh lá
    private static final String BTN_CANCEL_ACTIVE = "-fx-background-color: #F59E0B; " + BTN_BASE_STYLE; // Vàng
    private static final String BTN_CANCEL_DISABLED = "-fx-background-color: #E2E8F0; -fx-text-fill: #94A3B8; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 6 16;";

    @FXML private Label adminNameLabel;
    @FXML private TabPane adminTabPane;
    public static int targetTabIndex = 0;

    // Bảng User
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colUserId;
    @FXML private TableColumn<User, String> colUserName, colUserStatus;
    @FXML private TableColumn<User, Void> colUserAction;
    @FXML private TextField searchUserField;

    // Bảng Auction
    @FXML private TableView<Auction> auctionTable;
    @FXML private TableColumn<Auction, Integer> colAuctionId;
    @FXML private TableColumn<Auction, String> colAuctionTitle, colAuctionPrice, colAuctionStatus;
    @FXML private TableColumn<Auction, Void> colAuctionAction;
    @FXML private TextField searchAuctionField;

    // Dữ liệu nội bộ
    private final ObservableList<User> userMasterList = FXCollections.observableArrayList();
    private final ObservableList<Auction> auctionMasterList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (ClientSession.getInstance().getUser() != null) {
            adminNameLabel.setText("👤 " + ClientSession.getInstance().getUser().getUsername());
        }

        // Điều hướng TabPane
        if (adminTabPane != null && targetTabIndex >= 0 && targetTabIndex < adminTabPane.getTabs().size()) {
            adminTabPane.getSelectionModel().select(targetTabIndex);
        }
        targetTabIndex = 0;

        setupTableColumns();
        setupActionButtons();
        setupRealtimeListeners();
        setupSearchFilters();

        // Chỉ fetch từ server nếu RAM rỗng
        if (ClientSession.getInstance().getAdminUsers().isEmpty() ||
                ClientSession.getInstance().getAdminAuctions().isEmpty()) {
            refreshData();
        }
    }

    private void setupRealtimeListeners() {
        syncUserMapToList(ClientSession.getInstance().getAdminUsers(), userMasterList);
        syncAuctionMapToList(ClientSession.getInstance().getAdminAuctions(), auctionMasterList);
    }

    private void syncUserMapToList(ObservableMap<Integer, User> sourceMap, ObservableList<User> targetList) {
        targetList.setAll(sourceMap.values());
        targetList.sort((a, b) -> Integer.compare(b.getId(), a.getId()));

        sourceMap.addListener((MapChangeListener<Integer, User>) change -> {
            Platform.runLater(() -> {
                targetList.removeIf(item -> item.getId() == change.getKey());
                if (change.wasAdded()) {
                    targetList.add(change.getValueAdded());
                }
                targetList.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
                userTable.refresh();
            });
        });
    }

    private void syncAuctionMapToList(ObservableMap<Integer, Auction> sourceMap, ObservableList<Auction> targetList) {
        targetList.setAll(sourceMap.values());
        targetList.sort((a, b) -> Integer.compare(b.getId(), a.getId()));

        sourceMap.addListener((MapChangeListener<Integer, Auction>) change -> {
            Platform.runLater(() -> {
                targetList.removeIf(item -> item.getId() == change.getKey());
                if (change.wasAdded()) {
                    targetList.add(change.getValueAdded());
                }
                targetList.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
                auctionTable.refresh();
            });
        });
    }

    private void setupTableColumns() {
        colUserId.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getId()));
        colUserName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUsername()));
        colUserStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRole().name()));

        colAuctionId.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getId()));
        colAuctionPrice.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%.2f USD", cellData.getValue().getCurrentPrice())));
        colAuctionStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAuctionState().name()));

        colAuctionTitle.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItem().getName()));
        colAuctionTitle.setCellFactory(tc -> {
            TableCell<Auction, String> cell = new TableCell<>();
            Text text = new Text();
            cell.setGraphic(text);
            cell.setPrefHeight(Control.USE_COMPUTED_SIZE);
            text.wrappingWidthProperty().bind(colAuctionTitle.widthProperty().subtract(10));
            text.textProperty().bind(cell.itemProperty());
            return cell;
        });
    }

    private void setupActionButtons() {
        // Cột Hành động của User
        colUserAction.setCellFactory(p -> new TableCell<>() {
            private final Button btn = new Button();
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                User u = getTableView().getItems().get(getIndex());
                if (u == null) { setGraphic(null); return; }

                boolean isBanned = (u.getRole() == Role.BAN);
                btn.setText(isBanned ? "Unban" : "Ban");
                btn.setStyle(isBanned ? BTN_UNBAN_STYLE : BTN_BAN_STYLE);

                btn.setOnAction(e -> sendBanRequest(u));
                setGraphic(btn);
            }
        });

        // Cột Hành động của Auction
        colAuctionAction.setCellFactory(p -> new TableCell<>() {
            private final Button btnView = new Button("View");
            private final Button btnCancel = new Button("Cancel");
            private final HBox actionContainer = new HBox(10, btnView, btnCancel);

            { actionContainer.setStyle("-fx-alignment: center;"); }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null); return;
                }

                Auction a = getTableView().getItems().get(getIndex());
                if (a == null) { setGraphic(null); return; }

                // Setup nút View
                btnView.setStyle(BTN_VIEW_STYLE);
                btnView.setOnAction(e -> openViewAuction(a));

                // Setup nút Cancel
                boolean canCancel = (a.getAuctionState() == AuctionState.OPENING || a.getAuctionState() == AuctionState.RUNNING);
                btnCancel.setDisable(!canCancel);
                btnCancel.setStyle(canCancel ? BTN_CANCEL_ACTIVE : BTN_CANCEL_DISABLED);
                btnCancel.setOnAction(e -> sendCancelRequest(a));

                setGraphic(actionContainer);
            }
        });
    }

    private void setupSearchFilters() {
        FilteredList<User> filteredUsers = new FilteredList<>(userMasterList, p -> true);
        searchUserField.textProperty().addListener((o, old, newVal) -> {
            filteredUsers.setPredicate(u -> newVal == null || newVal.isEmpty() || u.getUsername().toLowerCase().contains(newVal.toLowerCase()));
        });
        userTable.setItems(filteredUsers);

        FilteredList<Auction> filteredAuctions = new FilteredList<>(auctionMasterList, p -> true);
        searchAuctionField.textProperty().addListener((o, old, newVal) -> {
            filteredAuctions.setPredicate(a -> newVal == null || newVal.isEmpty() || a.getItem().getName().toLowerCase().contains(newVal.toLowerCase()));
        });
        auctionTable.setItems(filteredAuctions);
    }

    private void openViewAuction(Auction currentA) {
        try {
            java.net.URL url = ViewManager.class.getResource("/a88/jbay/view/app/AuctionUI/viewAuction-view.fxml");
            if (url == null) return;

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(url);
            javafx.scene.Parent root = loader.load();

            ViewAuctionController controller = loader.getController();
            controller.setAuctionData(
                    currentA,
                    ClientSession.getInstance().getAdminAuctions(),
                    true,  // canCancel = true
                    false, // canConfirm = false
                    () -> {
                        try {
                            AdminHomeScreenController.targetTabIndex = 1;
                            ViewManager.displayScene("AdminUI/Admin-HomeScreens.fxml");
                        } catch (IOException ex) { ex.printStackTrace(); }
                    }
            );
            // Đặt scene hiện tại bằng FXML vừa load
            adminNameLabel.getScene().setRoot(root);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void refreshData() {
        try {
            ServerConnection.getInstance().send(new Request(RequestType.GET_USERS));
            ServerConnection.getInstance().send(new Request(RequestType.GET_AUCTIONS));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void sendBanRequest(User u) {
        try {
            String action = u.getRole() == Role.BAN ? "UNBAN" : "BAN";
            Request req = new Request(RequestType.BAN);
            req.put("userId", u.getId());
            req.put("action", action);
            ServerConnection.getInstance().send(req);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void sendCancelRequest(Auction a) {
        try {
            ServerConnection.getInstance().send(new Request(RequestType.CANCEL).put("auctionId", a.getId()));
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleLogout() {
        try {
            ServerConnection.getInstance().send(new Request(RequestType.LOGOUT));
        } catch (IOException e) { e.printStackTrace(); }
    }
}