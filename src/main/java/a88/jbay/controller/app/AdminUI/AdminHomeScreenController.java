package a88.jbay.controller.app.AdminUI;

import a88.jbay.client.ClientSession;
import a88.jbay.client.ServerConnection;
import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import a88.jbay.common.user.User;
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
    @FXML private Label adminNameLabel;

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colUserId;
    @FXML private TableColumn<User, String> colUserName, colUserStatus;
    @FXML private TableColumn<User, Void> colUserAction;
    @FXML private TextField searchUserField;

    @FXML private TableView<Auction> auctionTable;
    @FXML private TableColumn<Auction, Integer> colAuctionId;
    @FXML private TabPane adminTabPane;

    @FXML private TableColumn<Auction, String> colAuctionTitle, colAuctionPrice, colAuctionStatus;
    @FXML private TableColumn<Auction, Void> colAuctionAction;
    @FXML private TextField searchAuctionField;

    private final ObservableList<User> userMasterList = FXCollections.observableArrayList();
    private final ObservableList<Auction> auctionMasterList = FXCollections.observableArrayList();

    public static int targetTabIndex = 0;

    @FXML
    public void initialize() {
        if (ClientSession.getInstance().getUser() != null) {
            adminNameLabel.setText("👤 " + ClientSession.getInstance().getUser().getUsername());
        }

        if (adminTabPane != null && targetTabIndex >= 0 && targetTabIndex < adminTabPane.getTabs().size()) {
            adminTabPane.getSelectionModel().select(targetTabIndex);
        }
        targetTabIndex = 0; // Sử dụng xong thì reset về mặc định (tab 0)

        setupTableColumns();
        setupActionButtons();
        setupRealtimeListeners();
        setupSearchFilters();

        // Chỉ gửi request xin dữ liệu toàn bộ NẾU kho chứa cục bộ thực sự rỗng
        // (Tránh request lại mỗi khi bấm Cancel/View xong quay lại màn Home)
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
        // Thêm trước toàn bộ dữ liệu ĐANG CÓ SẴN từ kho RAM vào danh sách hiển thị
        targetList.setAll(sourceMap.values());
        targetList.sort((a, b) -> Integer.compare(b.getId(), a.getId()));

        // Cài đặt Listener để theo dõi các sự kiện thay đổi
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
        // Thêm trước toàn bộ dữ liệu ĐANG CÓ SẴN từ kho RAM vào danh sách hiển thị
        targetList.setAll(sourceMap.values());
        targetList.sort((a, b) -> Integer.compare(b.getId(), a.getId()));

        // Cài đặt Listener để theo dõi các sự kiện thay đổi
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
        colUserStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRole()));

        colAuctionId.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getId()));

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

        // 🌟 HIỂN THỊ CURRENT PRICE CHUẨN XÁC
        colAuctionPrice.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(String.format("%.2f USD", cellData.getValue().getCurrentPrice()));
        });

        colAuctionStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAuctionState().name()));
    }

    private void setupActionButtons() {
        colUserAction.setCellFactory(p -> new TableCell<>() {
            private final Button btn = new Button();
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                User u = getTableView().getItems().get(getIndex());
                if (u == null) {
                    setGraphic(null);
                    return;
                }

                boolean isBanned = "BAN".equals(u.getRole());
                btn.setText(isBanned ? "Unban" : "Ban");
                String baseStyle = "-fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 6 16; -fx-cursor: hand;";
                btn.setStyle((isBanned ? "-fx-background-color: #3B82F6; " : "-fx-background-color: #EF4444; ") + baseStyle);

                btn.setOnAction(e -> {
                    User currentUser = getTableView().getItems().get(getIndex());
                    if (currentUser != null) sendBanRequest(currentUser);
                });

                setGraphic(btn);
            }
        });

        colAuctionAction.setCellFactory(p -> new TableCell<>() {
            private final Button btnView = new Button("View");
            private final Button btnCancel = new Button("Cancel");
            private final HBox actionContainer = new HBox(10, btnView, btnCancel);

            { actionContainer.setStyle("-fx-alignment: center;"); }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                Auction a = getTableView().getItems().get(getIndex());
                if (a == null) {
                    setGraphic(null);
                    return;
                }

                btnView.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 6 16; -fx-cursor: hand;");
                btnView.setOnAction(e -> {
                    Auction currentA = getTableView().getItems().get(getIndex());
                    if (currentA == null) return;

                    try {
                        java.net.URL url = ViewManager.class.getResource("/a88/jbay/view/app/AuctionUI/viewAuction-view.fxml");
                        if (url == null) {
                            return;
                        }

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

                        btnView.getScene().setRoot(root);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });

                String activeStyle = "-fx-background-color: #F59E0B; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 6 16; -fx-cursor: hand;";
                String disabledStyle = "-fx-background-color: #E2E8F0; -fx-text-fill: #94A3B8; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 6 16;";

                boolean canCancel = a.getAuctionState() != AuctionState.FINISHED && a.getAuctionState() != AuctionState.CANCELED;
                btnCancel.setDisable(!canCancel);
                btnCancel.setStyle(canCancel ? activeStyle : disabledStyle);

                btnCancel.setOnAction(e -> {
                    Auction currentA = getTableView().getItems().get(getIndex());
                    if (currentA != null) sendCancelRequest(currentA);
                });

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

    private void refreshData() {
        try {
            ServerConnection.getInstance().send(new Request(RequestType.GET_USERS));
            ServerConnection.getInstance().send(new Request(RequestType.GET_AUCTIONS));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void sendBanRequest(User u) {
        try {
            String action = "BAN".equals(u.getRole()) ? "UNBAN" : "BAN";
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