package a88.jbay.model.entity.user;

import a88.jbay.model.entity.Entity;
import a88.jbay.model.entity.user.role.State;
import a88.jbay.model.event.Auction;

// the user class, for CLIENT app
// contain only username and session id, for security
// the bidder seller admin model is so rigid and hard to maintain. i'm gonna switch to state pattern

//extends entity and implements observer
public class User extends Entity {
    //the user has credentials, can be swapped so they can authorize different account
    private Credentials credentials;
    private State state;

    public User() {
        super();
        this.credentials = new Credentials("guest", "guest", "guest");
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    //bidding
    public String bid() {
        return state.bid();
    }

    //selling
    public String sell() {
        return state.sell();
    }

    //administration
    public String ban() {
        return state.ban();
    }

    public void update(Auction auction) {
        // info to notify to users
    }
}
