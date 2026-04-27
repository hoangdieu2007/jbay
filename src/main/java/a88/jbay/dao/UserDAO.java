package a88.jbay.dao;

import a88.jbay.controller.server.DatabaseController;
import a88.jbay.model.entity.user.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserDAO {
    private static UserDAO instance;

    private UserDAO() {}

    private Map<String, String> extractUserFromResultSet(ResultSet rs) throws SQLException {
        Map<String, String> userData = new HashMap<>();
        userData.put("id", String.valueOf(rs.getInt("id")));
        userData.put("username", rs.getString("username"));
        userData.put("password", rs.getString("password"));
        userData.put("role", rs.getString("role"));
        return userData;
    }

    private Map<String, String> executeUserQuery(String sql, Object... params) {
        try (Connection connection = DatabaseController.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof String) {
                    stmt.setString(i + 1, (String) params[i]);
                } else if (params[i] instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) params[i]);
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return extractUserFromResultSet(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean executeUpdate(String sql, Object... params) {
        try (Connection connection = DatabaseController.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof String) {
                    stmt.setString(i + 1, (String) params[i]);
                } else if (params[i] instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) params[i]);
                }
            }

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static synchronized UserDAO getInstance() {
        if (instance == null) {
            instance = new UserDAO();
        }
        return instance;
    }

    public Map<String, String> findByUsername(String username) {
        String sql = "SELECT id, username, password, role FROM users WHERE username = ?";
        return executeUserQuery(sql, username);
    }

    public Map<String, String> findByUserId(int userId) {
        String sql = "SELECT id, username, password, role FROM users WHERE id = ?";
        return executeUserQuery(sql, userId);
    }

    public Map<String, String> findBySessionId(String sessionId) {

        String sql = "SELECT userid FROM sessionids WHERE id = ?";

        try (Connection connection = DatabaseController.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, sessionId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return findByUserId(rs.getInt("userid"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection connection = DatabaseController.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int insertUser(String username, String hashedPassword, String role) {

        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseController.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, role);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                return -1;
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }

            return -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public boolean insertSession(String sessionId, int userId) {
        String sql = "INSERT INTO sessionids (id, userid) VALUES (?, ?)";
        return executeUpdate(sql, sessionId, userId);
    }

    public boolean deleteSession(String sessionId) {
        String sql = "DELETE FROM sessionids WHERE id = ?";
        return executeUpdate(sql, sessionId);
    }

    public boolean changeUserRole(int userId, String role) {
        String sql = "UPDATE users SET role = ? WHERE id = ?";
        return executeUpdate(sql, role, userId);
    }
}