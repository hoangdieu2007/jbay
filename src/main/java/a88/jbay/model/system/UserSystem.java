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
        if (users.containsKey(user.getUsername())) {return false;}
        users.put(user.getUsername(), user);
        return true;
    }

    public void deleteUser(String username) {
        User user = users.get(username);
        if (user != null) users.remove(username);
    }
}
