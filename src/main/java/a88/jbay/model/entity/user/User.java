package a88.jbay.model.entity.user;

import a88.jbay.model.UniqueID;
import a88.jbay.model.entity.Entity;
import a88.jbay.model.system.UserSystem;

public abstract class User extends Entity {
    protected String type;
    protected String username;
    protected String password;

    public User() {
        super();
        this.id = UniqueID.genUID();
        this.type = "guest";
        this.username = "guest";
        this.password = "guest";
    }

    public String getUsername() {
        return username;
    }

    public void register() {
        if (this.username != "guest") {
            boolean success = UserSystem.getInstance().addUser(this);
            if (success) {
                System.out.println("User registered successfully");
            }
        }
    }
}
