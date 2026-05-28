package a88.jbay.dao;

import a88.jbay.common.user.UserData;
import a88.jbay.server.DatabaseController;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class UserDAOImpl extends BaseDAO implements UserDAO {

    public UserDAOImpl(DatabaseController dbController) {
        super(dbController);
    }

    private UserData mapUser(ResultSet rs) throws SQLException {
        return new UserData(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("role"),
                rs.getString("password")
        );
    }

    @Override
    public UserData findByUsername(String username) {
        return executeQuery(
                "SELECT id, username, password, role FROM users WHERE username = ?",
                rs -> rs.next() ? mapUser(rs) : null,
                username
        );
    }

    @Override
    public UserData findByUserId(int userId) {
        return executeQuery(
                "SELECT id, username, password, role FROM users WHERE id = ?",
                rs -> rs.next() ? mapUser(rs) : null,
                userId
        );
    }

    @Override
    public boolean existsByUsername(String username) {
        return executeQuery(
                "SELECT 1 FROM users WHERE username = ?",
                rs -> rs.next(),
                username
        );
    }

    @Override
    public int insertUser(String username, String hashedPassword, String role, byte[] qrCode) {
        return executeInsert(
                "INSERT INTO users (username, password, role, qr) VALUES (?, ?, ?, ?)",
                username, hashedPassword, role, qrCode
        );
    }

    @Override
    public boolean changeUserRole(int userId, String role) {
        return executeUpdate(
                "UPDATE users SET role = ? WHERE id = ?",
                role, userId
        ) > 0;
    }

    @Override
    public List<UserData> getAllNormalUsers() {
        return executeQueryList(
                "SELECT id, username, role, password FROM users WHERE role != 'ADMIN' ORDER BY id DESC",
                this::mapUser
        );
    }

    @Override
    public byte[] getQr(int userId) {
        return executeQuery(
                "SELECT qr FROM users WHERE id = ?",
                rs -> rs.next() ? rs.getBytes("qr") : null,
                userId
        );
    }
}
