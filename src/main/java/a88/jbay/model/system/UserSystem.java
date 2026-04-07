package a88.jbay.model.system;

import a88.jbay.model.entity.user.User;

import java.util.ArrayList;
import java.util.HashMap;

//server app code
//singleton class for user system
// contain logics for handling the users
public class UserSystem {
    private HashMap<String, User> users; // username, user

    private UserSystem() {
        users = new HashMap<>();
    }

    // the singleton instance holder
    // why? class initialization is thread-safe
    private static class  SingletonHolder {
        private static final UserSystem INSTANCE = new UserSystem();
    }

    // the get instance method, simply works by returning the holder's instance
    public static UserSystem getInstance() {
        return UserSystem.SingletonHolder.INSTANCE;
    }

    public String addUser(User user) {
        //userdao connection

        return null;
    }

    public String deleteUser(String username) {
        // userdao connection

        return null;
    }

    public String login(String username, String password) {
        //login server, check credential and return message

        return null;
    }
}
