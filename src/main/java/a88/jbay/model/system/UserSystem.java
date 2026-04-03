package a88.jbay.model.system;

import a88.jbay.model.entity.user.User;

import java.util.ArrayList;
import java.util.HashMap;

public class UserSystem {
    private HashMap<String, User> users; // username, user
    private static UserSystem instance;

    private UserSystem() {
        users = new HashMap<>();
    }

    public static UserSystem getInstance() {
        if (instance == null) {instance = new UserSystem();}
        return instance;
    }

    public boolean addUser(User user) {
        if (users.containsKey(user.getUsername())) {
            System.out.println("User already exists!");
            return false;
        }
        users.put(user.getUsername(), user);
        System.out.println("User " + user.getUsername() + " has been successfully registered!");
        return true;
    }

    public void deleteUser(String username) {
        User user = users.get(username);
        if (user != null) users.remove(username);
        else System.out.println("No user found with username " + username);
    }

    public User login(String username, String password) {
        User user = users.get(username);
        if (user != null && user.matchPassword(password)) {
            return user;
        }
        System.out.println("Invalid username or password");
        return null;
    }
}
