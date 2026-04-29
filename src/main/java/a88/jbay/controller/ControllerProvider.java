package a88.jbay.controller;

import a88.jbay.controller.client.*;

import java.util.HashMap;
import java.util.Map;

public class ControllerProvider {
    private static ControllerProvider instance;
    private Map<String, Object> controllers = new HashMap<>();
    
    private ControllerProvider() {}
    
    public static ControllerProvider getInstance() {
        if (instance == null) {
            instance = new ControllerProvider();
        }
        return instance;
    }
    
    public void registerController(String name, Object controller) {
        controllers.put(name, controller);
    }
    
    // client controllers
    public ClientLoginRegisterController getLoginRegisterController() {
        return (ClientLoginRegisterController) controllers.get("loginRegister");
    }

    public MainClientController getMainClientController() {
        return (MainClientController) controllers.get("mainClient");
    }

    public ClientBidderItemController getBidderItemController() {
        return (ClientBidderItemController) controllers.get("bidderItem");
    }

    public ClientSellerItemController getSellerItemController() {
        return (ClientSellerItemController) controllers.get("sellerItem");
    }

    public SellerBidderHomeScreenController getHomeScreenController() {
        return (SellerBidderHomeScreenController) controllers.get("homeScreen");
    }

    public BidderItemCardController getBidderItemCardController() {
        return (BidderItemCardController) controllers.get("bidderItemCard");
    }

    public SellerItemCardController getSellerItemCardController() {
        return (SellerItemCardController) controllers.get("sellerItemCard");
    }

    // get general controller, caller need to type cast
    public Object getController(String name) {
        return controllers.get(name);
    }
    
    public void removeController(String name) {
        controllers.remove(name);
    }
    
    public boolean hasController(String name) {
        return controllers.containsKey(name);
    }
}
