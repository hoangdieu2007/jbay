package a88.jbay.controller.client;

import a88.jbay.client.ClientSession;
import a88.jbay.client.ServerConnection;
import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import a88.jbay.common.user.User;
import a88.jbay.view.ViewManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.io.IOException;

public class AdminHomeScreenController {
    // Khai báo các thành phần FXML
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
        // Setup hiển thị dữ liệu
        setupTableColumns();

        // Setup nút bấm động (Ban/Cancel)
        setupActionButtons();

        // Setup Real-time Listener - Tự động đồng bộ từ Map sang List
        setupRealtimeListeners();

        // Setup bộ lọc tìm kiếm
        setupSearchFilters();

        // Lấy dữ liệu ban đầu
        refreshData();
    }

    // --- CƠ CHẾ REAL-TIME: TỰ ĐỘNG ĐỒNG BỘ ---

    private void setupRealtimeListeners() {
        // Nghe ngóng trực tiếp từ "kho" trong ClientSession
        // Hễ ResponseHandler ném đồ vào Map là UI ở đây tự nhảy
        syncMapToList(ClientSession.getInstance().getAdminUsers(), userMasterList);
        syncMapToList(ClientSession.getInstance().getAdminAuctions(), auctionMasterList);
    }

    private <T> void syncMapToList(ObservableMap<Integer, T> sourceMap, ObservableList<T> targetList) {
        sourceMap.addListener((MapChangeListener<Integer, T>) change -> {
            Platform.runLater(() -> {
                // Xóa cũ thêm mới để cập nhật dòng
                targetList.removeIf(item -> {
                    if (item instanceof User) return ((User) item).getId() == change.getKey();
                    if (item instanceof Auction) return ((Auction) item).getId() == change.getKey();
                    return false;
                });
                if (change.wasAdded()) {
                    targetList.add(change.getValueAdded());
                }
                // Sắp xếp: ID lớn (mới nhất) lên đầu
                targetList.sort((a, b) -> Integer.compare(getObjectId(b), getObjectId(a)));
            });
        });
    }

    private int getObjectId(Object o) {
        if (o instanceof User) return ((User) o).getId();
        if (o instanceof Auction) return ((Auction) o).getId();
        return 0;
    }

    // --- SETUP GIAO DIỆN & NÚT BẤM ---

    private void setupTableColumns() {
        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUserName.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUserStatus.setCellValueFactory(new PropertyValueFactory<>("role"));

        colAuctionId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAuctionTitle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItem().getName()));
        colAuctionStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAuctionState().name()));
    }

    private void setupActionButtons() {
        // Nút Ban/Unban
        colUserAction.setCellFactory(p -> new TableCell<>() {
            private final Button btn = new Button();
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    User u = getTableView().getItems().get(getIndex());
                    boolean isBanned = "BAN".equals(u.getRole());
                    btn.setText(isBanned ? "Unban" : "Ban");
                    btn.setStyle(isBanned ? "-fx-background-color: #10B981; -fx-text-fill: white;" : "-fx-background-color: #EF4444; -fx-text-fill: white;");
                    btn.setOnAction(e -> sendBanRequest(u));
                    setGraphic(btn);
                }
            }
        });

        // Nút Cancel Auction
        colAuctionAction.setCellFactory(p -> new TableCell<>() {
            private final Button btn = new Button("Cancel");
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    Auction a = getTableView().getItems().get(getIndex());
                    btn.setDisable(a.getAuctionState() == AuctionState.FINISHED || a.getAuctionState() == AuctionState.CANCELED);
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

    // --- GỬI LỆNH LÊN SERVER ---

    private void refreshData() {
        try {
            ServerConnection.getInstance().send(new Request(RequestType.GET_USERS));
            ServerConnection.getInstance().send(new Request(RequestType.GET_AUCTIONS));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void sendBanRequest(User u) {
        try {
            String newRole = "BAN".equals(u.getRole()) ? "USER" : "BAN";
            Request req = new Request(RequestType.MISC).put("command", "CHANGE_ROLE").put("targetId", u.getId()).put("newRole", newRole);
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