package a88.jbay.controller.app.AdminUI;

import a88.jbay.client.ClientSession;
import a88.jbay.client.ServerConnection;
import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import a88.jbay.common.user.User;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.IOException;

public class AdminHomeScreenController {
    // Khai báo các thành phần FXML
    @FXML private Label adminNameLabel;

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colUserId;
    @FXML private TableColumn<User, String> colUserName, colUserStatus;
    @FXML private TableColumn<User, Void> colUserAction;
    @FXML private TextField searchUserField;

    @FXML private TableView<Auction> auctionTable;
    @FXML private TableColumn<Auction, Integer> colAuctionId;
    @FXML private TableColumn<Auction, String> colAuctionTitle, colAuctionStatus;
    @FXML private TableColumn<Auction, Void> colAuctionAction;
    @FXML private TextField searchAuctionField;

    // Danh sách hiển thị trên UI (đã được bọc bởi FilteredList để tìm kiếm)
    private final ObservableList<User> userMasterList = FXCollections.observableArrayList();
    private final ObservableList<Auction> auctionMasterList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // HIỂN THỊ TÊN ADMIN: Lấy thông tin từ session đăng nhập gán lên thanh Top Header
        if (ClientSession.getInstance().getUser() != null) {
            adminNameLabel.setText("👤 " + ClientSession.getInstance().getUser().getUsername());
        }

        // Setup hiển thị dữ liệu (Sử dụng Lambda thay vì PropertyValueFactory)
        setupTableColumns();

        // Setup nút bấm động (Ban/Cancel)
        setupActionButtons();

        // Setup Real-time Listener - Tự động đồng bộ từ Map sang List
        setupRealtimeListeners();

        // Setup bộ lọc tìm kiếm
        setupSearchFilters();

        // Lấy dữ liệu ban đầu từ Server
        refreshData();
    }

    // --- CƠ CHẾ REAL-TIME: TỰ ĐỘNG ĐỒNG BỘ ---

    private void setupRealtimeListeners() {
        syncMapToList(ClientSession.getInstance().getAdminUsers(), userMasterList);
        syncMapToList(ClientSession.getInstance().getAdminAuctions(), auctionMasterList);
    }

    private <T> void syncMapToList(ObservableMap<Integer, T> sourceMap, ObservableList<T> targetList) {
        sourceMap.addListener((MapChangeListener<Integer, T>) change -> {
            Platform.runLater(() -> {
                targetList.removeIf(item -> {
                    if (item instanceof User) return ((User) item).getId() == change.getKey();
                    if (item instanceof Auction) return ((Auction) item).getId() == change.getKey();
                    return false;
                });
                if (change.wasAdded()) {
                    targetList.add(change.getValueAdded());
                }
                targetList.sort((a, b) -> Integer.compare(getObjectId(b), getObjectId(a)));
            });
        });
    }

    private int getObjectId(Object o) {
        if (o instanceof User) return ((User) o).getId();
        if (o instanceof Auction) return ((Auction) o).getId();
        return 0;
    }

    // --- SETUP HIỂN THỊ DỮ LIỆU BẰNG LAMBDA ---

    private void setupTableColumns() {
        // Bảng quản lý User
        colUserId.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getId()));
        colUserName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUsername()));
        colUserStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRole()));

        // Bảng quản lý Đấu giá (Auction)
        colAuctionId.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getId()));
        colAuctionTitle.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItem().getName()));
        colAuctionStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAuctionState().name()));
    }

    private void setupActionButtons() {
        // Nút Ban/Unban (Dành cho bảng User)
        colUserAction.setCellFactory(p -> new TableCell<>() {
            private final Button btn = new Button();
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    User u = getTableView().getItems().get(getIndex());
                    boolean isBanned = "BAN".equals(u.getRole());

                    btn.setText(isBanned ? "Unban" : "Ban");

                    String baseStyle = "-fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 6 16; -fx-cursor: hand;";
                    if (isBanned) {
                        btn.setStyle("-fx-background-color: #3B82F6; " + baseStyle); // Màu xanh dương đồng bộ thiết kế
                    } else {
                        btn.setStyle("-fx-background-color: #EF4444; " + baseStyle); // Màu đỏ cảnh báo
                    }

                    btn.setOnAction(e -> sendBanRequest(u));
                    setGraphic(btn);
                }
            }
        });

        // Nút Cancel Auction (Dành cho bảng Auction)
        colAuctionAction.setCellFactory(p -> new TableCell<>() {
            private final Button btn = new Button("Cancel");
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    Auction a = getTableView().getItems().get(getIndex());

                    String activeStyle = "-fx-background-color: #F59E0B; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 6 16; -fx-cursor: hand;";
                    String disabledStyle = "-fx-background-color: #E2E8F0; -fx-text-fill: #94A3B8; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 6 16;";

                    boolean canCancel = a.getAuctionState() != AuctionState.FINISHED && a.getAuctionState() != AuctionState.CANCELED;

                    btn.setDisable(!canCancel);
                    btn.setStyle(canCancel ? activeStyle : disabledStyle);

                    btn.setOnAction(e -> sendCancelRequest(a));
                    setGraphic(btn);
                }
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
            // Tự động phân loại: Nếu đang bị BAN thì hành động tiếp theo là UNBAN, ngược lại là BAN
            String action = "BAN".equals(u.getRole()) ? "UNBAN" : "BAN";

            Request req = new Request(RequestType.BAN);
            req.put("userId", u.getId());
            req.put("action", action); // Đính kèm hành động cụ thể để Server biết đường rẽ nhánh

            ServerConnection.getInstance().send(req);
        } catch (IOException e) {
            e.printStackTrace();
        }
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