package a88.jbay.dao;

import a88.jbay.server.DatabaseController;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class BaseDAO {

    protected final DatabaseController dbController;

    protected BaseDAO(DatabaseController dbController) {
        this.dbController = dbController;
    }

    @FunctionalInterface
    protected interface ResultSetMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    @FunctionalInterface
    protected interface TransactionWork<T> {
        T execute(Connection connection) throws SQLException;
    }

    // --- Core helpers ---

    protected void bindStatement(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }

    // --- Transaction ---

    protected <T> T executeTransaction(TransactionWork<T> work) {
        try (Connection connection = dbController.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.execute(connection);
                connection.commit();
                return result;
            } catch (Exception e) {
                connection.rollback();
                e.printStackTrace();
                return null;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // --- single-connection overloads (for use inside transactions) ---

    protected int executeUpdate(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            bindStatement(stmt, params);
            return stmt.executeUpdate();
        }
    }

    protected int executeInsert(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            bindStatement(stmt, params);
            if (stmt.executeUpdate() == 0) throw new SQLException("Insert returned no rows");
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("Insert returned no generated key");
            }
        }
    }

    protected <T> T executeQuery(Connection connection, String sql, ResultSetMapper<T> mapper,
                                 Object... params) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            bindStatement(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                return mapper.map(rs);
            }
        }
    }

    // --- standalone overloads (manage their own connection) ---

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
                     sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
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