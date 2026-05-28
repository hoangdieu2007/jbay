package a88.jbay.di;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationContextTest {

    @BeforeEach
    @AfterEach
    void resetSingleton() throws Exception {
        Field instance = ApplicationContext.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    @Test
    void testGetInstanceReturnsSingleton() {
        ApplicationContext ctx1 = ApplicationContext.getInstance();
        ApplicationContext ctx2 = ApplicationContext.getInstance();
        assertSame(ctx1, ctx2);
    }

    @Test
    void testConfigureDatabaseRegistersDaoClasses() {
        ApplicationContext ctx = ApplicationContext.getInstance();
        ctx.configureDatabase();
        assertNotNull(ctx.getDependency(a88.jbay.dao.UserDAO.class));
        assertNotNull(ctx.getDependency(a88.jbay.dao.ItemDAO.class));
        assertNotNull(ctx.getDependency(a88.jbay.dao.BidDAO.class));
        assertNotNull(ctx.getDependency(a88.jbay.dao.AuctionDAO.class));
        assertNotNull(ctx.getDependency(a88.jbay.server.DatabaseController.class));
    }

    @Test
    void testGetDependencyBeforeConfigureThrows() {
        ApplicationContext ctx = ApplicationContext.getInstance();
        assertThrows(IllegalArgumentException.class,
                () -> ctx.getDependency(String.class));
    }
}
