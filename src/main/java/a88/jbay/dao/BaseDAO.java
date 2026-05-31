package a88.jbay.dao;

import a88.jbay.server.DatabaseController;
import a88.jbay.util.JBayLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class BaseDAO {

    protected final JBayLogger logger;
    protected final DatabaseController dbController;

    protected BaseDAO(DatabaseController dbController) {
        this.dbController = dbController;
        this.logger = JBayLogger.getLogger(this.getClass());
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
        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try (Connection connection = dbController.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    T result = work.execute(connection);
                    connection.commit();
                    return result;
                } catch (Exception e) {
                    logger.error("Transaction failed (attempt " + (attempt + 1) + "/" + maxRetries + "): " + e.getMessage(), e);
                    try {
                        connection.rollback();
                    } catch (SQLException re) {
                        logger.error("Rollback also failed: " + re.getMessage(), re);
                    }
                    if (attempt == maxRetries - 1) {
                        return null;
                    }
                    try { Thread.sleep(50L * (attempt + 1)); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                } finally {
                    try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
                }
            } catch (SQLException e) {
                logger.error("Connection error (attempt " + (attempt + 1) + "/" + maxRetries + "): " + e.getMessage());
                if (attempt == maxRetries - 1) {
                    return null;
                }
                try { Thread.sleep(50L * (attempt + 1)); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
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
            logger.error("executeUpdate failed: " + e.getMessage(), e);
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
            logger.error("executeInsert failed: " + e.getMessage(), e);
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
            logger.error("executeQuery failed: " + e.getMessage(), e);
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