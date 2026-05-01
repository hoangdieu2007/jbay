package a88.jbay.dao;

import a88.jbay.model.entity.user.User;
import a88.jbay.server.DatabaseConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
    private static UserDAO instance;
    private final DatabaseConnectionProvider dbProvider;

    public record UserData(
        int id,
        String username,
        String role,
        String password
    ) {}

    private UserDAO() {
        this.dbProvider = a88.jbay.server.DatabaseController.getInstance();
    }

    // dependency injection
    public UserDAO(DatabaseConnectionProvider dbProvider) {
        this.dbProvider = dbProvider;
    }

    private UserData extractUserDataFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String role = rs.getString("role");
        String password = rs.getString("password");
        return new UserData(id, username, role, password);
    }

    private UserData executeUserQuery(String sql, Object... params) {
        try (Connection connection = dbProvider.getConnection();
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
                return extractUserDataFromResultSet(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean executeUpdate(String sql, Object... params) {
        try (Connection connection = dbProvider.getConnection();
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

    public UserData findByUsername(String username) {
        String sql = "SELECT id, username, password, role FROM users WHERE username = ?";
        return executeUserQuery(sql, username);
    }

    
    public UserData findByUserId(int userId) {
        String sql = "SELECT id, username, password, role FROM users WHERE id = ?";
        return executeUserQuery(sql, userId);
    }

    public UserData findBySessionId(String sessionId) {
        String sql = "SELECT userid FROM sessionids WHERE id = ?";

        try (Connection connection = dbProvider.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, sessionId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                int userId = rs.getInt("userid");
                String userSql = "SELECT id, username, password, role FROM users WHERE id = ?";
                return executeUserQuery(userSql, userId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection connection = dbProvider.getConnection();
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

        try (Connection connection = dbProvider.getConnection();
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