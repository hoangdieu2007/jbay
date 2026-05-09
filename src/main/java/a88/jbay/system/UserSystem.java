package a88.jbay.system;

import a88.jbay.dao.UserDAO;
import a88.jbay.dao.UserDAO.UserData;
import a88.jbay.util.StringHash;
import a88.jbay.util.JBayLogger;
import a88.jbay.model.entity.user.User;
import a88.jbay.model.network.Response;

import java.util.ArrayList;
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
    private final JBayLogger logger;

    private final Map<Integer, List<User>> activeUsers = new ConcurrentHashMap<>();

    private UserSystem() {
        this.userDAO = UserDAO.getInstance();
        this.logger = JBayLogger.getInstance();
    }

    public static synchronized UserSystem getInstance() {
        if (instance == null) {
            instance = new UserSystem();
        }
        return instance;
    }

    //login: find user by username, then check password and generate session if valid
    public User login(String username, String password) {
        logger.debug("Attempting login for user: " + username);
        UserData userData = userDAO.findByUsername(username);
        if (userData == null) {
            logger.warn("Login failed - user not found: " + username);
            return null;
        }

        String hashedPassword = StringHash.hash(password);

        if (!hashedPassword.equals(userData.password())) {
            logger.warn("Login failed - invalid password for user: " + username);
            return null;
        }

        String sessionId = UUID.randomUUID().toString();
        if (userDAO.insertSession(sessionId, userData.id())) {
            logger.info("User logged in successfully: " + username);
            return new User(userData.id(), userData.role(), userData.username(), sessionId);
        }
        logger.error("Login failed - session creation failed for user: " + username);
        return null;
    }

    //register: find by username, then creates account if not exist
    public boolean register(String username, String password, String role) {
        logger.debug("Attempting registration for user: " + username);
        if (userDAO.existsByUsername(username)) {
            logger.warn("Registration failed - username already exists: " + username);
            return false;
        }

        password = StringHash.hash(password);
        
        boolean success = userDAO.insertUser(username, password, role) != -1;
        if (success) {
            logger.info("User registered successfully: " + username);
        } else {
            logger.error("Registration failed - database error for user: " + username);
        }
        return success;
    }

    public void logout(String sessionId) {
        logger.debug("User logging out with session: " + sessionId);
        userDAO.deleteSession(sessionId);
        logger.info("User logged out successfully");
    }

    public User findBySessionId(String sessionId) {
        UserData userData = userDAO.findBySessionId(sessionId);
        if (userData == null) return null;
        return new User(userData.id(), userData.role(), userData.username(), sessionId);
    }

    public void addActiveUser(int userId, User user) {
        activeUsers.computeIfAbsent(userId, k -> new ArrayList<>()).add(user);
    }

    //ban and cleanup current user cache
    public boolean banUser(int userId) {
        logger.info("Attempting to ban user ID: " + userId);
        if (userDAO.findByUserId(userId) == null) {
            logger.warn("Ban failed - user not found: " + userId);
            return false;
        }

        if (userDAO.changeUserRole(userId, "BAN")) {
            logger.info("User banned successfully: " + userId);
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

        logger.error("Ban failed - database error for user: " + userId);
        return false;
    }

    //unban user
    public boolean unbanUser(int userId) {
        logger.info("Attempting to unban user ID: " + userId);
        if (userDAO.findByUserId(userId) == null) {
            logger.warn("Unban failed - user not found: " + userId);
            return false;
        }

        boolean success = userDAO.changeUserRole(userId, "USER");
        if (success) {
            logger.info("User unbanned successfully: " + userId);
        } else {
            logger.error("Unban failed - database error for user: " + userId);
        }
        return success;
    }
}