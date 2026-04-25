package a88.jbay.dao;

import a88.jbay.controller.server.DatabaseController;
import a88.jbay.model.entity.user.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
    private static UserDAO instance;

    private UserDAO() {}

    public static synchronized UserDAO getInstance() {
        if (instance == null) {
            instance = new UserDAO();
        }
        return instance;
    }

    public User findByUsername(String username) {
        DatabaseController databaseController = new DatabaseController();

        String sql = "SELECT id, username, password, role FROM users WHERE username = ?";

        try (Connection connection = databaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                User user = new User();
                user.setId(rs.getInt("id"));
                user.getCredentials().setUsername(rs.getString("username"));
                user.getCredentials().setRole(rs.getString("role"));
                user.getCredentials().setSessionId(null);
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean existsByUsername(String username) {
        DatabaseController databaseController = new DatabaseController();

        String sql = "SELECT 1 FROM users WHERE username = ?";

        try (Connection connection = databaseController.getConnection();
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
        DatabaseController databaseController = new DatabaseController();

        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

        try (Connection connection = databaseController.getConnection();
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
        DatabaseController databaseController = new DatabaseController();

        String sql = "INSERT INTO sessionids (id, userid) VALUES (?, ?)";

        try (Connection connection = databaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, sessionId);
            stmt.setInt(2, userId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteSession(String sessionId) {
        DatabaseController databaseController = new DatabaseController();

        String sql = "DELETE FROM sessionids WHERE id = ?";

        try (Connection connection = databaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, sessionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Integer findUserIdBySessionId(String sessionId) {
        DatabaseController databaseController = new DatabaseController();

        String sql = "SELECT userid FROM sessionids WHERE id = ?";

        try (Connection connection = databaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, sessionId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getInt("userid");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}