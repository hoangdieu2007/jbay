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
    // Khai báo các thành phần FXML
    @FXML private Label adminNameLabel;

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colUserId;
    @FXML private TableColumn<User, String> colUserName, colUserStatus;
    @FXML private TableColumn<User, Void> colUserAction;
    @FXML private TextField searchUserField;

    @FXML private TableView<Auction> auctionTable;
    @FXML private TableColumn<Auction, Integer> colAuctionId;
    @FXML private TableColumn<Auction, String> colAuctionTitle, colAuctionWinner, colAuctionStatus;
    @FXML private TableColumn<Auction, Void> colAuctionAction;
    @FXML private TextField searchAuctionField;

    // Danh sách hiển thị trên UI
    private final ObservableList<User> userMasterList = FXCollections.observableArrayList();
    private final ObservableList<Auction> auctionMasterList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (ClientSession.getInstance().getUser() != null) {
            adminNameLabel.setText("👤 " + ClientSession.getInstance().getUser().getUsername());
        }

        setupTableColumns();
        setupActionButtons(); // Đã được vá lỗi chống kẹt nút
        setupRealtimeListeners();
        setupSearchFilters();
        refreshData();
    }

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

        colAuctionWinner.setCellValueFactory(cellData -> {
            String winner = cellData.getValue().getWinner();
            return new SimpleStringProperty((winner == null || winner.isEmpty()) ? "No bids yet" : winner);
        });

        colAuctionStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAuctionState().name()));
    }

    private void setupActionButtons() {
        // Nút Ban/Unban
        colUserAction.setCellFactory(p -> new TableCell<>() {
            private final Button btn = new Button();
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                // 🌟 VÁ LỖI 1: Lấy User an toàn qua Row, chống kẹt nút
                User u = (User) getTableRow().getItem();

                if (empty || u == null) {
                    setGraphic(null);
                } else {
                    boolean isBanned = "BAN".equals(u.getRole());
                    btn.setText(isBanned ? "Unban" : "Ban");
                    String baseStyle = "-fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 6 16; -fx-cursor: hand;";
                    btn.setStyle((isBanned ? "-fx-background-color: #3B82F6; " : "-fx-background-color: #EF4444; ") + baseStyle);
                    btn.setOnAction(e -> sendBanRequest(u));
                    setGraphic(btn);
                }
            }
        });

        // Nút Control Đấu giá (View / Cancel)
        colAuctionAction.setCellFactory(p -> new TableCell<>() {
            private final Button btnView = new Button("View");
            private final Button btnCancel = new Button("Cancel");
            private final HBox actionContainer = new HBox(10, btnView, btnCancel);

            { actionContainer.setStyle("-fx-alignment: center;"); }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                // 🌟 VÁ LỖI 1: Lấy Auction an toàn qua Row, chống kẹt nút
                Auction a = (Auction) getTableRow().getItem();

                if (empty || a == null) {
                    setGraphic(null);
                } else {
                    btnView.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 6 16; -fx-cursor: hand;");
                    btnView.setOnAction(e -> {
                        try {
                            // 🌟 VÁ LỖI 2: Dùng FXMLLoader load tay thay vì dùng ViewManager.loadIntoMainScene
                            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/a88/jbay/view/AuctionUI/viewAuction-view.fxml"));
                            javafx.scene.Parent root = loader.load();

                            ViewAuctionController controller = loader.getController();
                            controller.setAuctionData(
                                    a,
                                    ClientSession.getInstance().getAdminAuctions(),
                                    () -> {
                                        try {
                                            ViewManager.displayScene("client/Admin-HomeScreens.fxml");
                                        } catch (IOException ex) { ex.printStackTrace(); }
                                    }
                            );

                            // Gắn thẳng giao diện mới đè lên Scene hiện tại
                            btnView.getScene().setRoot(root);

                        } catch (IOException ex) {
                            ex.printStackTrace();
                            System.out.println("Lỗi mở màn hình View: " + ex.getMessage());
                        }
                    });

                    String activeStyle = "-fx-background-color: #F59E0B; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 6 16; -fx-cursor: hand;";
                    String disabledStyle = "-fx-background-color: #E2E8F0; -fx-text-fill: #94A3B8; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 6 16;";

                    boolean canCancel = a.getAuctionState() != AuctionState.FINISHED && a.getAuctionState() != AuctionState.CANCELED;
                    btnCancel.setDisable(!canCancel);
                    btnCancel.setStyle(canCancel ? activeStyle : disabledStyle);
                    btnCancel.setOnAction(e -> sendCancelRequest(a));

                    setGraphic(actionContainer);
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