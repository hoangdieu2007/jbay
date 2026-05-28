package a88.jbay.dao;

import a88.jbay.server.DatabaseController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BaseDAOTest {

    private static DatabaseController dbController;
    private TestDAO dao;

    @BeforeAll
    static void initDb() {
        dbController = new DatabaseController();
        dbController.initializePool("jdbc:h2:mem:basetest;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
    }

    @AfterAll
    static void closeDb() {
        dbController.close();
    }

    @BeforeEach
    void setUp() throws Exception {
        dao = new TestDAO(dbController);
        try (Connection c = dbController.getConnection();
             java.sql.Statement s = c.createStatement()) {
            s.execute("DROP TABLE IF EXISTS test_entity");
            s.execute("CREATE TABLE test_entity (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100))");
        }
    }

    // --- executeTransaction ---

    @Test
    void testExecuteTransactionSuccess() {
        Integer result = dao.txWork(conn -> {
            try (var ps = conn.prepareStatement("INSERT INTO test_entity (name) VALUES ('hello')")) {
                return ps.executeUpdate();
            }
        });
        assertEquals(1, result);
    }

    @Test
    void testExecuteTransactionRollbackOnException() {
        Integer result = dao.txWork(conn -> {
            try (var ps = conn.prepareStatement("INSERT INTO test_entity (name) VALUES ('will_rollback')")) {
                ps.executeUpdate();
                throw new RuntimeException("force rollback");
            }
        });
        assertNull(result);
        // verify rollback: table should be empty
        List<String> names = dao.findAll();
        assertTrue(names.isEmpty());
    }

    @Test
    void testExecuteTransactionConnectionError() {
        DatabaseController badDb = new DatabaseController();
        TestDAO badDao = new TestDAO(badDb);
        assertThrows(java.lang.IllegalStateException.class,
                () -> badDao.txWork(conn -> 1));
    }

    // --- standalone executeUpdate ---

    @Test
    void testExecuteUpdateBadSqlReturnsNegative() {
        int result = dao.executeUpdate("INSERT INTO nonexistent (x) VALUES (1)");
        assertEquals(-1, result);
    }

    // --- standalone executeInsert ---

    @Test
    void testExecuteInsertSuccess() {
        int id = dao.executeInsert("INSERT INTO test_entity (name) VALUES ('item')");
        assertTrue(id > 0);
    }

    @Test
    void testExecuteInsertBadSqlReturnsNegative() {
        int result = dao.executeInsert("INSERT INTO nonexistent (x) VALUES (1)");
        assertEquals(-1, result);
    }

    // --- standalone executeQuery ---

    @Test
    void testExecuteQueryBadSqlReturnsNull() {
        String result = dao.executeQuery("SELECT x FROM nonexistent", rs -> "ok");
        assertNull(result);
    }

    @Test
    void testExecuteQueryListBadSql() {
        List<String> result = dao.executeQueryList("SELECT x FROM nonexistent", rs -> "ok");
        assertNull(result);
    }

    // --- bindStatement ---

    @Test
    void testBindAndQuery() {
        dao.executeInsert("INSERT INTO test_entity (name) VALUES ('alpha')");
        String name = dao.executeQuery("SELECT name FROM test_entity WHERE id = ?",
                rs -> rs.next() ? rs.getString("name") : null, 1);
        assertEquals("alpha", name);
    }

    @Test
    void testExecuteUpdateWithConnectionSuccess() throws Exception {
        try (Connection c = dbController.getConnection()) {
            int rows = dao.executeUpdate(c, "INSERT INTO test_entity (name) VALUES (?)", "conn_test");
            assertEquals(1, rows);
        }
    }

    @Test
    void testExecuteQueryListViaStandalone() {
        dao.executeInsert("INSERT INTO test_entity (name) VALUES ('a')");
        dao.executeInsert("INSERT INTO test_entity (name) VALUES ('b')");
        List<String> results = dao.executeQueryList("SELECT name FROM test_entity ORDER BY id", rs -> rs.getString("name"));
        assertEquals(List.of("a", "b"), results);
    }

    // --- concrete DAO subclass for testing BaseDAO methods ---

    static class TestDAO extends BaseDAO {
        TestDAO(DatabaseController dbController) {
            super(dbController);
        }

        <T> T txWork(BaseDAO.TransactionWork<T> work) {
            return executeTransaction(work);
        }

        // expose protected standalone overloads for testing
        @Override
        protected int executeUpdate(String sql, Object... params) {
            return super.executeUpdate(sql, params);
        }

        @Override
        protected int executeInsert(String sql, Object... params) {
            return super.executeInsert(sql, params);
        }

        @Override
        protected <T> T executeQuery(String sql, BaseDAO.ResultSetMapper<T> mapper, Object... params) {
            return super.executeQuery(sql, mapper, params);
        }

        @Override
        protected <T> List<T> executeQueryList(String sql, BaseDAO.ResultSetMapper<T> rowMapper, Object... params) {
            return super.executeQueryList(sql, rowMapper, params);
        }

        @Override
        protected int executeUpdate(Connection connection, String sql, Object... params) throws SQLException {
            return super.executeUpdate(connection, sql, params);
        }

        List<String> findAll() {
            return executeQueryList("SELECT name FROM test_entity ORDER BY id", rs -> rs.getString("name"));
        }
    }
}
