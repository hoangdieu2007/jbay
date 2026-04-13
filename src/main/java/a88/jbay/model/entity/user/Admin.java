package a88.jbay.model.entity.user;

import a88.jbay.model.system.AuctionSystem;
import a88.jbay.model.system.UserSystem;

public class Admin extends User {
    public Admin()   {
        super();
        this.type = "admin";
        this.username = "admin";
    }

    public void deleteUser(String id) {
        UserSystem.getInstance().deleteUser(id);
    }

    public void endAuction(String id) {
        AuctionSystem.getInstance().endAuction(id);
    }

    public void setChanged() {}

    public void notifyObservers() {

    }
}
