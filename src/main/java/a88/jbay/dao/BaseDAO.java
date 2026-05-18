package a88.jbay.dao;

import a88.jbay.server.DatabaseController;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseDAO {

    protected final DatabaseController dbController;

    protected BaseDAO(DatabaseController dbController) {
        this.dbController = dbController;
    }

    @FunctionalInterface
    protected interface ResultSetMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    protected void bindStatement(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }

    protected int executeUpdate(String sql, Object... params) {
        try (Connection connection = dbController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            bindStatement(stmt, params);
            return stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    protected int executeInsert(String sql, Object... params) {
        try (Connection connection = dbController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     sql,
                     PreparedStatement.RETURN_GENERATED_KEYS
             )) {

            bindStatement(stmt, params);

            if (stmt.executeUpdate() == 0) return -1;

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }

            return -1;

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    protected <T> T executeQuery(String sql, ResultSetMapper<T> mapper, Object... params) {
        try (Connection connection = dbController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            bindStatement(stmt, params);

            try (ResultSet rs = stmt.executeQuery()) {
                return mapper.map(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected <T> List<T> executeQueryList(String sql, ResultSetMapper<T> rowMapper, Object... params) {
        return executeQuery(sql, rs -> {
            List<T> results = new ArrayList<>();
            while (rs.next()) results.add(rowMapper.map(rs));
            return results;
        }, params);
    }
}