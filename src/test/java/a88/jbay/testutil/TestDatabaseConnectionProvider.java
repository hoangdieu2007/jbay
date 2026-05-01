package a88.jbay.testutil;

import a88.jbay.server.DatabaseConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Manual mock implementation of DatabaseConnectionProvider for Java 25 compatibility
 * This replaces Mockito for testing without experimental flags
 */
public class TestDatabaseConnectionProvider implements DatabaseConnectionProvider {
    
    private final Map<String, Object> mockData = new HashMap<>();
    private boolean shouldThrowException = false;
    private SQLException exceptionToThrow = new SQLException("Test exception");
    private Function<String, Connection> connectionProvider;
    
    @Override
    public Connection getConnection() throws SQLException {
        if (shouldThrowException) {
            throw exceptionToThrow;
        }
        
        if (connectionProvider != null) {
            return connectionProvider.apply("test");
        }
        
        // Default behavior - return a mock connection or null
        return null;
    }
    
    // Test control methods


    public boolean isShouldThrowException() {
        return shouldThrowException;
    }

    public void setShouldThrowException(boolean shouldThrow) {
        this.shouldThrowException = shouldThrow;
    }
    
    public void setExceptionToThrow(SQLException exception) {
        this.exceptionToThrow = exception;
    }
    
    public void setConnectionProvider(Function<String, Connection> provider) {
        this.connectionProvider = provider;
    }
    
    public void addMockData(String key, Object value) {
        mockData.put(key, value);
    }
    
    public Object getMockData(String key) {
        return mockData.get(key);
    }
    
    public void clearMockData() {
        mockData.clear();
    }
    
    public void reset() {
        shouldThrowException = false;
        exceptionToThrow = new SQLException("Test exception");
        connectionProvider = null;
        mockData.clear();
    }
    
    public boolean shouldThrowException() {
        return shouldThrowException;
    }
}
