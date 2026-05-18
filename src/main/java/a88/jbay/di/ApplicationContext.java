package a88.jbay.di;

import a88.jbay.dao.*;
import a88.jbay.data.BidRepository;
import a88.jbay.data.UserRepository;
import a88.jbay.server.DatabaseController;
import a88.jbay.system.user.AdminService;
import a88.jbay.system.AuctionSystem;
import a88.jbay.system.BidSystem;
import a88.jbay.system.update.ConnectionSystem;
import a88.jbay.system.update.UpdateSystem;
import a88.jbay.system.user.UserSystem;
import a88.jbay.data.AuctionRepository;

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
        container.registerSingleton(DatabaseController.class, new DatabaseController());

        // dao - register as singletons
        container.registerSingleton(UserDAO.class, new UserDAOImpl(container.getInstance(DatabaseController.class)));
        ItemDAO itemDAO = new ItemDAOImpl(container.getInstance(DatabaseController.class));
        container.registerSingleton(ItemDAO.class, itemDAO);
        BidDAO bidDAO = new BidDAOImpl(container.getInstance(DatabaseController.class));
        container.registerSingleton(BidDAO.class, bidDAO);
        container.registerSingleton(AuctionDAO.class, new AuctionDAOImpl(container.getInstance(DatabaseController.class)));

        // system classes a.k.a. service layer - register as singletons
        container.registerSingleton(AuctionRepository.class, new AuctionRepository(
                container.getInstance(DatabaseController.class),
                container.getInstance(AuctionDAO.class),
                container.getInstance(ItemDAO.class),
                container.getInstance(UserDAO.class),
                container.getInstance(BidDAO.class)
        ));
        container.registerSingleton(UserRepository.class, new UserRepository(
                container.getInstance(UserDAO.class)
        ));
        container.registerSingleton(BidRepository.class, new BidRepository(
                container.getInstance(DatabaseController.class),
                container.getInstance(AuctionDAO.class),
                container.getInstance(BidDAO.class)
        ));

        container.registerSingleton(ConnectionSystem.class, new ConnectionSystem());
        container.registerSingleton(UpdateSystem.class, new UpdateSystem(
                container.getInstance(ConnectionSystem.class),
                container.getInstance(AuctionRepository.class)
        ));

        container.registerSingleton(UserSystem.class, new UserSystem(
                container.getInstance(UserRepository.class)
        ));
        container.registerSingleton(AdminService.class, new AdminService(
                container.getInstance(UserDAO.class),
                container.getInstance(ConnectionSystem.class),
                container.getInstance(UpdateSystem.class),
                container.getInstance(UserSystem.class)
        ));

        container.registerSingleton(BidSystem.class, new BidSystem(
            container.getInstance(AuctionRepository.class),
            container.getInstance(BidRepository.class),
            container.getInstance(BidDAO.class),
            container.getInstance(AuctionDAO.class)
        ));

        container.registerSingleton(AuctionSystem.class, new AuctionSystem(
            container.getInstance(ConnectionSystem.class),
            container.getInstance(AuctionRepository.class)
        ));
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
