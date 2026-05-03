package a88.jbay.system;

import a88.jbay.dao.UserDAO;
import a88.jbay.dao.UserDAO.UserData;
import a88.jbay.model.StringHash;
import a88.jbay.model.entity.user.User;
import a88.jbay.model.network.Response;
import a88.jbay.server.ClientConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * manages user data and sessions
 * handles authentication, user operations, and active session tracking
 * consolidated session management from separate SessionManager
 */
public class UserSystem {
    private static UserSystem instance;
    private final UserDAO userDAO;
    private final Map<Integer, ClientConnection> userSessions = new ConcurrentHashMap<>();

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

    
    /**
     * bans a user and cleans up their sessions
     * @param userId the user id to ban
     * @return true if successful
     */
    public boolean banUser(int userId) {
        if (userDAO.findByUserId(userId) == null) return false;

        if (userDAO.changeUserRole(userId, "BAN")) {
            // Remove user session
            removeUserSession(userId);
            
            // Notify user about ban
            ClientConnection connection = userSessions.get(userId);
            if (connection != null && connection.isConnected()) {
                Response banResponse = new Response(true, "BAN_USER", null);
                connection.sendResponse(banResponse);
            }
            
            return true;
        }

        return false;
    }

    //unban user
    public boolean unbanUser(int userId) {
        if (userDAO.findByUserId(userId) == null) return false;

        return userDAO.changeUserRole(userId, "USER");
    }

    /**
     * registers a user session with their client connection
     * @param userId the user id
     * @param connection the client connection
     */
    public synchronized void registerUserSession(int userId, ClientConnection connection) {
        userSessions.put(userId, connection);
        System.out.println("User session registered for user " + userId);
    }

    /**
     * removes a user session
     * @param userId the user id
     */
    public synchronized void removeUserSession(int userId) {
        ClientConnection connection = userSessions.remove(userId);
        if (connection != null) {
            connection.close();
            System.out.println("User session removed for user " + userId);
        }
    }

    /**
     * checks if a user has an active session
     * @param userId the user id
     * @return true if active session exists
     */
    public synchronized boolean hasActiveSession(int userId) {
        ClientConnection connection = userSessions.get(userId);
        return connection != null && connection.isConnected();
    }

    /**
     * gets the number of active user sessions
     * @return active session count
     */
    public synchronized int getActiveSessionCount() {
        return userSessions.size();
    }

    /**
     * shuts down all user sessions
     */
    public synchronized void shutdownAllSessions() {
        userSessions.values().forEach(ClientConnection::close);
        userSessions.clear();
        System.out.println("All user sessions shutdown complete");
    }
}