package a88.jbay.system;

import a88.jbay.dao.UserDAO;
import a88.jbay.model.StringHash;
import a88.jbay.model.entity.user.User;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/*
the code for operations on the user data
features: login, register, logout
subscrition are handled by the notification system and client handler, the only responsibility of this class is to manage the user data
 */

public class UserSystem {
    private static UserSystem instance;
    private final UserDAO userDAO;

    private final Map<Integer, List<User>> activeUsers = new ConcurrentHashMap<>();

    private UserSystem() {
        this.userDAO = UserDAO.getInstance();
    }

    public static synchronized UserSystem getInstance() {
        if (instance == null) {
            instance = new UserSystem();
        }
        return instance;
    }

    //login: find user by username, then check password and generate session if valid
    public User login(String username, String password) {
        Map<String, String> userData = userDAO.findByUsername(username);
        if (userData == null) return null;

        String hashedPassword = StringHash.hash(password);

        if (!hashedPassword.equals(userData.get("password"))) return null;

        String sessionId = UUID.randomUUID().toString();
        if (userDAO.insertSession(sessionId, Integer.parseInt(userData.get("id")))) {
            return new User(Integer.parseInt(userData.get("id")),userData.get("role"), userData.get("username"), sessionId);
        }
        return null;
    }

    //register: find by username, then creates account if not exist
    public boolean register(String username, String password, String role) {
        if (userDAO.existsByUsername(username)) {
            return false;
        }
        String hashedPassword = StringHash.hash(password);
        return userDAO.insertUser(username, hashedPassword, role) != -1;
    }

    public void logout(String sessionId) {
        userDAO.deleteSession(sessionId);
    }

    public User getBySessionId(String sessionId) {
        Map<String, String> userData = userDAO.findBySessionId(sessionId);
        if (userData == null) return null;
        return new User(Integer.parseInt(userData.get("id")),userData.get("role"), userData.get("username"), sessionId);
    }
}