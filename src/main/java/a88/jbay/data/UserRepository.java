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

    public UserData findByUserId(int userId) {
        return userDAO.findByUserId(userId);
    }

    public boolean usernameExists(String username) {
        return userDAO.existsByUsername(username);
    }

    public boolean createUser(String username, String hashedPassword, String role) {
        return userDAO.insertUser(username, hashedPassword, role) != -1;
    }

    public boolean createSession(String sessionId, User user) {
        sessionCache.put(sessionId, user);
        return true;
    }

    public void deleteSession(String sessionId) {
        sessionCache.remove(sessionId);
    }

    public User findBySessionId(String sessionId) {
        return sessionCache.get(sessionId);
    }

    public List<User> getAllNormalUsers() {
        return userDAO.getAllNormalUsers().stream()
                .map(data -> new User(data.id(), data.role(), data.username()))
                .collect(Collectors.toList());
    }

    public byte[] getQr(int userId) {
        return userDAO.getQr(userId);
    }

    public boolean updateRole(int userId, String newRole) {
        boolean updated = userDAO.changeUserRole(userId, newRole);
        if (updated) {
            if (newRole.equals("BAN")) {
                sessionCache.values().removeIf(user -> user.getId() == userId);
            } else {
                sessionCache.entrySet().stream()
                        .filter(e -> e.getValue().getId() == userId)
                        .forEach(e -> {
                            UserData fresh = userDAO.findByUserId(userId);
                            if (fresh != null) {
                                sessionCache.put(e.getKey(), new User(fresh.id(), fresh.role(), fresh.username()));
                            }
                        });
            }
        }
        return updated;
    }
}
