package a88.jbay.controller.client;

import a88.jbay.client.ClientSession;
import a88.jbay.client.ServerConnection;
import a88.jbay.common.network.Request;
import a88.jbay.common.network.RequestType;
import a88.jbay.controller.ControllerProvider;
import a88.jbay.view.ViewManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;

import java.io.IOException;

public class UserHomeScreenController {

    @FXML
    private StackPane contentArea;
    @FXML
    private Label lblUsername;


    public void initialize(){
        ViewManager.getInstance().setMainScene(contentArea);
        lblUsername.setText(ClientSession.getInstance().getUser().getUsername());
        ControllerProvider.getInstance().registerController(this);
        showMyListings();
    }

    @FXML
    private void handleLogOut(){
        try {
            ServerConnection.getInstance().send(new Request(RequestType.LOGOUT));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @FXML
    private void showMyListings(){
        try {
            ViewManager.getInstance().loadSubScene(contentArea, "client/my-Listings.fxml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @FXML
    private void showOngoingAuctions(){
        try{
            ViewManager.getInstance().loadSubScene(contentArea, "client/ongoing-Auctions.fxml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
