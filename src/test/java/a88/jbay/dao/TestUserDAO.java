package a88.jbay.dao;

import a88.jbay.testutil.TestDatabaseController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Test version of UserDAO that uses H2 test database
 */
public class TestUserDAO {
    
    public record UserData(
        int id,
        String username,
        String role,
        String password
    ) {}

    private UserData extractUserDataFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String role = rs.getString("role");
        String password = rs.getString("password");
        return new UserData(id, username, role, password);
    }

    private UserData executeUserQuery(String sql, Object... params) {
        try (Connection connection = TestDatabaseController.getConnection();
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

    public int insertUser(String username, String password, String role) {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        try (Connection connection = TestDatabaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, role);

            int result = stmt.executeUpdate();
            
            if (result > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            return -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
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
        String sql = "SELECT u.id, u.username, u.password, u.role FROM users u " +
                    "JOIN sessionids s ON u.id = s.userid WHERE s.id = ?";
        return executeUserQuery(sql, sessionId);
    }

    public boolean insertSession(String sessionId, int userId) {
        String sql = "INSERT INTO sessionids (id, userid) VALUES (?, ?)";
        try (Connection connection = TestDatabaseController.getConnection();
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
        String sql = "DELETE FROM sessionids WHERE id = ?";
        try (Connection connection = TestDatabaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, sessionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection connection = TestDatabaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean changeUserRole(int userId, String newRole) {
        String sql = "UPDATE users SET role = ? WHERE id = ?";
        try (Connection connection = TestDatabaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, newRole);
            stmt.setInt(2, userId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
