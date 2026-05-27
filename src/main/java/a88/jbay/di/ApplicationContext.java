package a88.jbay.di;

import a88.jbay.dao.*;
import a88.jbay.data.BidRepository;
import a88.jbay.data.UserRepository;
import a88.jbay.server.ClientService;
import a88.jbay.server.DatabaseController;
import a88.jbay.server.RequestHandler;
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
        this.container = new DependencyInjectionContainer();
    }

    public static synchronized ApplicationContext getInstance() {
        if (instance == null) {
            instance = new ApplicationContext();
        }
        return instance;
    }

    // phase 1 — just the database layer, safe to call before DB connects
    public void configureDatabase() {
        container.registerSingleton(DatabaseController.class, new DatabaseController()); // no getInstance()

        DatabaseController db = container.getInstance(DatabaseController.class);
        container.registerSingleton(UserDAO.class, new UserDAOImpl(db));
        container.registerSingleton(ItemDAO.class, new ItemDAOImpl(db));
        container.registerSingleton(BidDAO.class, new BidDAOImpl(db));
        container.registerSingleton(AuctionDAO.class, new AuctionDAOImpl(db));
    }

    // phase 2 — call this after DB pool is initialized
    public void configureServices() {
        configureRepositories();
        configureSystems();
    }

    private void configureRepositories() {
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
    }

    private void configureSystems() {
        container.registerSingleton(ConnectionSystem.class, new ConnectionSystem());
        container.registerSingleton(UpdateSystem.class, new UpdateSystem(
                container.getInstance(ConnectionSystem.class)
        ));
        container.registerSingleton(UserSystem.class, new UserSystem(
                container.getInstance(UserRepository.class)
        ));
        container.registerSingleton(BidSystem.class, new BidSystem(
                container.getInstance(AuctionRepository.class),
                container.getInstance(BidRepository.class),
                container.getInstance(BidDAO.class),
                container.getInstance(AuctionDAO.class),
                container.getInstance(UpdateSystem.class)
        ));
        container.registerSingleton(AuctionSystem.class, new AuctionSystem(
                container.getInstance(UpdateSystem.class),
                container.getInstance(AuctionRepository.class),
                container.getInstance(UserRepository.class)
        ));
        container.registerSingleton(AdminService.class, new AdminService(
                container.getInstance(UserDAO.class),
                container.getInstance(UserRepository.class),
                container.getInstance(ConnectionSystem.class),
                container.getInstance(AuctionSystem.class),
                container.getInstance(UserSystem.class)
        ));
        container.registerSingleton(RequestHandler.class, new RequestHandler(
                container.getInstance(UserSystem.class),
                container.getInstance(AdminService.class),
                container.getInstance(AuctionSystem.class),
                container.getInstance(ConnectionSystem.class),
                container.getInstance(UpdateSystem.class),
                container.getInstance(BidSystem.class)
        ));
        container.registerSingleton(ClientService.class, new ClientService(
                container.getInstance(ConnectionSystem.class),
                container.getInstance(UserSystem.class),
                container.getInstance(RequestHandler.class)
        ));
    }

    public <T> T getDependency(Class<T> type) {
        return container.getInstance(type);
    }

    public DependencyInjectionContainer getContainer() {
        return container;
    }
}
