package a88.jbay.system;

import a88.jbay.dao.UserDAO;
import a88.jbay.dao.UserDAO.UserData;
import a88.jbay.model.StringHash;
import a88.jbay.model.entity.user.User;
import a88.jbay.model.network.Response;

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
        UserData userData = userDAO.findByUsername(username);
        if (userData == null) return null;

        String hashedPassword = StringHash.hash(password);

        if (!hashedPassword.equals(userData.password())) return null;

        String sessionId = UUID.randomUUID().toString();
        if (userDAO.insertSession(sessionId, userData.id())) {
            return new User(userData.id(), userData.role(), userData.username(), sessionId);
        }
        return null;
    }

    //register: find by username, then creates account if not exist
    public boolean register(String username, String password, String role) {
        if (userDAO.existsByUsername(username)) {
            return false;
        }

        password = StringHash.hash(password);
        
        return userDAO.insertUser(username, password, role) != -1;
    }

    public void logout(String sessionId) {
        userDAO.deleteSession(sessionId);
    }

    public User getBySessionId(String sessionId) {
        UserData userData = userDAO.findBySessionId(sessionId);
        if (userData == null) return null;
        return new User(userData.id(), userData.role(), userData.username(), sessionId);
    }

    public void addActiveUser(int userId, User user) {
        activeUsers.computeIfAbsent(userId, k -> List.of()).add(user);
    }

    public boolean banUser(int userId) {
        if (userDAO.findByUserId(userId) == null) return false;

        if (userDAO.changeUserRole(userId, "BAN")) {
            UpdateSystem.getInstance().unsubscribeUserFromAllAuctions(userId);
            // when client receives this it will switch to login scene
            UpdateSystem.getInstance().updateByUserId(userId, new Response(true, "BAN_USER", null));
            UpdateSystem.getInstance().unregister(userId);
            activeUsers.remove(userId);

            List<User> sessions = activeUsers.get(userId);
            if (sessions != null && !sessions.isEmpty()) {
                for (User user : sessions) {
                    logout(user.getSessionId());
                }
            }
            return true;
        }

        return false;
    }
}