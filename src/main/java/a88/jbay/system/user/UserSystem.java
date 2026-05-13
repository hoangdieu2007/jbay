package a88.jbay.system.user;

import a88.jbay.dao.UserDAO;
import a88.jbay.dao.UserDAO.UserData;
import a88.jbay.util.StringHash;
import a88.jbay.util.JBayLogger;
import a88.jbay.common.user.User;
import a88.jbay.di.ApplicationContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserSystem {

    private final UserDAO userDAO;
    private final JBayLogger logger;

    // session cache only (no network logic here)
    private final Map<String, User> sessionCache = new ConcurrentHashMap<>();

    public UserSystem(UserDAO userDAO) {
        this.userDAO = userDAO;
        this.logger = JBayLogger.getLogger(UserSystem.class);
    }

    public static UserSystem getInstance() {
        return ApplicationContext.getInstance().getDependency(UserSystem.class);
    }

    public User login(String username, String password) {
        logger.debug("Login attempt: " + username);

        UserData userData = userDAO.findByUsername(username);
        if (userData == null) {
            logger.warn("User not found: " + username);
            return null;
        }

        if (!StringHash.hash(password).equals(userData.password())) {
            logger.warn("Invalid password: " + username);
            return null;
        }

        String sessionId = UUID.randomUUID().toString();

        if (!userDAO.insertSession(sessionId, userData.id())) {
            logger.error("Failed to create session: " + username);
            return null;
        }

        User user = new User(userData.id(), userData.role(), username, sessionId);
        sessionCache.put(sessionId, user);

        return user;
    }

    public boolean register(String username, String password, String role) {
        logger.debug("Register: " + username);

        if (userDAO.existsByUsername(username)) {
            logger.warn("Username exists: " + username);
            return false;
        }

        return userDAO.insertUser(
                username,
                StringHash.hash(password),
                role
        ) != -1;
    }

    public void logout(String sessionId) {
        logger.debug("Logout: " + sessionId);

        sessionCache.remove(sessionId);
        userDAO.deleteSession(sessionId);
    }

    public User findBySessionId(String sessionId) {
        User cached = sessionCache.get(sessionId);
        if (cached != null) return cached;

        UserData data = userDAO.findBySessionId(sessionId);
        if (data == null) return null;

        return new User(data.id(), data.role(), data.username(), sessionId);
    }

    public java.util.List<User> getAllNormalUsersForAdmin() {
        java.util.List<UserDAO.UserData> rawUsers = userDAO.getAllNormalUsers();
        java.util.List<User> userList = new java.util.ArrayList<>();

        if (rawUsers != null) {
            for (UserDAO.UserData data : rawUsers) {
                // Dùng constructor 3 tham số (id, role, username) - Session tự động gán là "none"
                userList.add(new User(data.id(), data.role(), data.username()));
            }
        }
        return userList;
    }
}