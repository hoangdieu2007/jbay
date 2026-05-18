package a88.jbay.data;

import a88.jbay.common.user.User;
import a88.jbay.common.user.UserData;
import a88.jbay.dao.UserDAO;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class UserRepository {

    private final UserDAO userDAO;
    private final Map<String, User> sessionCache = new ConcurrentHashMap<>();

    public UserRepository(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public UserData findByUsername(String username) {
        return userDAO.findByUsername(username);
    }

    public boolean usernameExists(String username) {
        return userDAO.existsByUsername(username);
    }

    public boolean createUser(String username, String hashedPassword, String role) {
        return userDAO.insertUser(username, hashedPassword, role) != -1;
    }

    public boolean createSession(String sessionId, User user) {
        boolean success = userDAO.insertSession(sessionId, user.getId());
        if (success) sessionCache.put(sessionId, user);
        return success;
    }

    public void deleteSession(String sessionId) {
        sessionCache.remove(sessionId);
        userDAO.deleteSession(sessionId);
    }

    public User findBySessionId(String sessionId) {
        User cached = sessionCache.get(sessionId);
        if (cached != null) return cached;

        UserData data = userDAO.findBySessionId(sessionId);
        if (data == null) return null;

        User user = new User(data.id(), data.role(), data.username(), sessionId);
        sessionCache.put(sessionId, user);
        return user;
    }

    public List<User> getAllNormalUsers() {
        return userDAO.getAllNormalUsers().stream()
                .map(data -> new User(data.id(), data.role(), data.username()))
                .collect(Collectors.toList());
    }
}