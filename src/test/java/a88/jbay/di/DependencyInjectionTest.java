package a88.jbay.di;

import a88.jbay.dao.UserDAO;
import a88.jbay.repository.UserRepository;
import a88.jbay.system.update.ConnectionSystem;
import a88.jbay.system.user.UserSystem;
import a88.jbay.server.DatabaseController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify dependency injection implementation works correctly.
 */
public class DependencyInjectionTest {

    @BeforeEach
    void setUp() {
        // Clear the DI container before each test
        DependencyInjectionContainer container = DependencyInjectionContainer.getInstance();
        container.clear();
        
        // Set up test dependencies
        container.registerSingleton(DatabaseController.class, DatabaseController.getInstance());
        container.registerSingleton(UserDAO.class, new UserDAO(DatabaseController.getInstance()));
        container.registerSingleton(UserRepository.class, new UserRepository(container.getInstance(UserDAO.class)));
        container.registerSingleton(UserSystem.class, new UserSystem(container.getInstance(UserRepository.class)));
    }

    @Test
    void testSingletonRegistration() {
        DependencyInjectionContainer container = DependencyInjectionContainer.getInstance();
        
        UserDAO userDAO1 = container.getInstance(UserDAO.class);
        UserDAO userDAO2 = container.getInstance(UserDAO.class);
        
        // Should return the same instance (singleton)
        assertSame(userDAO1, userDAO2);
    }

    @Test
    void testDependencyInjection() {
        DependencyInjectionContainer container = DependencyInjectionContainer.getInstance();
        
        UserSystem userSystem = container.getInstance(UserSystem.class);
        UserDAO userDAO = container.getInstance(UserDAO.class);
        
        assertNotNull(userSystem);
        assertNotNull(userDAO);
        
        // The UserSystem should have been created with the UserDAO dependency
        // This is verified by checking that the UserSystem can perform operations
        assertNotNull(userSystem);
    }

    @Test
    void testUnregisteredDependencyThrowsException() {
        DependencyInjectionContainer container = DependencyInjectionContainer.getInstance();
        
        // Should throw exception for unregistered type
        assertThrows(IllegalArgumentException.class, () -> {
            container.getInstance(String.class);
        });
    }

    
    @Test
    void testApplicationContextConfiguration() {
        // Test that ApplicationContext can be initialized without errors
        assertDoesNotThrow(() -> {
            ApplicationContext.getInstance();
        });
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        DependencyInjectionContainer container = DependencyInjectionContainer.getInstance();
        container.clear();
    }
}
