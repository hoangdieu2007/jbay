package a88.jbay.controller.client;

import a88.jbay.client.ClientSession;
import a88.jbay.client.ServerConnection;
import a88.jbay.model.entity.user.User;
import a88.jbay.model.event.Auction;
import a88.jbay.model.network.Request;
import a88.jbay.model.network.RequestType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

public class AdminHomeScreenController {

    @FXML private Label userNameLabel;
    @FXML private TextField searchUserField, searchAuctionField;

    // Table và Column cho User
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colUserId, colUserName, colUserStatus;
    @FXML private TableColumn<User, Void> colUserAction;

    // Table và Column cho Auction
    @FXML private TableView<Auction> auctionTable;
    @FXML private TableColumn<Auction, String> colAuctionId, colAuctionTitle, colAuctionStatus;
    @FXML private TableColumn<Auction, Void> colAuctionAction;

    // Danh sách Observable để Table tự động cập nhật UI
    private final ObservableList<User> userMasterData = FXCollections.observableArrayList();
    private final ObservableList<Auction> auctionMasterData = FXCollections.observableArrayList();

    // Style cho nút bấm theo yêu cầu
    private final String BLUE_STYLE = "-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 15; -fx-font-weight: bold; -fx-cursor: hand;";
    private final String RED_STYLE = "-fx-background-color: #ff0000; -fx-text-fill: white; -fx-background-radius: 15; -fx-font-weight: bold; -fx-cursor: hand;";

    @FXML
    public void initialize() {
        // Hiển thị tên Admin hiện tại
        userNameLabel.setText(ClientSession.getInstance().getUser().getUsername());

        // GỌI KHỞI TẠO TÁCH BIỆT CHO 2 TAB
        initUserTab();
        initAuctionTab();
    }

    // KHỞI TẠO (SETUP + REQUEST DỮ LIỆU BAN ĐẦU)

    private void initUserTab() {
        // 1. Mapping thông tin vào cột (Tách dữ liệu nhét vào đúng mục)
        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUserName.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUserStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // 2. Thiết lập khuôn nút bấm riêng (CellFactory)
        setupUserActionButtons();

        // 3. Thiết lập Filter và Sort ID từ thấp đến cao
        FilteredList<User> filteredUsers = new FilteredList<>(userMasterData, p -> true);
        searchUserField.textProperty().addListener((o, old, val) -> {
            filteredUsers.setPredicate(u -> val == null || val.isEmpty() || u.getUsername().toLowerCase().contains(val.toLowerCase()));
        });
        SortedList<User> sortedUsers = new SortedList<>(filteredUsers);
        sortedUsers.setComparator(Comparator.comparingInt(User::getId));
        userTable.setItems(sortedUsers);

        // 4. GỬI REQUEST LẤY DANH SÁCH HIỆN TẠI
        try {
            ServerConnection.getInstance().send(new Request(RequestType.GET_AUCTIONS).put("target", "ALL_USERS"));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void initAuctionTab() {
        colAuctionId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAuctionTitle.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colAuctionStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        setupAuctionActionButtons();

        FilteredList<Auction> filteredAuctions = new FilteredList<>(auctionMasterData, p -> true);
        searchAuctionField.textProperty().addListener((o, old, val) -> {
            filteredAuctions.setPredicate(a -> val == null || val.isEmpty() || a.getItem().getName().toLowerCase().contains(val.toLowerCase()));
        });
        SortedList<Auction> sortedAuctions = new SortedList<>(filteredAuctions);
        sortedAuctions.setComparator(Comparator.comparingInt(Auction::getId));
        auctionTable.setItems(sortedAuctions);

        try {
            ServerConnection.getInstance().send(new Request(RequestType.GET_AUCTIONS).put("target", "ALL_AUCTIONS"));
        } catch (IOException e) { e.printStackTrace(); }
    }

    // THIẾT LẬP NÚT BẤM (CELL FACTORY)

    private void setupUserActionButtons() {
        colUserAction.setCellFactory(param -> new TableCell<>() {
            private final Button unbanBtn = new Button("Unban"), banBtn = new Button("Ban");
            private final HBox container = new HBox(10, unbanBtn, banBtn);
            {
                unbanBtn.setStyle(BLUE_STYLE); banBtn.setStyle(RED_STYLE);
                banBtn.setOnAction(e -> sendRequest(RequestType.MISC, "BAN", getTableRow().getItem().getId()));
                unbanBtn.setOnAction(e -> sendRequest(RequestType.MISC, "UNBAN", getTableRow().getItem().getId()));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void setupAuctionActionButtons() {
        colAuctionAction.setCellFactory(param -> new TableCell<>() {
            private final Button viewBtn = new Button("View"), cancelBtn = new Button("Cancel");
            private final HBox container = new HBox(10, viewBtn, cancelBtn);
            {
                viewBtn.setStyle(BLUE_STYLE); cancelBtn.setStyle(RED_STYLE);
                cancelBtn.setOnAction(e -> sendRequest(RequestType.CANCEL, "ID", getTableRow().getItem().getId()));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void sendRequest(RequestType type, String action, Object id) {
        try {
            ServerConnection.getInstance().send(new Request(type).put("action", action).put("id", id));
        } catch (IOException e) { e.printStackTrace(); }
    }
}