// the user class, for CLIENT app
// contain only username and session id, for security
// the bidder seller admin model is so rigid and hard to maintain. i'm gonna switch to state pattern

//extends entity and implements observer

package a88.jbay.common.user;

import a88.jbay.common.Observer;
import a88.jbay.common.network.RequestType;
import a88.jbay.common.user.role.Permission;
import a88.jbay.common.user.role.Role;
import a88.jbay.common.auction.Auction;

import java.io.Serializable;

/**
this class is sent over network for authorization processes
 it implements observer interface to receive auction updates
 updates are sent from notification system, the update method of this object is only for local update after receiving auction updates
 */
public class User implements Observer, Serializable {
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

    public User(int id, String role, String username) {
        super();
        this.id = id;
        this.role = role;
        this.username = username;
        this.sessionId = null;
    }

    public User() {
        this(-1, "GUEST", "guest", null);
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getSessionId() { return sessionId; }

    /**
     * Checks if the user has permission to perform a specific action.
     */
    public boolean can(RequestType action) {
        Role role = Role.fromString(this.role);
        return Permission.isAllowed(role, action);
    }

    public String toString() {
        return Integer.toString(id) + " " + username + " " + role;
    }

    @Override
    public void update(Auction auction) {
        // updating data from the auction object received from the notification system
        System.out.println("Update for user " + username + " with auction " + auction.getId());
    }
}