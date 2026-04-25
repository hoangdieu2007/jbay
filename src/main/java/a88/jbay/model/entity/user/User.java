// the user class, for CLIENT app
// contain only username and session id, for security
// the bidder seller admin model is so rigid and hard to maintain. i'm gonna switch to state pattern

//extends entity and implements observer

package a88.jbay.model.entity.user;

import a88.jbay.model.Observer;
import a88.jbay.model.entity.Entity;
import a88.jbay.model.entity.user.role.ActionType;
import a88.jbay.model.entity.user.role.Permission;
import a88.jbay.model.entity.user.role.Role;
import a88.jbay.model.event.Auction;

import java.io.Serializable;

/**
this class is sent over network for authorization processes
 */
public class User extends Entity implements Observer, Serializable {
    private static final long serialVersionUID = 1L;

    //credentials
    private final int id;
    private final String role;
    private final String username;
    private final String sessionId;

    public User(int id, String role, String username, String sessionId) {
        super();
        this.id = id;
        this.role = role;
        this.username = username;
        this.sessionId = sessionId;
    }

    public User() {
        this(-1, "GUEST", "guest", "none");
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getSessionId() { return sessionId; }

    /**
     * Checks if the user has permission to perform a specific action.
     */
    public boolean can(ActionType action) {
        Role role = Role.fromString(this.role);
        return Permission.isAllowed(role, action);
    }

    @Override
    public void update(Auction auction) {
        // Notification logic
        System.out.println("Update for user " + username);
    }
}