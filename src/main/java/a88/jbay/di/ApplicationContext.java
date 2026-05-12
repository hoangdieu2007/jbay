package a88.jbay.di;

import a88.jbay.dao.AuctionDAO;
import a88.jbay.dao.BidDAO;
import a88.jbay.dao.ItemDAO;
import a88.jbay.dao.UserDAO;
import a88.jbay.repository.AuctionRepository;
import a88.jbay.server.DatabaseController;
import a88.jbay.system.AuctionSystem;
import a88.jbay.system.BidSystem;
import a88.jbay.system.UpdateSystem;
import a88.jbay.system.UserSystem;
import a88.jbay.util.JBayLogger;

/**
 * application context for configuring and managing application dependencies.
 * this class sets up all the necessary dependencies for the application.
 * because i'm having so much pain setting up tests for singleton classes with that
 * synchronized getInstance thing, i'm writing my own singleton manager
 * you can later mock any of the class configured in this class
 *
 * why am i using english for everything?
 * because i like it that way, feels professional while coding apps :v
 *
 */
public class ApplicationContext {
    private static ApplicationContext instance;
    private final DependencyInjectionContainer container;

    private ApplicationContext() {
        this.container = DependencyInjectionContainer.getInstance();
        configureDependencies();
    }

    public static synchronized ApplicationContext getInstance() {
        if (instance == null) {
            instance = new ApplicationContext();
        }
        return instance;
    }

    /**
     * configure all application dependencies.
     */
    private void configureDependencies() {
        // database controller - directly use the singleton
        container.registerSingleton(DatabaseController.class, DatabaseController.getInstance());

        // dao - register as singletons
        container.registerSingleton(UserDAO.class, new UserDAO(DatabaseController.getInstance()));
        container.registerSingleton(ItemDAO.class, new ItemDAO(DatabaseController.getInstance()));
        container.registerSingleton(BidDAO.class, new BidDAO(DatabaseController.getInstance()));
        container.registerSingleton(AuctionDAO.class, new AuctionDAO(
            DatabaseController.getInstance(),
            container.getInstance(ItemDAO.class),
            container.getInstance(BidDAO.class)
        ));

        // system classes a.k.a. service layer - register as singletons
        container.registerSingleton(AuctionRepository.class, new AuctionRepository());
        container.registerSingleton(UpdateSystem.class, new UpdateSystem());
        container.registerSingleton(BidSystem.class, new BidSystem(
            container.getInstance(AuctionDAO.class),
            container.getInstance(BidDAO.class),
            container.getInstance(UserDAO.class),
            container.getInstance(AuctionRepository.class)
        ));
        container.registerSingleton(UserSystem.class, new UserSystem(
                container.getInstance(UserDAO.class),
                container.getInstance(UpdateSystem.class),
                container.getInstance(AuctionSystem.class)
        ));
        container.registerSingleton(AuctionSystem.class, new AuctionSystem(
            container.getInstance(AuctionDAO.class),
            container.getInstance(UserDAO.class),
            container.getInstance(BidDAO.class),
            container.getInstance(AuctionRepository.class),
            container.getInstance(BidSystem.class)
        ));
        
        // resolve circular dependency by setting AuctionSystem in UpdateSystem
        container.getInstance(UpdateSystem.class).setAuctionSystem(container.getInstance(AuctionSystem.class));
    }

    /**
     * get a dependency from the DI container
     */
    public <T> T getDependency(Class<T> type) {
        return container.getInstance(type);
    }

    /**
     * get the dependency injection container, make sure you know what you're doing.
     */
    public DependencyInjectionContainer getContainer() {
        return container;
    }

    /**
     * init the application context, call upon production app's loading stage.
     */
    public static void initialize() {
        new ApplicationContext();
    }
}
