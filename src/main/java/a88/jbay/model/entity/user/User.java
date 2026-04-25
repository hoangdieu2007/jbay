// the user class, for CLIENT app
// contain only username and session id, for security
// the bidder seller admin model is so rigid and hard to maintain. i'm gonna switch to state pattern

//extends entity and implements observer

package a88.jbay.model.entity.user;

import a88.jbay.model.Observer;
import a88.jbay.model.entity.Entity;
import a88.jbay.model.entity.user.role.Permission;
import a88.jbay.model.entity.user.role.Role;
import a88.jbay.model.event.Auction;

import java.io.Serializable;

public class User extends Entity implements Observer, Serializable {
    private static final long serialVersionUID = 1L;
    private Credentials credentials;

    public User() {
        super();
        this.credentials = new Credentials("GUEST", "guest", "none");
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * Checks if the user has permission to perform a specific action.
     */
    public boolean can(Permission.ActionType action) {
        Role role = Role.fromString(credentials.getRole());
        return Permission.isAllowed(role, action);
    }

    @Override
    public void update(Auction auction) {
        // Notification logic
        System.out.println("Update for user " + credentials.getUsername());
    }
}