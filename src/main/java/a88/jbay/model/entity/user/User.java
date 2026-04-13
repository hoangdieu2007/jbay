package a88.jbay.model.entity.user;

import a88.jbay.model.Observer;
import a88.jbay.model.UniqueID;
import a88.jbay.model.entity.Entity;
import a88.jbay.model.event.Auction;
import a88.jbay.model.system.UserSystem;

import java.util.Objects;

// the user class, for CLIENT app
// contain only username and session id, for security

public abstract class User extends Entity {
    protected String type;
    protected String sessionId;
    protected String username;

    public User() {
        super();
        this.id = UniqueID.genUID();
        this.type = "guest";
        this.username = "guest";
        this.sessionId = "guest";
    }

    public String getUsername() {
        return username;
    }

    public void copy(User destination) {
        this.username = destination.username;
        this.sessionId = destination.sessionId;
        this.type = destination.type;
    }

    public void register(String username, String password) {
        //client register
    }

    public void login(String username, String password) {
        //client login
    }

    public void logout() {
        // client logout
    }

    public void update(Auction auction) {
        // info to notify to users
    }
}
