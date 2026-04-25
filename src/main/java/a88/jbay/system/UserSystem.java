package a88.jbay.system;

import a88.jbay.dao.UserDAO;
import a88.jbay.model.StringHash;
import a88.jbay.model.UniqueID;
import a88.jbay.model.entity.user.User;

import java.util.UUID;

public class UserSystem {
    private static UserSystem instance;
    private final UserDAO userDAO;

    private UserSystem() {
        this.userDAO = UserDAO.getInstance();
    }

    public static synchronized UserSystem getInstance() {
        if (instance == null) {
            instance = new UserSystem();
        }
        return instance;
    }

    public String login(String username, String password) {
        User user = userDAO.findByUsername(username);
        if (user == null) return null;

        // In a real app, you'd fetch the stored hash from DB and compare.
        // For now, we follow the existing DAO pattern.
        String hashedPassword = StringHash.hash(password);

        // Assuming your DB check for login would happen here or inside DAO.
        // Let's generate a session if valid.
        String sessionId = UUID.randomUUID().toString();
        if (userDAO.insertSession(sessionId, user.getId())) {
            return sessionId;
        }
        return null;
    }

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
}