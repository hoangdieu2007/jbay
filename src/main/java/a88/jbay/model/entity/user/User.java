package a88.jbay.model.entity.user;

import a88.jbay.model.UniqueID;
import a88.jbay.model.entity.Entity;
import a88.jbay.model.system.UserSystem;

import java.util.Objects;

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

    public boolean matchPassword(String password) {
        return this.password.equals(password);
    }

    public void copy(User destination) {
        this.username = destination.username;
        this.password = destination.password;
        this.type = destination.type;
    }

    public void register() {
        if (!this.username.equals("guest")) {
            boolean success = UserSystem.getInstance().addUser(this);
            if (success) {
                System.out.println("User registered successfully");
            }
        }
    }

    public void login(String username, String password) {
        User user = UserSystem.getInstance().login(username, password);
        if (user != null) {
            this.copy(user);
            System.out.println("Login successful");
        } else System.out.println("Login failed");
    }

    public void logout() {
        this.username = "guest";
        this.password = "guest";
        this.type = "guest";

        System.out.println("Logout successful");
    }
}
